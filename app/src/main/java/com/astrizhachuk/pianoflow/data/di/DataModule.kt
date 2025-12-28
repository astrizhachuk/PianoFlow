package com.astrizhachuk.pianoflow.data.di

import android.content.Context
import android.media.midi.MidiManager
import com.astrizhachuk.pianoflow.data.datasource.midi.MidiDataSource
import com.astrizhachuk.pianoflow.data.repository.MidiRepositoryImpl
import com.astrizhachuk.pianoflow.domain.repository.MidiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI модуль для Data слоя.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideMidiManager(@ApplicationContext context: Context): MidiManager? {
        return context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    }
    
    @Provides
    @Singleton
    fun provideMidiDataSource(midiManager: MidiManager?): MidiDataSource {
        return MidiDataSource(midiManager)
    }
    
    @Provides
    @Singleton
    fun provideMidiRepository(
        midiRepositoryImpl: MidiRepositoryImpl
    ): MidiRepository {
        return midiRepositoryImpl
    }
}

