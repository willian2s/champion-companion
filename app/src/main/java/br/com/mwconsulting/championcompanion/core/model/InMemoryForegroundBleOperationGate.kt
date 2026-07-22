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
    private val activeOperation = MutableStateFlow<ForegroundBleOperationSnapshot?>(null)

    override fun observeActiveOperation(): Flow<ForegroundBleOperationSnapshot?> = activeOperation.asStateFlow()

    override suspend fun tryAcquire(request: ForegroundBleOperationRequest): ForegroundBleOperationAcquireResult = mutex.withLock {
        val current = activeOperation.value
        if (current != null) {
            ForegroundBleOperationAcquireResult.Rejected(current)
        } else {
            val permit = ForegroundBleOperationPermit(
                owner = request.owner,
                operationId = request.operationId,
                acquiredAt = request.requestedAt,
            )
            activeOperation.value = ForegroundBleOperationSnapshot(
                owner = permit.owner,
                operationId = permit.operationId,
                acquiredAt = permit.acquiredAt,
            )
            ForegroundBleOperationAcquireResult.Acquired(permit)
        }
    }

    override suspend fun release(permit: ForegroundBleOperationPermit) {
        mutex.withLock {
            val current = activeOperation.value ?: return
            if (current.owner == permit.owner && current.operationId == permit.operationId) {
                activeOperation.value = null
            }
        }
    }
}
