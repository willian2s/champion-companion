package br.com.mwconsulting.championcompanion.feature.gpspoc

/**
 * Exceção local da PoC para inspeção visual com localização mock.
 *
 * Este objeto existe somente na variante debug. Não altera contratos produtivos,
 * nem substitui permissão, localização habilitada ou validação de GPS real.
 */
internal object GpsMapPocMockGeometryDebugMode {
    const val isEnabled: Boolean = true
    const val acceptsMockLocations: Boolean = isEnabled
    const val banner: String = "Modo geometria debug: mock aceito"
    const val disclaimer: String =
        "Somente inspeção de geometria, câmera, stop e reset; não comprova GPS real."
    const val requirements: String =
        "Permissão precisa e localização habilitada continuam necessárias."
}
