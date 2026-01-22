package com.astrizhachuk.pianoflow.log

import android.util.Log
import timber.log.Timber

/**
 * A Timber tree for sending crash reports and logging errors in release builds.
 * In a real application, this should be integrated with a crash reporting service,
 * for example, Firebase Crashlytics.
 */
class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            // In a real application, there would be logic here for sending to a crash reporting service.
            // For example, with Crashlytics:
            // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
        }
    }
}
