package br.com.mwconsulting.championcompanion.feature.device

/** Synthetic protocol outcomes. Contains no device capture or health measurement. */
object DevicePocSanitizedFixtures {
    val a1 = DevicePocFixture("A1", observed = true, count = 1)
    val fea1 = DevicePocFixture("FEA1", observed = true, count = 2)
    val d0 = DevicePocFixture("D0", observed = true, count = 4)
    val f002 = DevicePocFixture("F002", observed = true, count = 12)
}

data class DevicePocFixture(val protocol: String, val observed: Boolean, val count: Int)
