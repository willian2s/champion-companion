package br.com.mwconsulting.championcompanion.core.designsystem.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Selection delivered by shared chart interaction, without exposing Vico. */
@Immutable
data class ChampionChartSelection(
    val seriesKey: String,
    val point: ChampionChartPoint,
)

/** Common, locale-aware numeric formatters for chart contracts. */
object ChampionChartFormatters {
    fun decimal(
        minimumFractionDigits: Int = 0,
        maximumFractionDigits: Int = minimumFractionDigits,
    ): ChampionChartValueFormatter {
        require(minimumFractionDigits >= 0)
        require(maximumFractionDigits >= minimumFractionDigits)
        return ChampionChartValueFormatter { value ->
            NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                this.minimumFractionDigits = minimumFractionDigits
                this.maximumFractionDigits = maximumFractionDigits
                isGroupingUsed = false
            }.format(value)
        }
    }

    fun integer(): ChampionChartValueFormatter = decimal()
}

private data class RenderSeriesKey(
    val sourceKey: String,
    val segment: Int,
)

private data class RenderSeries(
    val key: RenderSeriesKey,
    val points: List<ChampionChartPoint>,
)

private enum class ChartKind {
    LINE,
    BAR,
}

private val linePointSpacing = 32.dp
private val linePointSize = 8.dp
private val columnWidth = 16.dp
private val columnCollectionSpacing = 16.dp
private val groupedColumnSpacing = 8.dp

/** Shared line renderer. Scroll and zoom stay opt-in through the contract. */
@Composable
fun ChampionLineChart(
    data: ChampionCartesianChartData,
    modifier: Modifier = Modifier,
    onSelectionChanged: ((List<ChampionChartSelection>) -> Unit)? = null,
) {
    ChampionChartFrame(data = data, modifier = modifier) {
        ChampionCartesianRenderer(
            data = data,
            kind = ChartKind.LINE,
            onSelectionChanged = onSelectionChanged,
        )
    }
}

/** Shared column renderer. Null points are omitted, never converted to zero. */
@Composable
fun ChampionBarChart(
    data: ChampionCartesianChartData,
    modifier: Modifier = Modifier,
    onSelectionChanged: ((List<ChampionChartSelection>) -> Unit)? = null,
) {
    ChampionChartFrame(data = data, modifier = modifier) {
        ChampionCartesianRenderer(
            data = data,
            kind = ChartKind.BAR,
            onSelectionChanged = onSelectionChanged,
        )
    }
}

/** Shared time-series entry point, using the contract's real X formatter. */
@Composable
fun ChampionTimeSeriesChart(
    data: ChampionCartesianChartData,
    modifier: Modifier = Modifier,
    onSelectionChanged: ((List<ChampionChartSelection>) -> Unit)? = null,
) {
    ChampionLineChart(
        data = data,
        modifier = modifier,
        onSelectionChanged = onSelectionChanged,
    )
}

@Composable
private fun ChampionChartFrame(
    data: ChampionCartesianChartData,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val summary = buildAccessibleSummary(data)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 192.dp)
            .semantics {
                contentDescription = summary
                if (!data.hasRecordedValues) stateDescription = "Sem dados para exibir"
            },
    ) {
        if (data.hasRecordedValues) {
            content()
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = "Sem dados para exibir",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ChampionCartesianRenderer(
    data: ChampionCartesianChartData,
    kind: ChartKind,
    onSelectionChanged: ((List<ChampionChartSelection>) -> Unit)?,
) {
    val theme = championChartTheme()
    val yFormatter = remember(data.yAxis) {
        CartesianValueFormatter { _, value, _ -> formatAxisValue(data.yAxis, value) }
    }
    val xFormatter = remember(data.xAxis) {
        CartesianValueFormatter { _, value, _ -> data.xAxis.formatter.format(value) }
    }
    val axisLine = rememberLineComponent(fill = Fill(theme.axis), thickness = 1.dp)
    val axisGuideline = rememberAxisGuidelineComponent(fill = Fill(theme.grid), thickness = 1.dp)
    val rangeProvider = remember(data.series, data.xAxis, data.yAxis) {
        intrinsicRangeProvider(data)
    }
    val visualSeries = remember(data.series, kind) {
        when (kind) {
            ChartKind.LINE -> data.series.flatMap(::lineSegments)
            ChartKind.BAR -> data.series.mapNotNull { series ->
                val points = series.points.filter { it.y != null }
                points.takeIf { it.isNotEmpty() }?.let {
                    RenderSeries(RenderSeriesKey(series.key, 0), it)
                }
            }
        }
    }
    val model = when (kind) {
        ChartKind.LINE -> CartesianChartModel(
            LineCartesianLayerModel.build {
                visualSeries.forEach { series ->
                    series(x = series.points.map { it.x }, y = series.points.mapNotNull { it.y }, key = series.key)
                }
            },
        )
        ChartKind.BAR -> CartesianChartModel(
            ColumnCartesianLayerModel.build {
                visualSeries.forEach { series ->
                    series(x = series.points.map { it.x }, y = series.points.mapNotNull { it.y }, key = series.key)
                }
            },
        )
    }
    val palette = listOf(theme.primarySeries, theme.secondarySeries, theme.tertiarySeries)
    val lineProvider = if (kind == ChartKind.LINE) {
        LineCartesianLayer.LineProvider.series(
            visualSeries.map { series ->
                val color = palette[
                    data.series.indexOfFirst { it.key == series.key.sourceKey }
                        .coerceAtLeast(0) % palette.size
                ]
                val point = LineCartesianLayer.Point(
                    component = rememberShapeComponent(fill = Fill(color), shape = CircleShape),
                    size = 8.dp,
                )
                LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(Fill(color)),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                    pointProvider = LineCartesianLayer.PointProvider.single(point),
                )
            },
        )
    } else {
        null
    }
    val layer = when (kind) {
        ChartKind.LINE -> {
                rememberLineCartesianLayer(
                    lineProvider = lineProvider!!,
                    pointSpacing = linePointSpacing,
                    rangeProvider = rangeProvider,
                )
        }
        ChartKind.BAR -> {
            val columns = visualSeries.mapIndexed { index, _ ->
                rememberLineComponent(
                    fill = Fill(palette[index % palette.size]),
                    thickness = 16.dp,
                )
            }
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
                columnCollectionSpacing = columnCollectionSpacing,
                mergeMode = {
                    ColumnCartesianLayer.MergeMode.Grouped(
                        columnSpacing = groupedColumnSpacing,
                    )
                },
                rangeProvider = rangeProvider,
            )
        }
    }
    val marker = if (data.interaction.selectionMode == ChampionChartSelectionMode.NEAREST_POINT) {
        rememberChampionMarker(data, theme, kind)
    } else {
        null
    }
    val markerListener = remember(data, kind, onSelectionChanged) {
        onSelectionChanged?.let {
            object : CartesianMarkerVisibilityListener {
                override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                    it(targets.toSelections(data, kind))
                }

                override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                    it(targets.toSelections(data, kind))
                }

                override fun onHidden(marker: CartesianMarker) {
                    it(emptyList())
                }
            }
        }
    }
    val startAxis = if (data.yAxis.visible) {
        VerticalAxis.rememberStart(
            line = axisLine,
            valueFormatter = yFormatter,
            guideline = axisGuideline.takeIf { data.yAxis.gridLinesVisible },
            title = { data.yAxis.title },
        )
    } else {
        null
    }
    val bottomAxis = if (data.xAxis.visible) {
        HorizontalAxis.rememberBottom(
            line = axisLine,
            valueFormatter = xFormatter,
            guideline = axisGuideline.takeIf { data.xAxis.gridLinesVisible },
            title = { data.xAxis.title },
        )
    } else {
        null
    }
    val decorations = data.referenceLines.map { reference ->
        HorizontalLine(
            y = { reference.value },
            line = rememberLineComponent(fill = Fill(theme.reference), thickness = 1.dp),
            label = { reference.label },
        )
    }
    val xStepValue = xStep(data)
    val scrollEnabled = data.interaction.scrollEnabled || data.interaction.zoomEnabled
    val scrollState = rememberVicoScrollState(scrollEnabled = scrollEnabled)
    val zoomState = rememberVicoZoomState(zoomEnabled = data.interaction.zoomEnabled)
    val markerController = if (marker != null) {
        rememberGapAwareMarkerController(
            data = data,
            kind = kind,
            renderedSeriesCount = visualSeries.size,
            xStep = xStepValue,
            zoomFactor = zoomState.value,
        )
    } else {
        CartesianMarkerController.rememberShowOnPress(consumeMoveEvents = false)
    }
    val chart = rememberCartesianChart(
        layer,
        startAxis = startAxis,
        bottomAxis = bottomAxis,
        marker = marker,
        markerVisibilityListener = markerListener,
        decorations = decorations,
        getXStep = { _, _, _ -> xStepValue },
        markerController = markerController,
    )
    CartesianChartHost(
        chart = chart,
        model = model,
        modifier = Modifier.fillMaxWidth(),
        scrollState = scrollState,
        zoomState = zoomState,
    )
}

@Composable
private fun rememberChampionMarker(
    data: ChampionCartesianChartData,
    theme: ChampionChartTheme,
    kind: ChartKind,
): CartesianMarker {
    val labelBackground = rememberShapeComponent(
        fill = Fill(theme.markerContainer),
        shape = RoundedCornerShape(8.dp),
        strokeFill = Fill(theme.marker),
        strokeThickness = 1.dp,
    )
    val label = rememberTextComponent(
        style = TextStyle(color = theme.onMarkerContainer, fontSize = 12.sp),
        padding = Insets(8.dp, 4.dp),
        background = labelBackground,
    )
    val guideline = rememberAxisGuidelineComponent(fill = Fill(theme.marker), thickness = 1.dp)
    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
            targets.toTooltip(data, kind)
        },
        indicator = { color -> ShapeComponent(Fill(color), CircleShape) },
        indicatorSize = 24.dp,
        guideline = guideline,
    )
}

/**
 * Vico stores only rendered entries in marker targets. Its controller still receives the raw
 * gesture pixel through [Interaction], which lets this adapter reject an explicit null bucket
 * before Vico displays the nearest rendered entry as if it were selected.
 */
@Composable
private fun rememberGapAwareMarkerController(
    data: ChampionCartesianChartData,
    kind: ChartKind,
    renderedSeriesCount: Int,
    xStep: Double,
    zoomFactor: Float,
): CartesianMarkerController {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val baseXSpacing = baseXSpacing(kind, renderedSeriesCount)
    val baseXSpacingPixels = with(density) { baseXSpacing.toPx() }
    return remember(
        data,
        kind,
        xStep,
        baseXSpacingPixels,
        zoomFactor,
        isLtr,
    ) {
        GapAwareMarkerController(
            data = data,
            kind = kind,
            xStep = xStep,
            pixelsPerX = baseXSpacingPixels.toDouble() * zoomFactor.effective() / xStep,
            isLtr = isLtr,
        )
    }
}

private fun Float.effective(): Double = takeIf { isFinite() && it > 0f }?.toDouble() ?: 1.0

private fun baseXSpacing(kind: ChartKind, renderedSeriesCount: Int): androidx.compose.ui.unit.Dp =
    when (kind) {
        // Vico line layer: point size (8 dp) + point spacing (32 dp).
        ChartKind.LINE -> linePointSize + linePointSpacing

        // Vico grouped columns: n * width + (n - 1) * group spacing + collection spacing.
        ChartKind.BAR ->
            columnWidth * renderedSeriesCount.coerceAtLeast(1) +
                groupedColumnSpacing * (renderedSeriesCount.coerceAtLeast(1) - 1) +
                columnCollectionSpacing
    }

private class GapAwareMarkerController(
    private val data: ChampionCartesianChartData,
    private val kind: ChartKind,
    private val xStep: Double,
    private val pixelsPerX: Double,
    private val isLtr: Boolean,
) : CartesianMarkerController {
    private var isPressed = false

    override val acceptsLongPress: Boolean = false
    override val lock: CartesianMarkerController.Lock = CartesianMarkerController.Lock.Position
    override val consumeMoveEvents: Boolean = false

    override fun shouldAcceptInteraction(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ): Boolean = when (interaction) {
        is Interaction.Press -> {
            isPressed = true
            true
        }

        is Interaction.Move -> isPressed
        is Interaction.Release -> {
            isPressed = false
            true
        }

        else -> false
    }

    override fun shouldShowMarker(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ): Boolean = when (interaction) {
        is Interaction.Press,
        is Interaction.Move,
        -> targets.isNotEmpty() && !targets.representGapAt(interaction.point.x)

        else -> false
    }

    private fun List<CartesianMarker.Target>.representGapAt(pointerX: Float): Boolean {
        if (pixelsPerX <= 0.0 || !pixelsPerX.isFinite() || !xStep.isFinite() || xStep <= 0.0) {
            return false
        }
        return isNotEmpty() && all { target ->
            val gestureX = target.x +
                (pointerX - target.canvasX).toDouble() /
                    (pixelsPerX * if (isLtr) 1 else -1)
            targetEntries(target, kind).all { (key, _, _) ->
                data.series.firstOrNull { series -> series.key == key.sourceKey }
                    ?.isGapAt(gestureX) == true
            }
        }
    }
}

private fun lineSegments(series: ChampionChartSeries): List<RenderSeries> {
    val segments = mutableListOf<RenderSeries>()
    var current = mutableListOf<ChampionChartPoint>()
    var segmentIndex = 0
    series.points.forEach { point ->
        if (point.y == null) {
            if (current.isNotEmpty()) {
                segments += RenderSeries(RenderSeriesKey(series.key, segmentIndex++), current)
                current = mutableListOf()
            }
        } else {
            current += point
        }
    }
    if (current.isNotEmpty()) {
        segments += RenderSeries(RenderSeriesKey(series.key, segmentIndex), current)
    }
    return segments
}

private fun ChampionChartSeries.isGapAt(x: Double): Boolean {
    val nearest = points.minByOrNull { point -> abs(point.x - x) } ?: return false
    return nearest.y == null
}

private fun formatAxisValue(axis: ChampionChartAxisSpec, value: Double): String {
    val formatted = axis.formatter.format(value)
    return axis.unit?.symbol?.let { "$formatted $it" } ?: formatted
}

private fun xStep(data: ChampionCartesianChartData): Double {
    val differences = data.series.flatMap { series ->
        series.points.zipWithNext().mapNotNull { (first, second) ->
            (second.x - first.x).takeIf { it > 0.0 }
        }
    }
    return differences.minOrNull() ?: 1.0
}

private fun intrinsicRangeProvider(data: ChampionCartesianChartData): CartesianLayerRangeProvider {
    val xValues = data.series.asSequence().flatMap { it.points.asSequence() }.map { it.x }.toList()
    val intrinsicMinX = xValues.minOrNull()
    val intrinsicMaxX = xValues.maxOrNull()
    return object : CartesianLayerRangeProvider {
        override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore): Double =
            safeRange(
                minimum = data.xAxis.minimum ?: intrinsicMinX ?: minX,
                maximum = data.xAxis.maximum ?: intrinsicMaxX ?: maxX,
            ).minimum

        override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore): Double =
            safeRange(
                minimum = data.xAxis.minimum ?: intrinsicMinX ?: minX,
                maximum = data.xAxis.maximum ?: intrinsicMaxX ?: maxX,
            ).maximum

        override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
            safeRange(
                minimum = data.yAxis.minimum ?: minY,
                maximum = data.yAxis.maximum ?: maxY,
            ).minimum

        override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
            safeRange(
                minimum = data.yAxis.minimum ?: minY,
                maximum = data.yAxis.maximum ?: maxY,
            ).maximum
    }
}

private data class SafeRange(
    val minimum: Double,
    val maximum: Double,
)

/**
 * Keeps explicit or intrinsic bounds intact when they are usable. A singleton
 * domain gets padding only in its bounds; point values remain unchanged. This
 * also supplies a finite 0..1 fallback when Vico has no usable intrinsic range.
 */
private fun safeRange(minimum: Double?, maximum: Double?): SafeRange {
    val lower = minimum?.takeIf { it.isFinite() } ?: 0.0
    val upper = maximum?.takeIf { it.isFinite() } ?: 1.0
    if (lower < upper) return SafeRange(lower, upper)

    val center = minimum?.takeIf { it.isFinite() }
        ?: maximum?.takeIf { it.isFinite() }
        ?: 0.0
    val paddedMinimum = center - 1.0
    val paddedMaximum = center + 1.0
    val candidateMinimum =
        if (paddedMinimum.isFinite() && paddedMinimum < center) paddedMinimum
        else Math.nextDown(center)
    val candidateMaximum =
        if (paddedMaximum.isFinite() && center < paddedMaximum) paddedMaximum
        else Math.nextUp(center)

    if (candidateMinimum.isFinite() && candidateMaximum.isFinite() && candidateMinimum < candidateMaximum) {
        return SafeRange(candidateMinimum, candidateMaximum)
    }

    val previous = Math.nextDown(center)
    if (previous.isFinite() && previous < center) return SafeRange(previous, center)

    val next = Math.nextUp(center)
    if (next.isFinite() && center < next) return SafeRange(center, next)

    return SafeRange(0.0, 1.0)
}

private fun buildAccessibleSummary(data: ChampionCartesianChartData): String = buildString {
    append(data.accessibilitySummary)
    if (data.gapCount > 0) append(". ${data.gapCount} lacunas sem dados")
    data.series
        .asSequence()
        .flatMap { it.points.asSequence() }
        .mapNotNull { it.contentDescription }
        .distinct()
        .toList()
        .takeIf { it.isNotEmpty() }
        ?.let { descriptions -> append(". ${descriptions.joinToString(". ")}") }
    data.referenceLines
        .asSequence()
        .map { it.contentDescription }
        .distinct()
        .toList()
        .takeIf { it.isNotEmpty() }
        ?.let { descriptions ->
            append(". Linhas de referência: ${descriptions.joinToString(". ")}")
        }
    if (!data.hasRecordedValues) append(". Sem dados para exibir")
}

private fun List<CartesianMarker.Target>.toSelections(
    data: ChampionCartesianChartData,
    kind: ChartKind,
): List<ChampionChartSelection> = flatMap { target ->
    targetEntries(target, kind).mapNotNull { (key, x, y) ->
        // Vico exposes selected rendered entries, not raw gesture coordinates.
        // Segmented models omit null points, so accept only an exact recorded
        // point. An unmappable target is dropped rather than treated as zero,
        // interpolated, or presented as a synthetic gap value.
        data.series.firstOrNull { it.key == key.sourceKey }
            ?.points?.firstOrNull { point -> point.x == x && point.y == y }
            ?.let { ChampionChartSelection(key.sourceKey, it) }
    }
}.distinctBy { it.seriesKey to it.point.x }

private fun List<CartesianMarker.Target>.toTooltip(
    data: ChampionCartesianChartData,
    kind: ChartKind,
): CharSequence = toSelections(data, kind).mapNotNull { selection ->
    val point = selection.point
    val yValue = point.y ?: return@mapNotNull null
    val x = point.xLabel ?: data.xAxis.formatter.format(point.x)
    val y = formatAxisValue(data.yAxis, yValue)
    "${data.series.first { it.key == selection.seriesKey }.label}: $x · $y"
}.joinToString("\n")

private fun targetEntries(
    target: CartesianMarker.Target,
    kind: ChartKind,
): List<Triple<RenderSeriesKey, Double, Double>> = when (kind) {
    ChartKind.LINE -> (target as? LineCartesianLayerMarkerTarget)?.points.orEmpty().map {
        Triple(it.entry.seriesKey as? RenderSeriesKey ?: return@map null, it.entry.x, it.entry.y)
    }.filterNotNull()
    ChartKind.BAR -> (target as? ColumnCartesianLayerMarkerTarget)?.columns.orEmpty().map {
        Triple(it.entry.seriesKey as? RenderSeriesKey ?: return@map null, it.entry.x, it.entry.y)
    }.filterNotNull()
}
