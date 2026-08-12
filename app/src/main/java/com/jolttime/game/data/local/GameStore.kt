package com.jolttime.game.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.jolttime.game.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "jolt_time_save",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class GameStore(private val context: Context) {
    private val key = stringPreferencesKey("game_state")
    val state: Flow<GameState> = context.dataStore.data.map { preferences ->
        GameStateCodec.decodeOrDefault(preferences[key])
    }
    suspend fun save(state: GameState) = context.dataStore.edit { it[key] = GameStateCodec.encode(state) }
    suspend fun reset() = context.dataStore.edit { it.remove(key) }
}
