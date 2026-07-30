package br.com.mwconsulting.championcompanion.core.location

import br.com.mwconsulting.championcompanion.core.model.AppendWorkoutRoutePointsInput
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteAppendCursor
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointCandidate
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterInput
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterPolicy
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointFilterResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointRejectionReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRecoverySnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegment
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegmentReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteState
import java.time.Duration
import java.time.Instant

data class WorkoutRouteAccumulatorPolicy(
    val prolongedGap: Duration = Duration.ofSeconds(30),
    val pointFilter: WorkoutRoutePointFilterPolicy = WorkoutRoutePointFilterPolicy(),
) {
    init {
        require(!prolongedGap.isNegative && !prolongedGap.isZero)
    }
}

sealed interface WorkoutRouteAccumulationResult {
    val accumulator: WorkoutRouteAccumulator

    data class Accepted(
        override val accumulator: WorkoutRouteAccumulator,
        val point: WorkoutRoutePoint,
        val distanceMeters: Double,
    ) : WorkoutRouteAccumulationResult

    data class Rejected(
        override val accumulator: WorkoutRouteAccumulator,
        val reason: WorkoutRoutePointRejectionReason,
    ) : WorkoutRouteAccumulationResult

    data class BatchFull(override val accumulator: WorkoutRouteAccumulator) : WorkoutRouteAccumulationResult
}

class WorkoutRouteAccumulator private constructor(
    val routeId: WorkoutRouteId,
    val sessionId: AppWorkoutSessionId,
    val runtimeGeneration: Long,
    val expectedRevision: Long,
    val cursor: WorkoutRouteAppendCursor,
    val metrics: WorkoutRouteMetrics,
    val previousAcceptedPoint: WorkoutRoutePoint?,
    val segments: List<WorkoutRouteSegment>,
    val pendingPoints: List<WorkoutRoutePoint>,
    val rejectionCounts: Map<WorkoutRoutePointRejectionReason, Long>,
    val policy: WorkoutRouteAccumulatorPolicy,
    private val requestedSegmentReason: WorkoutRouteSegmentReason?,
) {
    val hasPendingBatch: Boolean get() = pendingPoints.isNotEmpty()
    val isBatchFull: Boolean get() = pendingPoints.size == AppendWorkoutRoutePointsInput.MAX_ROUTE_POINT_BATCH_SIZE
    val rejectedPointCount: Long get() = rejectionCounts.values.sum()

    fun requestNewSegment(reason: WorkoutRouteSegmentReason): WorkoutRouteAccumulator {
        require(reason != WorkoutRouteSegmentReason.SESSION_START)
        if (previousAcceptedPoint == null) return this
        return copy(requestedSegmentReason = reason)
    }

    fun accumulate(
        pointId: WorkoutRoutePointId,
        candidate: WorkoutRoutePointCandidate,
        observedAt: Instant,
    ): WorkoutRouteAccumulationResult {
        if (isBatchFull) return WorkoutRouteAccumulationResult.BatchFull(this)

        val previous = previousAcceptedPoint
        val automaticReason = if (
            previous != null && candidate.sampledAt.isAfter(previous.sampledAt) &&
            Duration.between(previous.sampledAt, candidate.sampledAt) > policy.prolongedGap
        ) WorkoutRouteSegmentReason.PROLONGED_GAP else null
        val segmentReason = when {
            previous == null -> WorkoutRouteSegmentReason.SESSION_START
            requestedSegmentReason != null -> requestedSegmentReason
            else -> automaticReason
        }
        val segmentIndex = when {
            previous == null -> 0
            segmentReason != null -> previous.segmentIndex + 1
            else -> previous.segmentIndex
        }
        val sequence = (previous?.sequence ?: -1L) + 1L
        return when (val filtered = RoutePointFilter.filter(WorkoutRoutePointFilterInput(
            pointId = pointId,
            routeId = routeId,
            sequence = sequence,
            segmentIndex = segmentIndex,
            observedAt = observedAt,
            expectedRuntimeGeneration = runtimeGeneration,
            candidate = candidate,
            previousAcceptedPoint = previous,
            policy = policy.pointFilter,
        ))) {
            is WorkoutRoutePointFilterResult.Rejected -> {
                val count = rejectionCounts.getOrDefault(filtered.reason, 0L) + 1L
                WorkoutRouteAccumulationResult.Rejected(
                    copy(rejectionCounts = rejectionCounts + (filtered.reason to count)),
                    filtered.reason,
                )
            }
            is WorkoutRoutePointFilterResult.Accepted -> {
                val point = filtered.point
                val updatedSegments = updateSegments(point, segmentReason, filtered.distanceFromPreviousMeters)
                val acceptedCount = metrics.acceptedPointCount + 1L
                val accuracyTotal = (metrics.averageHorizontalAccuracyMeters ?: 0.0) * metrics.acceptedPointCount +
                    point.horizontalAccuracyMeters
                val updatedMetrics = WorkoutRouteMetrics(
                    acceptedPointCount = acceptedCount,
                    segmentCount = updatedSegments.size,
                    distanceMeters = metrics.distanceMeters + filtered.distanceFromPreviousMeters,
                    averageHorizontalAccuracyMeters = accuracyTotal / acceptedCount,
                )
                val updated = copy(
                    metrics = updatedMetrics,
                    previousAcceptedPoint = point,
                    segments = updatedSegments,
                    pendingPoints = pendingPoints + point,
                    requestedSegmentReason = null,
                )
                WorkoutRouteAccumulationResult.Accepted(updated, point, filtered.distanceFromPreviousMeters)
            }
        }
    }

    fun buildAppendInput(): AppendWorkoutRoutePointsInput {
        require(pendingPoints.isNotEmpty())
        val pendingSegmentIndices = pendingPoints.map { it.segmentIndex }.distinct()
        return AppendWorkoutRoutePointsInput(
            routeId = routeId,
            sessionId = sessionId,
            expectedRevision = expectedRevision,
            runtimeGeneration = runtimeGeneration,
            cursor = cursor,
            points = pendingPoints,
            segments = pendingSegmentIndices.map { index -> segments.single { it.index == index } },
            metrics = metrics,
        )
    }

    /** Confirma somente retorno coerente do append; nenhuma persistência ocorre aqui. */
    fun confirmPersisted(persistedRoute: WorkoutRoute): WorkoutRouteAccumulator {
        require(pendingPoints.isNotEmpty())
        require(persistedRoute.id == routeId && persistedRoute.sessionId == sessionId)
        require(persistedRoute.state == WorkoutRouteState.CAPTURING)
        require(persistedRoute.revision == expectedRevision + 1L && persistedRoute.metrics == metrics)
        val last = pendingPoints.last()
        return copy(
            expectedRevision = persistedRoute.revision,
            cursor = WorkoutRouteAppendCursor(routeId, last.sequence, last.sampledAt, last.segmentIndex),
            pendingPoints = emptyList(),
        )
    }

    private fun updateSegments(
        point: WorkoutRoutePoint,
        segmentReason: WorkoutRouteSegmentReason?,
        distanceMeters: Double,
    ): List<WorkoutRouteSegment> {
        val current = segments.lastOrNull()
        return if (current == null || current.index != point.segmentIndex) {
            segments + WorkoutRouteSegment(
                routeId = routeId,
                index = point.segmentIndex,
                reason = requireNotNull(segmentReason),
                firstSequence = point.sequence,
                lastSequence = point.sequence,
                startedAt = point.sampledAt,
                endedAt = point.sampledAt,
                distanceMeters = 0.0,
            )
        } else {
            segments.dropLast(1) + current.copy(
                lastSequence = point.sequence,
                endedAt = point.sampledAt,
                distanceMeters = current.distanceMeters + distanceMeters,
            )
        }
    }

    @Suppress("LongParameterList")
    private fun copy(
        expectedRevision: Long = this.expectedRevision,
        cursor: WorkoutRouteAppendCursor = this.cursor,
        metrics: WorkoutRouteMetrics = this.metrics,
        previousAcceptedPoint: WorkoutRoutePoint? = this.previousAcceptedPoint,
        segments: List<WorkoutRouteSegment> = this.segments,
        pendingPoints: List<WorkoutRoutePoint> = this.pendingPoints,
        rejectionCounts: Map<WorkoutRoutePointRejectionReason, Long> = this.rejectionCounts,
        requestedSegmentReason: WorkoutRouteSegmentReason? = this.requestedSegmentReason,
    ) = WorkoutRouteAccumulator(
        routeId, sessionId, runtimeGeneration, expectedRevision, cursor, metrics,
        previousAcceptedPoint, segments, pendingPoints, rejectionCounts, policy, requestedSegmentReason,
    )

    companion object {
        fun create(
            route: WorkoutRoute,
            runtimeGeneration: Long,
            policy: WorkoutRouteAccumulatorPolicy = WorkoutRouteAccumulatorPolicy(),
        ): WorkoutRouteAccumulator {
            require(route.state == WorkoutRouteState.CAPTURING && route.metrics == WorkoutRouteMetrics.EMPTY)
            require(runtimeGeneration >= 0)
            return WorkoutRouteAccumulator(
                routeId = route.id,
                sessionId = route.sessionId,
                runtimeGeneration = runtimeGeneration,
                expectedRevision = route.revision,
                cursor = WorkoutRouteAppendCursor(route.id, null, null, null),
                metrics = route.metrics,
                previousAcceptedPoint = null,
                segments = emptyList(),
                pendingPoints = emptyList(),
                rejectionCounts = emptyMap(),
                policy = policy,
                requestedSegmentReason = null,
            )
        }

        fun restore(
            snapshot: WorkoutRouteRecoverySnapshot,
            runtimeGeneration: Long,
            reason: WorkoutRouteSegmentReason = WorkoutRouteSegmentReason.PROCESS_RECOVERY,
            policy: WorkoutRouteAccumulatorPolicy = WorkoutRouteAccumulatorPolicy(),
        ): WorkoutRouteAccumulator {
            require(snapshot.route.state == WorkoutRouteState.CAPTURING)
            require(runtimeGeneration >= 0)
            require(reason != WorkoutRouteSegmentReason.SESSION_START)
            return WorkoutRouteAccumulator(
                routeId = snapshot.route.id,
                sessionId = snapshot.route.sessionId,
                runtimeGeneration = runtimeGeneration,
                expectedRevision = snapshot.route.revision,
                cursor = snapshot.cursor,
                metrics = snapshot.route.metrics,
                previousAcceptedPoint = snapshot.lastAcceptedPoint,
                segments = snapshot.segments,
                pendingPoints = emptyList(),
                rejectionCounts = emptyMap(),
                policy = policy,
                requestedSegmentReason = reason.takeIf { snapshot.lastAcceptedPoint != null },
            )
        }
    }
}
