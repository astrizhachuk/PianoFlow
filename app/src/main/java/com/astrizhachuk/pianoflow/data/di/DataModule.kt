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
 * Hilt module responsible for providing dependencies related to the data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Binds the [MidiRepository] interface, defined in the domain layer,
     * to its concrete implementation [MidiRepositoryImpl] from the data layer.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiRepository(impl: MidiRepositoryImpl): MidiRepository

    /**
     * Binds the [MidiDeviceMapper] interface, defined in the domain layer,
     * to its concrete implementation [MidiDeviceMapperImpl] from the data layer.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiDeviceMapper(impl: MidiDeviceMapperImpl): MidiDeviceMapper

    companion object {
        /**
         * Provides [MidiDataSource] as a singleton. This data source is the
         * main point of interaction with the Android MIDI API.
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
         * Provides [MidiMessageParser] as a singleton.
         */
        @Provides
        @Singleton
        fun provideMidiMessageParser(): MidiMessageParser {
            return MidiMessageParser()
        }
    }
}
