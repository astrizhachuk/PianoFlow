package com.astrizhachuk.pianoflow

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Runner для запуска инструментальных тестов с Hilt.
 * Он подменяет стандартный класс Application на HiltTestApplication,
 * который необходим для корректной работы Hilt в тестах.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
