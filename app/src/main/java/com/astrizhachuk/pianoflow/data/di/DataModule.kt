package com.astrizhachuk.pianoflow.data.di

import android.content.Context
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiMessageParser
import com.astrizhachuk.pianoflow.data.mapper.midi.MidiDeviceMapperImpl
import com.astrizhachuk.pianoflow.data.repository.MidiRepositoryImpl
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль, отвечающий за предоставление зависимостей, связанных со слоем данных.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Связывает интерфейс [MidiRepository], определенный в доменном слое,
     * с его конкретной реализацией [MidiRepositoryImpl] из слоя данных.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiRepository(impl: MidiRepositoryImpl): MidiRepository

    /**
     * Связывает интерфейс [MidiDeviceMapper], определенный в доменном слое,
     * с его конкретной реализацией [MidiDeviceMapperImpl] из слоя данных.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiDeviceMapper(impl: MidiDeviceMapperImpl): MidiDeviceMapper

    companion object {
        /**
         * Предоставляет [MidiDataSource] как синглтон. Этот источник данных является
         * основной точкой взаимодействия с Android MIDI API.
         */
        @Provides
        @Singleton
        fun provideMidiDataSource(
            @ApplicationContext context: Context,
            midiDeviceMapper: MidiDeviceMapper,
            midiMessageParser: MidiMessageParser
        ): MidiDataSource {
            return MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        }

        /**
         * Предоставляет [MidiMessageParser] как синглтон.
         */
        @Provides
        @Singleton
        fun provideMidiMessageParser(): MidiMessageParser {
            return MidiMessageParser()
        }
    }
}
