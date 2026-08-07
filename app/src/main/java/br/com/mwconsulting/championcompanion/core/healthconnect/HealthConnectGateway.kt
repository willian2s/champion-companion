package br.com.mwconsulting.championcompanion.core.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
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
    SPEED("android.permission.health.WRITE_SPEED"),
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

    /** Remove somente record próprio identificado por clientRecordId exato. */
    suspend fun delete(recordType: HealthSyncRecordType, clientRecordId: String)
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

    override suspend fun delete(recordType: HealthSyncRecordType, clientRecordId: String) {
        require(clientRecordId.isNotBlank())
        client().deleteRecords(
            recordType = recordType.toHealthConnectRecordClass(),
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(clientRecordId),
        )
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context, platform.providerPackageName)
}

private fun HealthSyncRecordType.toHealthConnectRecordClass() = when (this) {
    HealthSyncRecordType.STEPS -> StepsRecord::class
    HealthSyncRecordType.HEART_RATE -> HeartRateRecord::class
    HealthSyncRecordType.EXERCISE_SESSION -> ExerciseSessionRecord::class
    HealthSyncRecordType.DISTANCE -> DistanceRecord::class
    HealthSyncRecordType.TOTAL_CALORIES_BURNED -> TotalCaloriesBurnedRecord::class
    HealthSyncRecordType.WEIGHT -> WeightRecord::class
    HealthSyncRecordType.HEIGHT -> HeightRecord::class
    HealthSyncRecordType.SPEED -> SpeedRecord::class
}
