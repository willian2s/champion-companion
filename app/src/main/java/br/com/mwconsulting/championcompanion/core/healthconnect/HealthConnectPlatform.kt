package br.com.mwconsulting.championcompanion.core.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val HEALTH_CONNECT_PROVIDER_PACKAGE_NAME: String = "com.google.android.apps.healthdata"

sealed interface HealthConnectAvailability {
    data object Available : HealthConnectAvailability

    data object ProviderUpdateRequired : HealthConnectAvailability

    data object Unavailable : HealthConnectAvailability
}

interface HealthConnectPlatform {
    val providerPackageName: String

    fun getAvailability(): HealthConnectAvailability

    fun createProviderUpdateIntent(): Intent
}

@Singleton
class AndroidHealthConnectPlatform @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HealthConnectPlatform {
    override val providerPackageName: String = HEALTH_CONNECT_PROVIDER_PACKAGE_NAME

    override fun getAvailability(): HealthConnectAvailability = when (
        HealthConnectClient.getSdkStatus(context, providerPackageName)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
            HealthConnectAvailability.ProviderUpdateRequired
        }

        else -> HealthConnectAvailability.Unavailable
    }

    override fun createProviderUpdateIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$providerPackageName"))
            .setPackage("com.android.vending")
}
