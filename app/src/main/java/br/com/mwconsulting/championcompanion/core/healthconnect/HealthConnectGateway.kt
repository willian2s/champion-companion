package br.com.mwconsulting.championcompanion.core.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectWritePermission(
    val rawValue: String,
) {
    STEPS("android.permission.health.WRITE_STEPS"),
    HEART_RATE("android.permission.health.WRITE_HEART_RATE"),
    EXERCISE("android.permission.health.WRITE_EXERCISE"),
    DISTANCE("android.permission.health.WRITE_DISTANCE"),
    TOTAL_CALORIES_BURNED("android.permission.health.WRITE_TOTAL_CALORIES_BURNED"),
    WEIGHT("android.permission.health.WRITE_WEIGHT"),
    HEIGHT("android.permission.health.WRITE_HEIGHT"),
    ;

    companion object {
        fun fromRaw(value: String): HealthConnectWritePermission? = entries.firstOrNull { it.rawValue == value }
    }
}

interface HealthConnectGateway {
    fun getAvailability(): HealthConnectAvailability

    suspend fun getGrantedPermissions(): Set<HealthConnectWritePermission>

    suspend fun insert(records: List<Record>)
}

@Singleton
class AndroidHealthConnectGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val platform: HealthConnectPlatform,
) : HealthConnectGateway {
    override fun getAvailability(): HealthConnectAvailability = platform.getAvailability()

    override suspend fun getGrantedPermissions(): Set<HealthConnectWritePermission> =
        client().permissionController.getGrantedPermissions()
            .mapNotNull(HealthConnectWritePermission::fromRaw)
            .toSet()

    override suspend fun insert(records: List<Record>) {
        if (records.isEmpty()) return
        client().insertRecords(records)
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context, platform.providerPackageName)
}
