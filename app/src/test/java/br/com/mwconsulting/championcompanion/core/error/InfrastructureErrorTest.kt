package br.com.mwconsulting.championcompanion.core.error

import org.junit.Assert.assertEquals
import org.junit.Test

class InfrastructureErrorTest {
    @Test
    fun `network error maps to safe message`() {
        assertEquals(
            "Não foi possível conectar. Tente novamente.",
            InfrastructureError.NetworkUnavailable.toUiError().message,
        )
    }

    @Test
    fun `service error maps to safe message`() {
        assertEquals(
            "Serviço indisponível no momento. Tente novamente.",
            InfrastructureError.ServiceUnavailable.toUiError().message,
        )
    }

    @Test
    fun `unexpected error maps to safe message`() {
        assertEquals(
            "Não foi possível concluir esta ação. Tente novamente.",
            InfrastructureError.Unexpected.toUiError().message,
        )
    }
}
