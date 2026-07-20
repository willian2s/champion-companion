package br.com.mwconsulting.championcompanion.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthChartScaleTest {
    @Test
    fun `chart scale maps supplied minimum and maximum to chart bounds`() {
        val scale = normalizedChartY(
            listOf(
                HealthChartPoint(10f, "primeiro"),
                HealthChartPoint(20f, "segundo"),
                HealthChartPoint(15f, "terceiro"),
            ),
        )

        assertEquals(listOf(1f, 0f, 0.5f), scale)
    }

    @Test
    fun `chart scale centers equal supplied values`() {
        val scale = normalizedChartY(
            listOf(
                HealthChartPoint(10f, "primeiro"),
                HealthChartPoint(10f, "segundo"),
            ),
        )

        assertEquals(listOf(0.5f, 0.5f), scale)
    }

    @Test
    fun `chart scale keeps empty input empty`() {
        assertEquals(emptyList<Float>(), normalizedChartY(emptyList()))
    }
}
