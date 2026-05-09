package com.astrizhachuk.pianoflow.data.di

import android.content.Context
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiMessageParser
import com.astrizhachuk.pianoflow.data.mapper.midi.MidiDeviceMapperImpl
import com.astrizhachuk.pianoflow.data.repository.ChordAnalysisRepositoryImpl
import com.astrizhachuk.pianoflow.data.repository.MidiRepositoryImpl
import com.astrizhachuk.pianoflow.domain.mapper.midi.MidiDeviceMapper
import com.astrizhachuk.pianoflow.domain.repository.ChordAnalysisRepository
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Suppress("unused")
    abstract fun bindMidiDeviceMapper(impl: MidiDeviceMapperImpl): MidiDeviceMapper

    @Binds
    @Suppress("unused")
    abstract fun bindMidiRepository(impl: MidiRepositoryImpl): MidiRepository

    @Binds
    @Singleton
    @Suppress("unused")
    abstract fun bindChordAnalysisRepository(impl: ChordAnalysisRepositoryImpl): ChordAnalysisRepository

    companion object {

        @Provides
        @Singleton
        fun provideMidiDataSource(
            @ApplicationContext context: Context,
            midiDeviceMapper: MidiDeviceMapper,
            midiMessageParser: MidiMessageParser
        ): MidiDataSource {
            return MidiDataSource(context, midiDeviceMapper, midiMessageParser)
        }

        @Provides
        @Singleton
        fun provideMidiMessageParser(): MidiMessageParser {
            return MidiMessageParser()
        }
    }
}
