package br.com.mwconsulting.championcompanion.core.bluetooth.android

import java.time.Instant

data class AndroidScanResultView(
    val transportKey: String,
    val name: String?,
    val localName: String?,
    val observedAt: Instant,
)
