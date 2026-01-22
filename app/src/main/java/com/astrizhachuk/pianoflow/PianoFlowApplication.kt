package com.astrizhachuk.pianoflow

import android.app.Application
import androidx.tracing.trace
import com.astrizhachuk.pianoflow.log.CrashReportingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.astrizhachuk.pianoflow.BuildConfig

/**
 * Application класс для инициализации Hilt.
 */
@HiltAndroidApp
class PianoFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        trace("PianoFlowApplication.onCreate") {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            } else {
                Timber.plant(CrashReportingTree())
            }
        }
    }
}
