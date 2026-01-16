package com.astrizhachuk.pianoflow.domain.repository

import com.astrizhachuk.pianoflow.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

interface MidiRepository {
    fun observeConnectionState(): Flow<ConnectionState>
}

