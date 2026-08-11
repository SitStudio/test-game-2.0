package com.jolttime.game

import com.jolttime.game.data.local.GameStateCodec
import com.jolttime.game.domain.model.GameContent
import com.jolttime.game.domain.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateCodecTest {
    @Test
    fun saveAndRestorePreservesCompleteProgress() {
        val expected = GameState(
            level = 12,
            xp = 345,
            timeShards = 9_876,
            coins = 543,
            totalTaps = 1_234,
            upgrades = GameContent.upgrades.map { it.copy(level = 2) },
            artifacts = GameContent.artifacts.mapIndexed { index, artifact ->
                if (index == 0) artifact.copy(fragmentsOwned = artifact.fragmentsRequired, isCompleted = true)
                else artifact
            },
            dailyDay = 4,
            lastDailyClaim = "2026-01-10",
            soundEnabled = true,
            vibrationEnabled = false,
            lastSeenAt = 1_000
        )

        val restored = GameStateCodec.decodeOrDefault(GameStateCodec.encode(expected), now = 2_000)

        assertEquals(expected, restored)
        assertEquals(expected.unlockedEpochs, restored.unlockedEpochs)
        assertEquals(expected.expedition, restored.expedition)
    }

    @Test
    fun corruptJsonFallsBackToSafeNewGame() {
        val restored = GameStateCodec.decodeOrDefault("{not valid json", now = 42_000)

        assertEquals(1, restored.level)
        assertEquals(100L, restored.coins)
        assertEquals(42_000L, restored.lastSeenAt)
    }

    @Test
    fun impossiblePersistedValuesAreSanitized() {
        val invalid = GameState(
            level = -2,
            xp = -1,
            timeShards = -10,
            coins = -20,
            artifacts = GameContent.artifacts.map { it.copy(fragmentsOwned = -4) },
            lastSeenAt = Long.MAX_VALUE
        )

        val restored = GameStateCodec.decodeOrDefault(GameStateCodec.encode(invalid), now = 50_000)

        assertEquals(1, restored.level)
        assertEquals(0L, restored.xp)
        assertEquals(0L, restored.timeShards)
        assertEquals(0L, restored.coins)
        assertTrue(restored.artifacts.all { it.fragmentsOwned >= 0 })
        assertFalse(restored.artifacts.any { it.fragmentsRequired <= 0 })
        assertTrue(restored.lastSeenAt <= 50_000)
    }
}
