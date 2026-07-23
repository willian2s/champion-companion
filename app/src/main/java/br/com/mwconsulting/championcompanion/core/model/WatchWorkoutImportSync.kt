package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant

const val WATCH_WORKOUT_D3_D4_IMPORT_TYPE = "WATCH_WORKOUT_D3_D4"

data class WatchWorkoutImportAttempt(
    val importAttemptId: String,
    val bleSessionId: String? = null,
    val deviceId: DeviceId? = null,
    val startedAt: Instant? = null,
    val parserVersion: ParserVersion? = null,
)

data class WatchWorkoutImportProgress(
    val discoveredBlockCount: Int = 0,
    val processedBlockCount: Int = 0,
    val currentBlockNumber: Int? = null,
    val currentPacketIndex: Int? = null,
    val totalPacketsInBlock: Int? = null,
    val missingPacketCount: Int = 0,
) {
    val overallProgressFraction: Float?
        get() = if (discoveredBlockCount > 0) {
            (processedBlockCount.toFloat() / discoveredBlockCount.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }

    val blockProgressFraction: Float?
        get() = if (totalPacketsInBlock != null && totalPacketsInBlock > 0 && currentPacketIndex != null) {
            (currentPacketIndex.toFloat() / totalPacketsInBlock.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
}

data class WatchWorkoutImportSummary(
    val discoveredBlockCount: Int = 0,
    val processedBlockCount: Int = 0,
    val insertedCount: Int = 0,
    val duplicateCount: Int = 0,
    val partialCount: Int = 0,
    val invalidCount: Int = 0,
    val timedOutBlockCount: Int = 0,
    val skippedValidCount: Int = 0,
    val missingPacketCount: Int = 0,
    val startedAt: Instant,
    val finishedAt: Instant,
    val timedOut: Boolean = false,
)

enum class WatchWorkoutImportStatus {
    COMPLETED,
    PARTIAL,
    EMPTY,
    CANCELLED,
    FAILED,
}

sealed interface WatchWorkoutImportResult {
    val status: WatchWorkoutImportStatus
    val summary: WatchWorkoutImportSummary?

    data class Completed(override val summary: WatchWorkoutImportSummary) : WatchWorkoutImportResult {
        override val status: WatchWorkoutImportStatus = WatchWorkoutImportStatus.COMPLETED
    }

    data class Partial(override val summary: WatchWorkoutImportSummary) : WatchWorkoutImportResult {
        override val status: WatchWorkoutImportStatus = WatchWorkoutImportStatus.PARTIAL
    }

    data class Empty(override val summary: WatchWorkoutImportSummary) : WatchWorkoutImportResult {
        override val status: WatchWorkoutImportStatus = WatchWorkoutImportStatus.EMPTY
    }

    data class Cancelled(override val summary: WatchWorkoutImportSummary? = null) : WatchWorkoutImportResult {
        override val status: WatchWorkoutImportStatus = WatchWorkoutImportStatus.CANCELLED
    }

    data class Failed(override val summary: WatchWorkoutImportSummary? = null) : WatchWorkoutImportResult {
        override val status: WatchWorkoutImportStatus = WatchWorkoutImportStatus.FAILED
    }
}

data class WatchWorkoutImportMetadata(
    val deviceId: DeviceId,
    val lastAttemptAt: Instant,
    val lastSuccessAt: Instant? = null,
    val lastResult: WatchWorkoutImportStatus,
    val parserVersion: ParserVersion? = null,
    val insertedCount: Int = 0,
    val duplicateCount: Int = 0,
    val conflictCount: Int = 0,
    val invalidCount: Int = 0,
    val missingIndexCount: Int = 0,
    val timedOut: Boolean = false,
    val updatedAt: Instant,
)

data class WatchWorkoutImportError(
    val category: WatchWorkoutImportErrorCategory,
    val message: String,
    val isRetryable: Boolean,
)

enum class WatchWorkoutImportErrorCategory {
    NO_ASSOCIATED_DEVICE,
    PERMISSIONS_REQUIRED,
    BLUETOOTH_DISABLED,
    BLE_OPERATION_IN_PROGRESS,
    CONNECTION_FAILED,
    HANDSHAKE_FAILED,
    F008_UNAVAILABLE,
    SEND_D3_FAILED,
    D3_EMPTY,
    D3_INVALID,
    D3_TIMEOUT,
    SEND_D4_FAILED,
    D4_TIMEOUT,
    D4_EMPTY,
    REQUIRED_PACKET_MISSING,
    INVALID_LAYOUT,
    CRC_MISMATCH,
    STORAGE_FAILED,
    UNEXPECTED_DISCONNECT,
    DIAGNOSTIC_STORAGE_UNAVAILABLE,
    CANCELLED,
    INTERNAL,
}

data class WatchWorkoutParserPolicy(
    val parserValidity: ParserValidity,
    val hasRequiredPackets: Boolean,
    val hasSafeMinimumData: Boolean,
) {
    val shouldPersistActivity: Boolean
        get() = parserValidity == ParserValidity.VALID ||
            (parserValidity == ParserValidity.PARTIAL && hasRequiredPackets && hasSafeMinimumData)
}

enum class WatchWorkoutD4Layout {
    PREFERRED,
    LEGACY,
}

data class WatchWorkoutParserSummary(
    val layout: WatchWorkoutD4Layout? = null,
    val timedOut: Boolean = false,
    val missingPacketIndices: Set<Int> = emptySet(),
    val duplicatePacketIndices: Set<Int> = emptySet(),
    val recordCount: Int? = null,
    val parsedIntervalCount: Int = 0,
    val validHeartRateSampleCount: Int = 0,
    val ignoredHeartRateSampleCount: Int = 0,
    val outOfDurationSampleCount: Int = 0,
    val reportedBlockCrc: Int? = null,
    val crcMatchesD3: Boolean? = null,
)

sealed interface WatchWorkoutParserResult {
    val policy: WatchWorkoutParserPolicy
    val summary: WatchWorkoutParserSummary?

    data class Valid(
        val input: WatchWorkoutParserInput,
        override val summary: WatchWorkoutParserSummary,
    ) : WatchWorkoutParserResult {
        override val policy: WatchWorkoutParserPolicy = WatchWorkoutParserPolicy(
            parserValidity = ParserValidity.VALID,
            hasRequiredPackets = true,
            hasSafeMinimumData = true,
        )
    }

    data class Partial(
        val input: WatchWorkoutParserInput,
        override val summary: WatchWorkoutParserSummary,
    ) : WatchWorkoutParserResult {
        override val policy: WatchWorkoutParserPolicy = WatchWorkoutParserPolicy(
            parserValidity = ParserValidity.PARTIAL,
            hasRequiredPackets = true,
            hasSafeMinimumData = true,
        )
    }

    data class Invalid(
        val errorCategory: WatchWorkoutImportErrorCategory,
        override val summary: WatchWorkoutParserSummary? = null,
    ) : WatchWorkoutParserResult {
        override val policy: WatchWorkoutParserPolicy = WatchWorkoutParserPolicy(
            parserValidity = ParserValidity.INVALID,
            hasRequiredPackets = false,
            hasSafeMinimumData = false,
        )
    }
}
