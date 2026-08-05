package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoverySnapshot

fun interface ChampionCapabilityResolver {
    suspend fun resolve(
        session: ChampionSession,
        queue: ChampionGattQueue,
    ): ChampionBleResult<ChampionSessionCapabilities>
}

class ChampionStoredDiscoveryCapabilityResolver(
    private val snapshotProvider: suspend (ChampionSessionId) -> AndroidGattDiscoverySnapshot?,
) : ChampionCapabilityResolver {
    override suspend fun resolve(
        session: ChampionSession,
        queue: ChampionGattQueue,
    ): ChampionBleResult<ChampionSessionCapabilities> {
        val unused = queue
        val discoverySnapshot = snapshotProvider(session.id)
            ?: return ChampionBleResult.Failure(ChampionBleError.DiscoveryFailed())
        return ChampionCapabilityRegistry.resolve(session.id, discoverySnapshot)
    }
}

class ChampionConnectionManager(
    private val scanner: ChampionBleScanner,
    private val client: ChampionBleClient,
    private val capabilityResolver: ChampionCapabilityResolver,
    parentScope: CoroutineScope,
    private val timeouts: ChampionBleTimeouts = ChampionBleTimeouts(),
    private val protocolLogger: ChampionProtocolLogger? = null,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val mutex = Mutex()
    private val scannerMutex = Mutex()
    private val mutableState = MutableStateFlow<ChampionBleState>(ChampionBleState.Disconnected)
    private var activeTransport: ChampionTransportSession? = null
    private var pendingConnection: PendingConnection? = null
    private var stateGeneration: Long = 0

    val state: StateFlow<ChampionBleState> = mutableState.asStateFlow()

    init {
        scope.launch {
            client.events.collect(::handleClientEvent)
        }
    }

    suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>> {
        val generation = beginStateOperation(ChampionBleState.Scanning(request))
        val result = scannerMutex.withLock { scanner.scan(request) }
        publishScanResultIfCurrent(generation, result)
        return result
    }

    suspend fun connectFirst(request: ChampionScanRequest): ChampionBleResult<ChampionTransportSession> {
        return when (val scanResult = scan(request)) {
            is ChampionBleResult.Failure -> ChampionBleResult.Failure(scanResult.error)
            is ChampionBleResult.Success -> {
                val device = scanResult.value.firstOrNull()
                    ?: return ChampionBleResult.Failure(ChampionBleError.DeviceNotFound)
                connect(device)
            }
        }
    }

    suspend fun connect(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionTransportSession> {
        val generation = beginStateOperation()
        val resources = ConnectionResources()
        try {
            disconnectCurrent(
                reason = ChampionDisconnectReason.CLEANUP,
                generation = generation,
            )
            if (!isCurrentGeneration(generation)) return cancelledResult()

            val session = when (val openResult = client.openSession(device)) {
                is ChampionBleResult.Failure -> {
                    publishStateIfCurrent(generation, ChampionBleState.Failed(null, openResult.error))
                    return ChampionBleResult.Failure(openResult.error)
                }

                is ChampionBleResult.Success -> openResult.value
            }
            resources.session = session

            val pending = PendingConnection(session.id, generation)
            resources.pending = pending
            val pendingInstalled = mutex.withLock {
                if (stateGeneration != generation) {
                    false
                } else {
                    pendingConnection = pending
                    true
                }
            }
            if (!pendingInstalled) {
                cleanupSession(session, null, null, expectedPending = pending)
                return cancelledResult()
            }

            val queue = ChampionGattQueue(
                sessionId = session.id,
                parentScope = scope,
                operationExecutor = client::execute,
            )
            resources.queue = queue
            client.attachQueue(session.id, queue)
            if (!publishStateIfCurrent(generation, ChampionBleState.Connecting(session, device))) {
                cleanupSession(session, queue, null, expectedPending = pending)
                return cancelledResult()
            }

            val connected = withTimeoutOrNull(timeouts.connect) {
                pending.connected.await()
            } == true
            if (!connected) {
                if (!isCurrentGeneration(generation)) {
                    cleanupSession(session, queue, null, expectedPending = pending)
                    return cancelledResult()
                }
                val error = ChampionBleError.Timeout(ChampionOperationType.CONNECT, timeouts.connect)
                cleanupSession(session, queue, error, expectedPending = pending)
                publishStateIfCurrent(generation, ChampionBleState.Failed(session, error))
                return ChampionBleResult.Failure(error)
            }

            if (!publishStateIfCurrent(generation, ChampionBleState.DiscoveringServices(session))) {
                cleanupSession(session, queue, null, expectedPending = pending)
                return cancelledResult()
            }
            val discoveryResult = queue.enqueue(
                ChampionGattOperation.create(
                    sessionId = session.id,
                    type = ChampionOperationType.DISCOVER_SERVICES,
                    target = ChampionGattTarget.Session,
                    timeout = timeouts.discovery,
                ),
            )
            if (discoveryResult is ChampionGattTerminalResult.Failure) {
                cleanupSession(session, queue, discoveryResult.error, expectedPending = pending)
                publishStateIfCurrent(generation, ChampionBleState.Failed(session, discoveryResult.error))
                return ChampionBleResult.Failure(discoveryResult.error)
            }

            if (!publishStateIfCurrent(generation, ChampionBleState.ResolvingChannels(session))) {
                cleanupSession(session, queue, null, expectedPending = pending)
                return cancelledResult()
            }
            val capabilities = when (val resolved = capabilityResolver.resolve(session, queue)) {
                is ChampionBleResult.Failure -> {
                    cleanupSession(session, queue, resolved.error, expectedPending = pending)
                    publishStateIfCurrent(generation, ChampionBleState.Failed(session, resolved.error))
                    return ChampionBleResult.Failure(resolved.error)
                }

                is ChampionBleResult.Success -> resolved.value
            }

            if (!publishStateIfCurrent(
                    generation,
                    ChampionBleState.ConfiguringSubscriptions(session, capabilities.snapshot),
                )
            ) {
                cleanupSession(session, queue, null, expectedPending = pending)
                return cancelledResult()
            }
            val packetSource = merge(
                client.events
                    .filterIsInstance<ChampionGattSessionEvent.CharacteristicChanged>()
                    .filter { it.sessionId == session.id }
                    .mapNotNull { it.packet },
                client.events
                    .filterIsInstance<ChampionGattSessionEvent.CharacteristicRead>()
                    .filter { it.sessionId == session.id }
                    .mapNotNull { it.packet },
            )
            val streams = ChampionTransportStreams(packetSource = packetSource, scope = scope)
            resources.streams = streams
            val notificationHub = ChampionNotificationHub(
                sessionCapabilities = capabilities,
                queue = queue,
                sessionEvents = client.events,
                scope = scope,
            )
            resources.notifications = notificationHub

            for (channel in capabilities.defaultSubscribedChannels) {
                val subscription = notificationHub.subscribe(channel)
                if (subscription is ChampionBleResult.Failure) {
                    cleanupSession(
                        session,
                        queue,
                        subscription.error,
                        notificationHub,
                        streams,
                        jobs = resources.jobs,
                        expectedPending = pending,
                    )
                    publishStateIfCurrent(generation, ChampionBleState.Failed(session, subscription.error))
                    return ChampionBleResult.Failure(subscription.error)
                }
                val collector = scope.launch {
                    (subscription as ChampionBleResult.Success).value.collect { packet ->
                        protocolLogger?.logPacketObserved(packet, consumedByMatcher = false)
                    }
                }
                resources.jobs += collector
            }

            val transport = ChampionTransportSession(
                session = session,
                capabilities = capabilities,
                queue = queue,
                notifications = notificationHub,
                streams = streams,
                internalJobs = resources.jobs,
            )
            resources.transport = transport

            val activated = mutex.withLock {
                if (stateGeneration != generation) {
                    false
                } else {
                    activeTransport = transport
                    mutableState.value = ChampionBleState.TransportReady(session, capabilities.snapshot)
                    true
                }
            }
            if (!activated) {
                cleanupSession(
                    session = transport.session,
                    queue = transport.queue,
                    error = null,
                    notifications = transport.notifications,
                    streams = transport.streams,
                    jobs = transport.internalJobs,
                    expectedTransport = transport,
                    expectedPending = pending,
                )
                return cancelledResult()
            }
            protocolLogger?.logTransportState(session.id, "transport_ready", null)
            return ChampionBleResult.Success(transport)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                resources.session?.let { session ->
                    cleanupSession(
                        session = session,
                        queue = resources.queue,
                        error = null,
                        notifications = resources.notifications,
                        streams = resources.streams,
                        jobs = resources.jobs,
                        expectedTransport = resources.transport,
                        expectedPending = resources.pending,
                    )
                }
                publishDisconnectedAfterCancelledConnectIfIdle(generation)
            }
            throw error
        }
    }

    suspend fun currentSession(): ChampionTransportSession? = mutex.withLock { activeTransport }

    suspend fun disconnect(
        reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED,
    ): ChampionBleResult<Unit> {
        val generation = beginStateOperation()
        return disconnectCurrent(reason, generation)
    }

    suspend fun disconnectExpectedTransport(
        expectedTransport: ChampionTransportSession,
        reason: ChampionDisconnectReason = ChampionDisconnectReason.CLEANUP,
    ): ChampionBleResult<Unit> {
        val generation = beginExpectedTransportOperation(expectedTransport)
            ?: return cancelledResult()
        return disconnectCurrent(reason, generation, expectedTransport)
    }

    private suspend fun disconnectCurrent(
        reason: ChampionDisconnectReason,
        generation: Long,
        expectedTransport: ChampionTransportSession? = null,
    ): ChampionBleResult<Unit> {
        scannerMutex.withLock {
            scanner.cancelScan(ChampionCancellationReason.DISCONNECT)
        }
        val selection = mutex.withLock {
            if (stateGeneration != generation) {
                DisconnectSelection.Stale
            } else if (expectedTransport != null && activeTransport !== expectedTransport) {
                DisconnectSelection.Stale
            } else {
                activeTransport?.let { transport ->
                    mutableState.value = ChampionBleState.Disconnecting(transport.session, reason)
                    DisconnectSelection.Active(transport)
                } ?: run {
                    mutableState.value = ChampionBleState.Disconnected
                    DisconnectSelection.None
                }
            }
        }
        val transport = when (selection) {
            is DisconnectSelection.Active -> selection.transport
            DisconnectSelection.None -> return ChampionBleResult.Success(Unit)
            DisconnectSelection.Stale -> return cancelledResult()
        }
        cleanupSession(
            session = transport.session,
            queue = transport.queue,
            error = null,
            notifications = transport.notifications,
            streams = transport.streams,
            jobs = transport.internalJobs,
            disconnectReason = reason,
            expectedTransport = transport,
            terminalState = ChampionBleState.Disconnected,
            terminalGeneration = generation,
        )
        return ChampionBleResult.Success(Unit)
    }

    private suspend fun handleClientEvent(event: ChampionGattSessionEvent) {
        val current = mutex.withLock {
            val pending = pendingConnection
            if (event is ChampionGattSessionEvent.ConnectionStateChanged &&
                pending != null &&
                pending.generation == stateGeneration &&
                pending.sessionId == event.sessionId &&
                event.isConnected
            ) {
                pending.connected.complete(true)
                pendingConnection = null
            }
            activeTransport
        }

        if (current == null || current.session.id != event.sessionId) return
        if (event is ChampionGattSessionEvent.ConnectionStateChanged && !event.isConnected) {
            val error = ChampionBleError.UnexpectedDisconnect(event.detail)
            val generation = mutex.withLock {
                val disconnecting = mutableState.value as? ChampionBleState.Disconnecting
                if (activeTransport !== current || disconnecting?.session?.id == current.session.id) {
                    null
                } else {
                    stateGeneration
                }
            } ?: return
            cleanupSession(
                session = current.session,
                queue = current.queue,
                error = error,
                notifications = current.notifications,
                streams = current.streams,
                jobs = current.internalJobs,
                disconnectReason = ChampionDisconnectReason.TRANSPORT_FAILURE,
                expectedTransport = current,
                terminalState = ChampionBleState.Failed(current.session, error),
                terminalGeneration = generation,
            )
        }
    }

    private suspend fun cleanupSession(
        session: ChampionSession,
        queue: ChampionGattQueue?,
        error: ChampionBleError?,
        notifications: ChampionNotificationHub? = null,
        streams: ChampionTransportStreams? = null,
        jobs: List<Job> = emptyList(),
        disconnectReason: ChampionDisconnectReason = ChampionDisconnectReason.CLEANUP,
        expectedTransport: ChampionTransportSession? = null,
        expectedPending: PendingConnection? = null,
        terminalState: ChampionBleState? = null,
        terminalGeneration: Long? = null,
    ) = withContext(NonCancellable) {
        jobs.forEach { it.cancel() }
        runCleanupStep { notifications?.close(disconnectReason) }
        runCleanupStep { streams?.close(disconnectReason) }
        runCleanupStep { queue?.disconnect(disconnectReason) }
        runCleanupStep { client.disconnect(session.id, disconnectReason) }
        runCleanupStep { client.close(session.id) }
        mutex.withLock {
            if (expectedPending != null && pendingConnection === expectedPending) {
                expectedPending.connected.complete(false)
                pendingConnection = null
            }
            val currentTransport = activeTransport
            if (expectedTransport != null &&
                currentTransport === expectedTransport &&
                currentTransport.session.id == session.id
            ) {
                activeTransport = null
                if (terminalState != null && terminalGeneration == stateGeneration) {
                    mutableState.value = terminalState
                }
            }
        }
        runCleanupStep { protocolLogger?.logTransportState(session.id, "session_cleanup", error) }
    }

    private suspend fun runCleanupStep(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // Cleanup é best-effort por recurso; identidade/estado ainda precisam ser finalizados.
        }
    }

    private suspend fun beginStateOperation(state: ChampionBleState? = null): Long = mutex.withLock {
        stateGeneration += 1
        pendingConnection?.connected?.complete(false)
        pendingConnection = null
        state?.let { mutableState.value = it }
        stateGeneration
    }

    private suspend fun beginExpectedTransportOperation(
        expectedTransport: ChampionTransportSession,
    ): Long? = mutex.withLock {
        if (activeTransport !== expectedTransport) return@withLock null
        stateGeneration += 1
        pendingConnection?.connected?.complete(false)
        pendingConnection = null
        stateGeneration
    }

    private suspend fun publishStateIfCurrent(
        generation: Long,
        state: ChampionBleState,
    ): Boolean = mutex.withLock {
        if (stateGeneration != generation) {
            false
        } else {
            mutableState.value = state
            true
        }
    }

    private suspend fun publishScanResultIfCurrent(
        generation: Long,
        result: ChampionBleResult<List<ChampionDiscoveredDevice>>,
    ): Boolean = mutex.withLock {
        if (stateGeneration != generation) return@withLock false
        val transport = activeTransport
        mutableState.value = when {
            transport != null -> ChampionBleState.TransportReady(transport.session, transport.capabilities.snapshot)
            result is ChampionBleResult.Failure &&
                result.error !is ChampionBleError.OperationCancelled -> ChampionBleState.Failed(null, result.error)
            else -> ChampionBleState.Disconnected
        }
        true
    }

    private suspend fun publishDisconnectedAfterCancelledConnectIfIdle(generation: Long): Boolean =
        mutex.withLock {
            if (stateGeneration != generation || activeTransport != null || pendingConnection != null) {
                false
            } else {
                mutableState.value = ChampionBleState.Disconnected
                true
            }
        }

    private suspend fun isCurrentGeneration(generation: Long): Boolean =
        mutex.withLock { stateGeneration == generation }

    private fun <T> cancelledResult(): ChampionBleResult<T> = ChampionBleResult.Failure(
        ChampionBleError.OperationCancelled(ChampionCancellationReason.CLEANUP),
    )

    private data class PendingConnection(
        val sessionId: ChampionSessionId,
        val generation: Long,
        val connected: CompletableDeferred<Boolean> = CompletableDeferred(),
    )

    private data class ConnectionResources(
        var session: ChampionSession? = null,
        var pending: PendingConnection? = null,
        var queue: ChampionGattQueue? = null,
        var notifications: ChampionNotificationHub? = null,
        var streams: ChampionTransportStreams? = null,
        val jobs: MutableList<Job> = mutableListOf(),
        var transport: ChampionTransportSession? = null,
    )

    private sealed interface DisconnectSelection {
        data class Active(val transport: ChampionTransportSession) : DisconnectSelection
        data object None : DisconnectSelection
        data object Stale : DisconnectSelection
    }
}
