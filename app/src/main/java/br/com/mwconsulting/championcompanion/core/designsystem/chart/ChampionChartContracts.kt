package br.com.mwconsulting.championcompanion.core.designsystem.chart

import androidx.compose.runtime.Immutable

/**
 * Ponto neutro de apresentação para gráficos cartesianos.
 *
 * [x] preserva a ordem real do eixo sem impor unidade ao consumidor. [y] nulo
 * representa ausência de dado e nunca deve ser reinterpretado como zero.
 */
@Immutable
data class ChampionChartPoint(
    val x: Double,
    val y: Double?,
    val xLabel: String? = null,
    val contentDescription: String? = null,
) {
    init {
        require(x.isFinite())
        require(y?.isFinite() != false)
        require(xLabel?.isNotBlank() != false)
        require(contentDescription?.isNotBlank() != false)
    }
}

/** Série ordenada que uma feature entrega à infraestrutura de gráficos. */
@Immutable
data class ChampionChartSeries(
    val key: String,
    val label: String,
    val points: List<ChampionChartPoint>,
) {
    init {
        require(key.isNotBlank())
        require(label.isNotBlank())
        require(points.zipWithNext().all { (previous, next) -> previous.x < next.x })
    }
}

/** Formatter independente do renderer para eixos, marker e tooltip. */
fun interface ChampionChartValueFormatter {
    fun format(value: Double): String
}

/** Configuração semântica comum de um eixo cartesiano. */
@Immutable
data class ChampionChartAxisSpec(
    val title: String? = null,
    val formatter: ChampionChartValueFormatter,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val visible: Boolean = true,
    val gridLinesVisible: Boolean = true,
) {
    init {
        require(title?.isNotBlank() != false)
        require(minimum?.isFinite() != false)
        require(maximum?.isFinite() != false)
        require(minimum == null || maximum == null || minimum < maximum)
    }
}

/** Linha de referência opcional, como meta de passos ou guideline de BPM. */
@Immutable
data class ChampionChartReferenceLine(
    val value: Double,
    val label: String,
    val contentDescription: String = label,
) {
    init {
        require(value.isFinite())
        require(label.isNotBlank())
        require(contentDescription.isNotBlank())
    }
}

/** Política de seleção visual; o renderer continuará responsável por hit-testing. */
enum class ChampionChartSelectionMode {
    NONE,
    NEAREST_POINT,
}

/**
 * Capacidades interativas do gráfico.
 *
 * Scroll e zoom permanecem desligados por padrão para não competir com a
 * rolagem vertical das telas.
 */
@Immutable
data class ChampionChartInteractionPolicy(
    val selectionMode: ChampionChartSelectionMode = ChampionChartSelectionMode.NEAREST_POINT,
    val scrollEnabled: Boolean = false,
    val zoomEnabled: Boolean = false,
)

/** Estado neutro completo consumido por line/column charts compartilhados. */
@Immutable
data class ChampionCartesianChartData(
    val series: List<ChampionChartSeries>,
    val xAxis: ChampionChartAxisSpec,
    val yAxis: ChampionChartAxisSpec,
    val referenceLines: List<ChampionChartReferenceLine> = emptyList(),
    val interaction: ChampionChartInteractionPolicy = ChampionChartInteractionPolicy(),
    val accessibilitySummary: String,
) {
    init {
        require(series.isNotEmpty())
        require(series.map(ChampionChartSeries::key).distinct().size == series.size)
        require(accessibilitySummary.isNotBlank())
    }

    val hasRecordedValues: Boolean
        get() = series.any { item -> item.points.any { point -> point.y != null } }

    val gapCount: Int
        get() = series.sumOf { item -> item.points.count { point -> point.y == null } }
}
