package br.com.mwconsulting.championcompanion.feature.exercises

import android.content.Context
import android.content.Intent
import br.com.mwconsulting.championcompanion.feature.gpspoc.GpsMapPocActivity

internal object DebugPocLauncher {
    fun createCallback(context: Context): (() -> Unit)? = {
        context.startActivity(Intent(context, GpsMapPocActivity::class.java))
    }
}
