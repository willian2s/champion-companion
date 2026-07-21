package br.com.mwconsulting.championcompanion.core.bluetooth

import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.withTimeoutOrNull

fun interface ChampionCapabilityResolver {
    suspend fun resolve(
        session: ChampionSession,
        queue: ChampionGattQueue,
    ): ChampionBleResult<ChampionSessionCapabilities>
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
    private val mutableState = MutableStateFlow<ChampionBleState>(ChampionBleState.Disconnected)
    private var activeTransport: ChampionTransportSession? = null
    private var pendingConnection: PendingConnection? = null

    val state: StateFlow<ChampionBleState> = mutableState.asStateFlow()

    init {
        scope.launch {
            client.events.collect(::handleClientEvent)
        }
    }

    suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>> {
        mutableState.value = ChampionBleState.Scanning(request)
        val result = scanner.scan(request)
        if (result is ChampionBleResult.Failure) {
            mutableState.value = ChampionBleState.Failed(null, result.error)
        } else if (activeTransport == null) {
            mutableState.value = ChampionBleState.Disconnected
        }
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
        disconnect(ChampionDisconnectReason.CLEANUP)

        val session = when (val openResult = client.openSession(device)) {
            is ChampionBleResult.Failure -> {
                mutableState.value = ChampionBleState.Failed(null, openResult.error)
                return ChampionBleResult.Failure(openResult.error)
            }

            is ChampionBleResult.Success -> openResult.value
        }

        val queue = ChampionGattQueue(
            sessionId = session.id,
            parentScope = scope,
            operationExecutor = client::execute,
        )
        client.attachQueue(session.id, queue)
        mutableState.value = ChampionBleState.Connecting(session, device)

        pendingConnection = PendingConnection(session.id)
        val connected = withTimeoutOrNull(timeouts.connect) {
            pendingConnection?.connected?.await()
        } == true
        if (!connected) {
            val error = ChampionBleError.Timeout(ChampionOperationType.CONNECT, timeouts.connect)
            cleanupSession(session, null, error)
            mutableState.value = ChampionBleState.Failed(session, error)
            return ChampionBleResult.Failure(error)
        }

        mutableState.value = ChampionBleState.DiscoveringServices(session)
        val discoveryResult = queue.enqueue(
            ChampionGattOperation.create(
                sessionId = session.id,
                type = ChampionOperationType.DISCOVER_SERVICES,
                target = ChampionGattTarget.Session,
                timeout = timeouts.discovery,
            ),
        )
        if (discoveryResult is ChampionGattTerminalResult.Failure) {
            cleanupSession(session, queue, discoveryResult.error)
            mutableState.value = ChampionBleState.Failed(session, discoveryResult.error)
            return ChampionBleResult.Failure(discoveryResult.error)
        }

        mutableState.value = ChampionBleState.ResolvingChannels(session)
        val capabilities = when (val resolved = capabilityResolver.resolve(session, queue)) {
            is ChampionBleResult.Failure -> {
                cleanupSession(session, queue, resolved.error)
                mutableState.value = ChampionBleState.Failed(session, resolved.error)
                return ChampionBleResult.Failure(resolved.error)
            }

            is ChampionBleResult.Success -> resolved.value
        }

        mutableState.value = ChampionBleState.ConfiguringSubscriptions(session, capabilities.snapshot)
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
        val notificationHub = ChampionNotificationHub(
            sessionCapabilities = capabilities,
            queue = queue,
            sessionEvents = client.events,
            scope = scope,
        )

        val requiredSubscription = notificationHub.subscribe(ChampionChannelAlias.F008_NOTIFY)
        if (requiredSubscription is ChampionBleResult.Failure) {
            cleanupSession(session, queue, requiredSubscription.error, notificationHub, streams)
            mutableState.value = ChampionBleState.Failed(session, requiredSubscription.error)
            return ChampionBleResult.Failure(requiredSubscription.error)
        }

        val requiredCollector = scope.launch {
            (requiredSubscription as ChampionBleResult.Success).value.collect { packet ->
                protocolLogger?.logPacketObserved(packet, consumedByMatcher = false)
            }
        }

        val transport = ChampionTransportSession(
            session = session,
            capabilities = capabilities,
            queue = queue,
            notifications = notificationHub,
            streams = streams,
            internalJobs = listOf(requiredCollector),
        )

        mutex.withLock {
            activeTransport = transport
        }
        mutableState.value = ChampionBleState.TransportReady(session, capabilities.snapshot)
        protocolLogger?.logTransportState(session.id, "transport_ready", null)
        return ChampionBleResult.Success(transport)
    }

    suspend fun currentSession(): ChampionTransportSession? = mutex.withLock { activeTransport }

    suspend fun disconnect(
        reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED,
    ): ChampionBleResult<Unit> {
        scanner.cancelScan(ChampionCancellationReason.DISCONNECT)
        val transport = mutex.withLock { activeTransport } ?: run {
            mutableState.value = ChampionBleState.Disconnected
            return ChampionBleResult.Success(Unit)
        }

        mutableState.value = ChampionBleState.Disconnecting(transport.session, reason)
        cleanupSession(
            session = transport.session,
            queue = transport.queue,
            error = null,
            notifications = transport.notifications,
            streams = transport.streams,
            jobs = transport.internalJobs,
            disconnectReason = reason,
        )
        mutableState.value = ChampionBleState.Disconnected
        return ChampionBleResult.Success(Unit)
    }

    private suspend fun handleClientEvent(event: ChampionGattSessionEvent) {
        val current = mutex.withLock { activeTransport }

        if (event is ChampionGattSessionEvent.ConnectionStateChanged) {
            val pending = pendingConnection
            if (pending != null && pending.sessionId == event.sessionId && event.isConnected) {
                pending.connected.complete(true)
                pendingConnection = null
            }
        }

        if (current == null || current.session.id != event.sessionId) return
        if (event is ChampionGattSessionEvent.ConnectionStateChanged && !event.isConnected) {
            val error = ChampionBleError.UnexpectedDisconnect(event.detail)
            cleanupSession(
                session = current.session,
                queue = current.queue,
                error = error,
                notifications = current.notifications,
                streams = current.streams,
                jobs = current.internalJobs,
                disconnectReason = ChampionDisconnectReason.TRANSPORT_FAILURE,
            )
            mutableState.value = ChampionBleState.Failed(current.session, error)
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
    ) {
        jobs.forEach { it.cancel() }
        notifications?.close(disconnectReason)
        streams?.close(disconnectReason)
        queue?.disconnect(disconnectReason)
        client.disconnect(session.id, disconnectReason)
        client.close(session.id)
        mutex.withLock {
            if (activeTransport?.session?.id == session.id) {
                activeTransport = null
            }
        }
        protocolLogger?.logTransportState(session.id, "session_cleanup", error)
    }

    private data class PendingConnection(
        val sessionId: ChampionSessionId,
        val connected: CompletableDeferred<Boolean> = CompletableDeferred(),
    )
}
