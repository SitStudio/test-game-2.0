package com.jolttime.game

import com.jolttime.game.domain.model.ExpeditionStatus
import com.jolttime.game.domain.model.GameContent
import com.jolttime.game.domain.model.GameState
import com.jolttime.game.game.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameEngineTest {
    private fun stateWithPassiveIncome(lastSeenAt: Long = 0): GameState = GameState(
        upgrades = GameContent.upgrades.map { if (it.id == "engine") it.copy(level = 1) else it },
        lastSeenAt = lastSeenAt
    )

    @Test
    fun tapAwardsAllCurrencies() {
        val after = GameEngine.tap(GameState()).first

        assertEquals(1L, after.timeShards)
        assertEquals(101L, after.coins)
        assertEquals(4L, after.xp)
        assertEquals(1L, after.totalTaps)
    }

    @Test
    fun tapCrossesLevelBoundary() {
        val (after, leveledUp) = GameEngine.tap(GameState(xp = 99)).let { it.first to it.second }

        assertTrue(leveledUp)
        assertEquals(2, after.level)
        assertEquals(3L, after.xp)
    }

    @Test
    fun aLargeXpRewardCanCrossMultipleLevels() {
        val after = GameEngine.awardXp(GameState(), 600)

        assertEquals(4, after.level)
        assertEquals(0L, after.xp)
    }

    @Test
    fun maximumLongXpDoesNotOverflowOrLoopIndefinitely() {
        val after = GameEngine.awardXp(GameState(), Long.MAX_VALUE)

        assertTrue(after.level > 1)
        assertTrue(after.xp >= 0)
    }

    @Test
    fun upgradeChargesCoinsAndRaisesLevel() {
        val after = GameEngine.buy(GameState(coins = 100), "tap")

        assertEquals(50L, after.coins)
        assertEquals(1, after.upgrades.first { it.id == "tap" }.level)
    }

    @Test
    fun insufficientCurrencyCannotBuyUpgrade() {
        val before = GameState(coins = 49)

        assertEquals(before, GameEngine.buy(before, "tap"))
    }

    @Test
    fun zeroOfflineTimeAwardsNothing() {
        val before = stateWithPassiveIncome(lastSeenAt = 1_000)
        val (after, reward) = GameEngine.offlineReward(before, now = 1_000)

        assertEquals(0L, reward)
        assertEquals(before.timeShards, after.timeShards)
    }

    @Test
    fun negativeOfflineTimeAwardsNothing() {
        val before = stateWithPassiveIncome(lastSeenAt = 2_000)
        val (_, reward) = GameEngine.offlineReward(before, now = 1_000)

        assertEquals(0L, reward)
    }

    @Test
    fun sevenOfflineHoursAreCappedAtSix() {
        val (_, reward) = GameEngine.offlineReward(stateWithPassiveIncome(), now = 7 * 60 * 60 * 1_000L)

        assertEquals(21_600L, reward)
    }

    @Test
    fun tenOfflineHoursAreCappedAtSix() {
        val (_, reward) = GameEngine.offlineReward(stateWithPassiveIncome(), now = 10 * 60 * 60 * 1_000L)

        assertEquals(21_600L, reward)
    }

    @Test
    fun oneHundredOfflineHoursAreCappedAtSix() {
        val (_, reward) = GameEngine.offlineReward(stateWithPassiveIncome(), now = 100 * 60 * 60 * 1_000L)

        assertEquals(21_600L, reward)
    }

    @Test
    fun expeditionCompletionUsesPersistedEndTimestamp() {
        val started = GameEngine.startExpedition(GameState(), "egypt", now = 1_000)
        val restoredBeforeEnd = GameEngine.updateTime(started, now = 30_999)
        val restoredAfterEnd = GameEngine.updateTime(started, now = 31_000)

        assertEquals(ExpeditionStatus.IN_PROGRESS, restoredBeforeEnd.expedition.status)
        assertEquals(ExpeditionStatus.COMPLETED, restoredAfterEnd.expedition.status)
    }

    @Test
    fun completedExpeditionCanOnlyBeClaimedOnce() {
        val completed = GameEngine.updateTime(
            GameEngine.startExpedition(GameState(), "egypt", now = 1_000),
            now = 31_000
        )
        val firstClaim = GameEngine.claimExpedition(completed)
        val secondClaim = GameEngine.claimExpedition(firstClaim)

        assertEquals(175L, firstClaim.coins)
        assertEquals(firstClaim, secondClaim)
    }

    @Test
    fun dailyRewardAdvancesThroughSevenDayCycle() {
        var state = GameState()
        val start = LocalDate.of(2026, 1, 1)
        repeat(7) { day -> state = GameEngine.daily(state, start.plusDays(day.toLong()).toString()) }

        assertEquals(0, state.dailyDay)
        assertEquals(start.plusDays(6).toString(), state.lastDailyClaim)
        assertTrue(state.coins > 100)
    }

    @Test
    fun dailyRewardCannotBeClaimedTwiceOrByMovingClockBack() {
        val first = GameEngine.daily(GameState(), "2026-01-10")

        assertEquals(first, GameEngine.daily(first, "2026-01-10"))
        assertEquals(first, GameEngine.daily(first, "2026-01-09"))
        assertFalse(GameEngine.canClaimDaily(first, LocalDate.of(2026, 1, 9)))
    }
}
