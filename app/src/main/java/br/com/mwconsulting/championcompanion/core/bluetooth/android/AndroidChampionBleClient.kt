package br.com.mwconsulting.championcompanion.core.bluetooth.android

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCommand
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattCallbackBridge
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattOperation
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattQueue
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattSessionEvent
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionGattTarget
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSession
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionUuidCatalog
import br.com.mwconsulting.championcompanion.core.bluetooth.SafeGattDetail
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidChampionBleClient @Inject constructor(
    private val gattFacade: AndroidGattFacade,
    private val callbackBridge: ChampionGattCallbackBridge = ChampionGattCallbackBridge(),
) : ChampionBleClient {
    private val mutex = Mutex()
    private val callbackScope = CoroutineScope(Dispatchers.Default)
    private val mutableEvents = MutableSharedFlow<ChampionGattSessionEvent>(extraBufferCapacity = 32)
    private var activeSession: ActiveAndroidSession? = null

    override val events: Flow<ChampionGattSessionEvent> = mutableEvents.asSharedFlow()

    suspend fun discoverySnapshot(sessionId: ChampionSessionId): AndroidGattDiscoverySnapshot? = mutex.withLock {
        val session = activeSession ?: return@withLock null
        if (session.session.id != sessionId) return@withLock null
        session.discoverySnapshot
    }

    override suspend fun openSession(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionSession> = mutex.withLock {
        closeActiveSessionLocked()

        val session = ChampionSession(
            id = ChampionSessionId.random(),
            transportKey = device.transportKey,
            connectedAt = Instant.now(),
        )
        val callback = callbackFor(session)
        val connection = gattFacade.connect(device.transportKey, callback)
            ?: return@withLock ChampionBleResult.Failure(ChampionBleError.ConnectionFailed())

        activeSession = ActiveAndroidSession(
            session = session,
            connection = connection,
            queue = null,
            discoverySnapshot = null,
        )
        ChampionBleResult.Success(session)
    }

    override fun attachQueue(sessionId: ChampionSessionId, queue: ChampionGattQueue) {
        val session = activeSession ?: return
        if (session.session.id != sessionId) return
        activeSession = session.copy(queue = queue)
    }

    override suspend fun execute(operation: ChampionGattOperation) {
        val session = mutex.withLock { activeSession }
        if (session == null) return
        if (session.session.id != operation.sessionId) {
            session.queue?.fail(
                sessionId = operation.sessionId,
                operationId = operation.operationId,
                error = ChampionBleError.StaleSession(
                    expected = session.session.id,
                    received = operation.sessionId,
                ),
            )
            return
        }

        when (operation.type) {
            ChampionOperationType.CONNECT -> {
                session.queue?.complete(operation.sessionId, operation.operationId)
            }

            ChampionOperationType.DISCOVER_SERVICES -> {
                if (!session.connection.discoverServices()) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                }
            }

            ChampionOperationType.READ_CHARACTERISTIC -> {
                val channel = operation.target.channelAliasOrNull()
                val characteristicUuid = channel?.let(ChampionUuidCatalog::channelUuid)
                if (characteristicUuid == null || !session.connection.readCharacteristic(characteristicUuid)) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                }
            }

            ChampionOperationType.WRITE_CHARACTERISTIC -> {
                val channel = operation.target.channelAliasOrNull()
                val characteristicUuid = channel?.let(ChampionUuidCatalog::channelUuid)
                val payload = operation.payloadCopy()
                if (characteristicUuid == null || payload == null || !session.connection.writeCharacteristic(characteristicUuid, payload)) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                }
            }

            ChampionOperationType.WRITE_DESCRIPTOR -> {
                val channel = operation.target.channelAliasOrNull()
                val characteristicUuid = channel?.let(ChampionUuidCatalog::channelUuid)
                val payload = operation.payloadCopy()
                if (characteristicUuid == null || payload == null || !session.connection.writeDescriptor(characteristicUuid, ChampionUuidCatalog.cccd, payload)) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                }
            }

            ChampionOperationType.REQUEST_MTU -> {
                val mtu = operation.payloadCopy()?.toRequestedMtu()
                if (mtu == null || !session.connection.requestMtu(mtu)) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                }
            }

            ChampionOperationType.CONFIGURE_NOTIFICATION,
            ChampionOperationType.CONFIGURE_INDICATION,
            -> {
                val channel = operation.target.channelAliasOrNull()
                val characteristicUuid = channel?.let(ChampionUuidCatalog::channelUuid)
                if (characteristicUuid == null || !session.connection.setCharacteristicNotification(characteristicUuid, true)) {
                    session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
                } else {
                    session.queue?.complete(operation.sessionId, operation.operationId)
                }
            }

            ChampionOperationType.WAIT_FOR_RESPONSE -> Unit

            ChampionOperationType.DISCONNECT -> {
                session.connection.disconnect()
            }

            ChampionOperationType.SCAN -> {
                session.queue?.fail(operation.sessionId, operation.operationId, ChampionBleError.OperationRejected(operation.type))
            }
        }
    }

    override suspend fun disconnect(
        sessionId: ChampionSessionId,
        reason: ChampionDisconnectReason,
    ): ChampionBleResult<Unit> = mutex.withLock {
        val session = activeSession ?: return@withLock ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (session.session.id != sessionId) {
            return@withLock ChampionBleResult.Failure(
                ChampionBleError.StaleSession(expected = session.session.id, received = sessionId),
            )
        }
        session.connection.disconnect()
        ChampionBleResult.Success(Unit)
    }

    override suspend fun close(sessionId: ChampionSessionId): ChampionBleResult<Unit> = mutex.withLock {
        val session = activeSession ?: return@withLock ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
        if (session.session.id != sessionId) {
            return@withLock ChampionBleResult.Failure(
                ChampionBleError.StaleSession(expected = session.session.id, received = sessionId),
            )
        }
        session.connection.close()
        mutableEvents.tryEmit(callbackBridge.closed(sessionId, session.session.transportKey))
        activeSession = null
        ChampionBleResult.Success(Unit)
    }

    private fun callbackFor(session: ChampionSession): AndroidGattCallback = object : AndroidGattCallback {
        override fun onConnectionStateChanged(isConnected: Boolean, detail: SafeGattDetail?) {
            val event = callbackBridge.connectionStateChanged(
                sessionId = session.id,
                transportKey = session.transportKey,
                isConnected = isConnected,
                detail = detail,
            )
            handleEvent(event)
        }

        override fun onServicesDiscovered(
            serviceUuids: Set<java.util.UUID>,
            snapshot: AndroidGattDiscoverySnapshot,
            detail: SafeGattDetail?,
        ) {
            val current = activeSession
            if (current != null && current.session.id == session.id) {
                activeSession = current.copy(discoverySnapshot = snapshot)
            }
            handleEvent(callbackBridge.servicesDiscovered(session.id, serviceUuids, detail))
        }

        override fun onCharacteristicRead(characteristicUuid: java.util.UUID, value: ByteArray, detail: SafeGattDetail?) {
            handleEvent(callbackBridge.characteristicRead(session.id, characteristicUuid, value, detail))
        }

        override fun onCharacteristicWrite(characteristicUuid: java.util.UUID, detail: SafeGattDetail?) {
            handleEvent(callbackBridge.characteristicWrite(session.id, characteristicUuid, detail))
        }

        override fun onCharacteristicChanged(characteristicUuid: java.util.UUID, value: ByteArray) {
            handleEvent(callbackBridge.characteristicChanged(session.id, characteristicUuid, value))
        }

        override fun onDescriptorWrite(characteristicUuid: java.util.UUID?, descriptorUuid: java.util.UUID, detail: SafeGattDetail?) {
            handleEvent(callbackBridge.descriptorWrite(session.id, characteristicUuid, descriptorUuid, detail))
        }

        override fun onMtuChanged(mtu: Int, detail: SafeGattDetail?) {
            handleEvent(callbackBridge.mtuChanged(session.id, mtu, detail))
        }
    }

    private fun handleEvent(event: ChampionGattSessionEvent) {
        val session = activeSession ?: return
        if (session.session.id != event.sessionId) return

        mutableEvents.tryEmit(event)
        val queue = session.queue ?: return
        val activeOperation = queue.activeOperation.value ?: return
        if (activeOperation.sessionId != event.sessionId) return

        when (event) {
            is ChampionGattSessionEvent.ConnectionStateChanged -> when {
                activeOperation.type == ChampionOperationType.DISCONNECT && !event.isConnected ->
                    queueLaunch(queue) { complete(event.sessionId, activeOperation.operationId) }

                activeOperation.type == ChampionOperationType.CONNECT && event.isConnected ->
                    queueLaunch(queue) { complete(event.sessionId, activeOperation.operationId) }

                !event.isConnected ->
                    queueLaunch(queue) {
                        fail(
                            event.sessionId,
                            activeOperation.operationId,
                            ChampionBleError.UnexpectedDisconnect(event.detail),
                        )
                    }
            }

            is ChampionGattSessionEvent.ServicesDiscovered ->
                queueResolveByDetail(queue, event.sessionId, activeOperation, event.detail, ChampionBleError.DiscoveryFailed(event.detail))

            is ChampionGattSessionEvent.CharacteristicRead ->
                queueResolveByDetail(queue, event.sessionId, activeOperation, event.detail, ChampionBleError.CallbackFailure(activeOperation.type, event.detail))

            is ChampionGattSessionEvent.CharacteristicWrite ->
                queueResolveByDetail(queue, event.sessionId, activeOperation, event.detail, ChampionBleError.CallbackFailure(activeOperation.type, event.detail))

            is ChampionGattSessionEvent.DescriptorWrite ->
                queueResolveByDetail(queue, event.sessionId, activeOperation, event.detail, ChampionBleError.CallbackFailure(activeOperation.type, event.detail))

            is ChampionGattSessionEvent.MtuChanged ->
                queueResolveByDetail(queue, event.sessionId, activeOperation, event.detail, ChampionBleError.CallbackFailure(activeOperation.type, event.detail))

            is ChampionGattSessionEvent.CharacteristicChanged,
            is ChampionGattSessionEvent.Closed,
            -> Unit
        }
    }

    private fun queueResolveByDetail(
        queue: ChampionGattQueue,
        sessionId: ChampionSessionId,
        operation: ChampionGattOperation,
        detail: SafeGattDetail?,
        failure: ChampionBleError,
    ) {
        if (detail?.statusCode == null || detail.statusCode == 0) {
            queueLaunch(queue) { complete(sessionId, operation.operationId) }
        } else {
            queueLaunch(queue) { fail(sessionId, operation.operationId, failure) }
        }
    }

    private fun queueLaunch(queue: ChampionGattQueue, block: suspend ChampionGattQueue.() -> Unit) {
        callbackScope.launch {
            queue.block()
        }
    }

    private suspend fun closeActiveSessionLocked() {
        val session = activeSession ?: return
        session.connection.close()
        mutableEvents.tryEmit(callbackBridge.closed(session.session.id, session.session.transportKey))
        activeSession = null
    }

    private fun ChampionGattTarget.channelAliasOrNull(): ChampionChannelAlias? = when (this) {
        is ChampionGattTarget.Channel -> alias
        ChampionGattTarget.Session -> null
    }

    private fun ByteArray.toRequestedMtu(): Int? {
        if (size < 2) return null
        return ((this[0].toInt() and 0xFF) shl 8) or (this[1].toInt() and 0xFF)
    }

    private data class ActiveAndroidSession(
        val session: ChampionSession,
        val connection: AndroidGattConnection,
        val queue: ChampionGattQueue?,
        val discoverySnapshot: AndroidGattDiscoverySnapshot?,
    )
}
