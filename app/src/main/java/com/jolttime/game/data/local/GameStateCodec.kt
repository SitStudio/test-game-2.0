package com.jolttime.game.data.local

import com.jolttime.game.domain.model.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

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
            migrateLegacy(raw, now)
        }
    }

    /** Recovers reusable account values even when the old clicker artifact schema is incompatible. */
    private fun migrateLegacy(raw: String, now: Long): GameState = runCatching {
        val old = json.parseToJsonElement(raw).jsonObject
        GameState(
            level = old["level"]?.jsonPrimitive?.longOrNull?.toInt() ?: 1,
            xp = old["xp"]?.jsonPrimitive?.longOrNull ?: 0,
            timeShards = old["timeShards"]?.jsonPrimitive?.longOrNull ?: 0,
            coins = old["coins"]?.jsonPrimitive?.longOrNull ?: 100,
            totalTaps = old["totalTaps"]?.jsonPrimitive?.longOrNull ?: 0,
            lastSeenAt = old["lastSeenAt"]?.jsonPrimitive?.longOrNull ?: now,
        ).sanitized(now)
    }.getOrElse { GameState(lastSeenAt = now) }
}
