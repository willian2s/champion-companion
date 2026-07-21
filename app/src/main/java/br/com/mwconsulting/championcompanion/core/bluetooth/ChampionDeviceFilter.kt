package br.com.mwconsulting.championcompanion.core.bluetooth

object ChampionDeviceFilter {
    private const val DEFAULT_PREFIX = "champion"

    fun matches(device: ChampionDiscoveredDevice, prefix: String = DEFAULT_PREFIX): Boolean =
        matches(name = device.name, localName = device.localName, prefix = prefix)

    fun matches(name: String?, localName: String?, prefix: String = DEFAULT_PREFIX): Boolean {
        val normalizedPrefix = prefix.normalizedOrNull() ?: return false
        return name.normalizedOrNull()?.startsWith(normalizedPrefix) == true ||
            localName.normalizedOrNull()?.startsWith(normalizedPrefix) == true
    }

    fun deduplicationKey(device: ChampionDiscoveredDevice): String = device.transportKey.trim()

    fun deduplicate(devices: Iterable<ChampionDiscoveredDevice>): List<ChampionDiscoveredDevice> {
        val unique = linkedMapOf<String, ChampionDiscoveredDevice>()
        devices.forEach { device -> unique.putIfAbsent(deduplicationKey(device), device) }
        return unique.values.toList()
    }

    private fun String?.normalizedOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
}
