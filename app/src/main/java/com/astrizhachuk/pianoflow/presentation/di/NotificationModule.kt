package com.astrizhachuk.pianoflow.presentation.di

import com.astrizhachuk.pianoflow.presentation.service.UserNotifier
import com.astrizhachuk.pianoflow.presentation.service.UserNotifierImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-модуль, отвечающий за предоставление зависимостей, связанных с уведомлениями.
 *
 * Этот модуль использует [@Binds], чтобы сообщить Hilt, какую конкретную реализацию
 * следует использовать, когда запрашивается интерфейс [UserNotifier].
 */
@Module
@InstallIn(SingletonComponent::class)
interface NotificationModule {

    /**
     * Связывает интерфейс [UserNotifier] с его конкретной реализацией [UserNotifierImpl].
     *
     * Эта привязка гарантирует, что Hilt будет предоставлять один и тот же экземпляр
     * [UserNotifierImpl] каждый раз, когда в коде запрашивается зависимость типа [UserNotifier]
     * в пределах жизненного цикла приложения.
     *
     * @param impl Конкретная реализация, которую Hilt должен предоставить.
     * @return Абстракция (интерфейс), которая будет использоваться в приложении.
     */
    @Binds
    fun bindUserNotifier(impl: UserNotifierImpl): UserNotifier
}
