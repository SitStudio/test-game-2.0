package com.jolttime.game.data.local

import com.jolttime.game.domain.model.GameState
import kotlinx.serialization.json.Json

/** A small, JVM-testable boundary around the on-device save format. */
object GameStateCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: GameState): String = json.encodeToString(GameState.serializer(), state.sanitized())

    fun decodeOrDefault(raw: String?, now: Long = System.currentTimeMillis()): GameState {
        if (raw.isNullOrBlank()) return GameState(lastSeenAt = now)
        return try {
            json.decodeFromString(GameState.serializer(), raw).sanitized(now)
        } catch (_: Exception) {
            GameState(lastSeenAt = now)
        }
    }
}
