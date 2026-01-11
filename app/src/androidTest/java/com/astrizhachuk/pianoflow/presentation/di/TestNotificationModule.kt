package com.astrizhachuk.pianoflow.presentation.di

import com.astrizhachuk.pianoflow.presentation.service.FakeUserNotifier
import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NotificationModule::class]
)
object TestNotificationModule {

    @Provides
    @Singleton
    fun provideUserNotifier(): UserNotifier {
        return FakeUserNotifier()
    }
}
