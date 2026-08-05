package br.com.mwconsulting.championcompanion.core.model

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class InMemoryForegroundBleOperationGate @Inject constructor() : ForegroundBleOperationGate {
    private val mutex = Mutex()
    private var activePermit: ForegroundBleOperationPermit? = null
    private val activeOperation = MutableStateFlow<ForegroundBleOperationSnapshot?>(null)

    override fun observeActiveOperation(): Flow<ForegroundBleOperationSnapshot?> = activeOperation.asStateFlow()

    override suspend fun tryAcquire(request: ForegroundBleOperationRequest): ForegroundBleOperationAcquireResult = mutex.withLock {
        val current = activePermit
        if (current != null) {
            ForegroundBleOperationAcquireResult.Rejected(current.toSnapshot())
        } else {
            val permit = ForegroundBleOperationPermit(
                owner = request.owner,
                operationId = request.operationId,
                acquiredAt = request.requestedAt,
            )
            activePermit = permit
            activeOperation.value = permit.toSnapshot()
            ForegroundBleOperationAcquireResult.Acquired(permit)
        }
    }

    override suspend fun release(permit: ForegroundBleOperationPermit) {
        mutex.withLock {
            if (activePermit === permit) {
                activePermit = null
                activeOperation.value = null
            }
        }
    }

    private fun ForegroundBleOperationPermit.toSnapshot(): ForegroundBleOperationSnapshot =
        ForegroundBleOperationSnapshot(
            owner = owner,
            operationId = operationId,
            acquiredAt = acquiredAt,
        )
}
