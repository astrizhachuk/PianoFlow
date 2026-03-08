package com.astrizhachuk.pianoflow.data.di

import android.content.Context
import android.webkit.WebView
import com.astrizhachuk.pianoflow.data.datasource.analysis.MusicScriptEngine
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

/**
 * Hilt module responsible for providing dependencies related to the data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    /**
     * Binds the [MidiDeviceMapper] interface, defined in the domain layer,
     * to its concrete implementation [MidiDeviceMapperImpl] from the data layer.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiDeviceMapper(impl: MidiDeviceMapperImpl): MidiDeviceMapper

    /**
     * Binds the [MidiRepository] interface, defined in the domain layer,
     * to its concrete implementation [MidiRepositoryImpl] from the data layer.
     */
    @Binds
    @Suppress("unused")
    abstract fun bindMidiRepository(impl: MidiRepositoryImpl): MidiRepository

    /**
     * Binds the [ChordAnalysisRepository] interface, defined in the domain layer,
     * to its concrete implementation [ChordAnalysisRepositoryImpl] from the data layer.
     */
    @Binds
    @Singleton
    @Suppress("unused")
    abstract fun bindChordAnalysisRepository(impl: ChordAnalysisRepositoryImpl): ChordAnalysisRepository

    companion object {
        /**
         * Provides a WebView for executing JavaScript in the Data layer.
         *
         * This WebView is used only for chord analysis, not for rendering.
         * It's separate from the UI WebView in PianoStaff to keep layers independent.
         */
        @Provides
        @Singleton
        fun provideWebView(@ApplicationContext context: Context): WebView {
            return WebView(context).apply {
                settings.javaScriptEnabled = true
            }
        }

        /**
         * Provides [MusicScriptEngine] for executing JavaScript code.
         *
         * This allows Repository to analyze chords without any UI involvement.
         */
        @Provides
        @Singleton
        fun provideMusicScriptEngine(webView: WebView): MusicScriptEngine {
            return MusicScriptEngine(
                webView = webView,
                pageUrl = "file:///android_asset/tonal-analysis.html"
            )
        }

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
