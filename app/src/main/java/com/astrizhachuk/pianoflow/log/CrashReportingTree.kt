package com.astrizhachuk.pianoflow.log

import android.util.Log
import timber.log.Timber

/**
 * Дерево Timber для отправки отчетов о сбоях и логирования ошибок в release-сборках.
 * В реальном приложении это должно быть интегрировано с сервисом для сбора отчетов о сбоях,
 * например, с Firebase Crashlytics.
 */
class CrashReportingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            // В реальном приложении здесь будет логика отправки в сервис для сбора отчетов о сбоях.
            // Например, с Crashlytics:
            // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
        }
    }
}
