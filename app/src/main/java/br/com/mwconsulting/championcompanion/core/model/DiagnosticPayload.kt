package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class DiagnosticCaptureFlow {
    WATCH_HISTORY,
    WATCH_WORKOUT,
    MANUAL_CAPTURE,
}

enum class DiagnosticParseResult {
    COMPLETED,
    PARTIAL,
    INVALID,
    TECHNICAL_ERROR,
}

enum class SafeDiagnosticError {
    TRANSPORT_UNAVAILABLE,
    PARSER_FAILURE,
    STORAGE_UNAVAILABLE,
}

enum class DiagnosticDerivedOutputType {
    WATCH_WORKOUT_IMPORT,
    HEART_RATE_MEASUREMENT,
    DAILY_SUMMARY,
}

data class WatchWorkoutDiagnosticCapturePolicy(
    val flow: DiagnosticCaptureFlow = DiagnosticCaptureFlow.WATCH_WORKOUT,
    val derivedOutputType: DiagnosticDerivedOutputType = DiagnosticDerivedOutputType.WATCH_WORKOUT_IMPORT,
    val maximumAge: Duration = DiagnosticRetention.maximumAge,
    val maximumBytes: Long = DiagnosticRetention.maximumBytes,
    val captureValidInRelease: Boolean = false,
    val capturePartialInRelease: Boolean = true,
    val captureInvalidInRelease: Boolean = true,
    val captureTechnicalErrorInRelease: Boolean = true,
    val captureValidInDebugWhenExplicit: Boolean = true,
) {
    fun shouldCapture(
        result: DiagnosticParseResult,
        isDebugBuild: Boolean,
        explicitDebugCaptureRequested: Boolean,
    ): Boolean = when (result) {
        DiagnosticParseResult.COMPLETED -> isDebugBuild && explicitDebugCaptureRequested && captureValidInDebugWhenExplicit
        DiagnosticParseResult.PARTIAL -> capturePartialInRelease || (isDebugBuild && explicitDebugCaptureRequested)
        DiagnosticParseResult.INVALID -> captureInvalidInRelease || (isDebugBuild && explicitDebugCaptureRequested)
        DiagnosticParseResult.TECHNICAL_ERROR -> captureTechnicalErrorInRelease || (isDebugBuild && explicitDebugCaptureRequested)
    }

    fun derivedOutputKey(payloadId: DiagnosticPayloadId, ordinal: Int = 0): DiagnosticDerivedOutputKey =
        DiagnosticDerivedOutputKey(payloadId = payloadId, type = derivedOutputType, ordinal = ordinal)
}

val WatchWorkoutDiagnostics: WatchWorkoutDiagnosticCapturePolicy = WatchWorkoutDiagnosticCapturePolicy()

data class DiagnosticDerivedOutputKey(
    val payloadId: DiagnosticPayloadId,
    val type: DiagnosticDerivedOutputType,
    val ordinal: Int,
)

data class DiagnosticPayloadCapture(
    val command: String,
    val flow: DiagnosticCaptureFlow,
    val privateBytes: ByteArray,
    val deviceId: DeviceId?,
    val firmware: String?,
    val parserVersion: ParserVersion,
    val capturedAt: Instant,
)

/** Private binary content remains local and is intentionally not a UI or logging value. */
@ConsistentCopyVisibility
data class DiagnosticPayload private constructor(
    val id: DiagnosticPayloadId,
    val command: String,
    val flow: DiagnosticCaptureFlow,
    private val bytes: ByteArray,
    val byteCount: Long,
    val deviceId: DeviceId?,
    val firmware: String?,
    val parserVersion: ParserVersion,
    val result: DiagnosticParseResult,
    val safeError: SafeDiagnosticError?,
    val capturedAt: Instant,
    val derivedOutputs: Set<DiagnosticDerivedOutputKey>,
) {
    fun bytesForPrivateStorage(): ByteArray = bytes.copyOf()

    fun recordDerivedOutput(key: DiagnosticDerivedOutputKey): DomainResult<DiagnosticPayload> =
        if (key.payloadId != id || key.ordinal < 0) {
            DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        } else {
            DomainResult.Success(copy(derivedOutputs = derivedOutputs + key))
        }

    companion object {
        fun create(
            id: DiagnosticPayloadId,
            capture: DiagnosticPayloadCapture,
            result: DiagnosticParseResult,
            safeError: SafeDiagnosticError? = null,
        ): DomainResult<DiagnosticPayload> {
            if (capture.command.isBlank() || capture.privateBytes.isEmpty() ||
                (result == DiagnosticParseResult.TECHNICAL_ERROR) != (safeError != null)
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
            return DomainResult.Success(DiagnosticPayload(
                id = id,
                command = capture.command,
                flow = capture.flow,
                bytes = capture.privateBytes.copyOf(),
                byteCount = capture.privateBytes.size.toLong(),
                deviceId = capture.deviceId,
                firmware = capture.firmware?.takeIf { it.isNotBlank() },
                parserVersion = capture.parserVersion,
                result = result,
                safeError = safeError,
                capturedAt = capture.capturedAt,
                derivedOutputs = emptySet(),
            ))
        }

        fun parserValidityFor(result: DiagnosticParseResult): ParserValidity? = when (result) {
            DiagnosticParseResult.COMPLETED -> ParserValidity.VALID
            DiagnosticParseResult.PARTIAL -> ParserValidity.PARTIAL
            DiagnosticParseResult.INVALID -> ParserValidity.INVALID
            DiagnosticParseResult.TECHNICAL_ERROR -> null
        }
    }
}

object DiagnosticRetention {
    val maximumAge: Duration = Duration.ofDays(14)
    const val maximumBytes: Long = 10L * 1024L * 1024L

    /** Returns payload IDs to delete, oldest first. Domain-derived records are not part of deletion. */
    fun payloadIdsToDelete(
        payloads: Collection<DiagnosticPayload>,
        now: Instant,
    ): List<DiagnosticPayloadId> {
        val oldestFirst = payloads.sortedBy { it.capturedAt }
        val expired = oldestFirst.filter { it.capturedAt.isBefore(now.minus(maximumAge)) }
        val retained = oldestFirst - expired.toSet()
        var retainedBytes = retained.sumOf { it.byteCount }
        val overLimit = buildList {
            for (payload in retained) {
                if (retainedBytes <= maximumBytes) break
                add(payload)
                retainedBytes -= payload.byteCount
            }
        }
        return (expired + overLimit).distinct().map { it.id }
    }
}
