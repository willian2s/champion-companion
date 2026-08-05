package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import java.time.Instant

internal class DiagnosticPayloadRepositoryTestDouble : DiagnosticPayloadRepository {
    val storedPayloads = mutableListOf<DiagnosticPayload>()

    override suspend fun store(payload: DiagnosticPayload): RepositoryResult<Unit> {
        storedPayloads += payload
        return RepositoryResult.Success(Unit)
    }

    override suspend fun reprocess(payloadId: DiagnosticPayloadId): RepositoryResult<Unit> =
        RepositoryResult.Success(Unit)

    override suspend fun clearExpired(now: Instant): RepositoryResult<Int> = RepositoryResult.Success(0)
}
