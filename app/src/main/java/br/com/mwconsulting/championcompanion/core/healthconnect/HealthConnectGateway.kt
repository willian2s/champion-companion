package br.com.mwconsulting.championcompanion.core.healthconnect

import android.content.Context
import android.os.RemoteException
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
import kotlinx.coroutines.CancellationException

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

enum class HealthConnectFailureKind {
    PERMISSION_REQUIRED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    WRITE_REJECTED,
    INVALID_LOCAL_DATA,
    INTERNAL,
}

data class HealthConnectGatewayFailure(
    val kind: HealthConnectFailureKind,
    val reason: String,
)

sealed interface HealthConnectPermissionResult {
    data class Granted(
        val permissions: Set<HealthConnectWritePermission>,
    ) : HealthConnectPermissionResult

    data class Failed(
        val failure: HealthConnectGatewayFailure,
    ) : HealthConnectPermissionResult
}

sealed interface HealthConnectWriteResult {
    data object Accepted : HealthConnectWriteResult

    data class Failed(
        val failure: HealthConnectGatewayFailure,
    ) : HealthConnectWriteResult
}

interface HealthConnectGateway {
    fun getAvailability(): HealthConnectAvailability

    suspend fun getGrantedPermissions(): HealthConnectPermissionResult

    suspend fun insert(records: List<Record>): HealthConnectWriteResult

    /** Remove somente record próprio identificado por clientRecordId exato. */
    suspend fun delete(recordType: HealthSyncRecordType, clientRecordId: String): HealthConnectWriteResult
}

@Singleton
class AndroidHealthConnectGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val platform: HealthConnectPlatform,
) : HealthConnectGateway {
    override fun getAvailability(): HealthConnectAvailability = platform.getAvailability()

    override suspend fun getGrantedPermissions(): HealthConnectPermissionResult = try {
        HealthConnectPermissionResult.Granted(
            client().permissionController.getGrantedPermissions()
                .mapNotNull(HealthConnectWritePermission::fromRaw)
                .toSet(),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        HealthConnectPermissionResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PROVIDER_UNAVAILABLE,
                reason = "Não foi possível consultar as permissões atuais do provider Health Connect.",
            ),
        )
    }

    override suspend fun insert(records: List<Record>): HealthConnectWriteResult = try {
        if (records.isNotEmpty()) client().insertRecords(records)
        HealthConnectWriteResult.Accepted
    } catch (error: CancellationException) {
        throw error
    } catch (_: SecurityException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PERMISSION_REQUIRED,
                reason = "O provider Health Connect rejeitou a escrita por falta de permissão.",
            ),
        )
    } catch (_: RemoteException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PROVIDER_UNAVAILABLE,
                reason = "O provider Health Connect não respondeu à escrita.",
            ),
        )
    } catch (_: IllegalStateException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PROVIDER_UNAVAILABLE,
                reason = "O provider Health Connect não está disponível para esta escrita.",
            ),
        )
    } catch (_: IllegalArgumentException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.WRITE_REJECTED,
                reason = "O provider Health Connect rejeitou o tipo de record ou sua capacidade atual.",
            ),
        )
    } catch (_: Exception) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.WRITE_REJECTED,
                reason = "O provider Health Connect rejeitou a escrita.",
            ),
        )
    }

    override suspend fun delete(
        recordType: HealthSyncRecordType,
        clientRecordId: String,
    ): HealthConnectWriteResult = try {
        require(clientRecordId.isNotBlank())
        client().deleteRecords(
            recordType = recordType.toHealthConnectRecordClass(),
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(clientRecordId),
        )
        HealthConnectWriteResult.Accepted
    } catch (error: CancellationException) {
        throw error
    } catch (_: SecurityException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PERMISSION_REQUIRED,
                reason = "O provider Health Connect rejeitou a remoção por falta de permissão.",
            ),
        )
    } catch (_: RemoteException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PROVIDER_UNAVAILABLE,
                reason = "O provider Health Connect não respondeu à remoção.",
            ),
        )
    } catch (_: IllegalStateException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.PROVIDER_UNAVAILABLE,
                reason = "O provider Health Connect não está disponível para esta remoção.",
            ),
        )
    } catch (_: IllegalArgumentException) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.WRITE_REJECTED,
                reason = "O provider Health Connect rejeitou a remoção.",
            ),
        )
    } catch (_: Exception) {
        HealthConnectWriteResult.Failed(
            HealthConnectGatewayFailure(
                kind = HealthConnectFailureKind.WRITE_REJECTED,
                reason = "O provider Health Connect rejeitou a remoção.",
            ),
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
