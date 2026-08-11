package com.jolttime.game.game

import com.jolttime.game.domain.model.*
import java.math.BigInteger
import java.time.LocalDate
import kotlin.random.Random

object GameEngine {
    fun tap(old: GameState): Pair<GameState, Boolean> {
        val safe = old.sanitized()
        val progressed = awardXp(safe, 4)
        var artifacts = safe.artifacts
        if (Random.nextDouble() < safe.discoveryChance) {
            artifacts = addFragment(artifacts, progressed.unlockedEpochs.randomOrNull())
        }
        return progressed.copy(
            timeShards = safe.timeShards.saturatedAdd(safe.tapPower),
            coins = safe.coins.saturatedAdd(1),
            totalTaps = safe.totalTaps.saturatedAdd(1),
            artifacts = artifacts
        ) to (progressed.level > safe.level)
    }

    fun awardXp(old: GameState, amount: Long): GameState {
        val initialLevel = old.level.coerceAtLeast(1)
        val available = old.xp.coerceAtLeast(0).saturatedAdd(amount.coerceAtLeast(0))
        var low = 0
        var high = Int.MAX_VALUE - initialLevel
        while (low < high) {
            val middle = low + (high - low + 1) / 2
            if (xpCost(initialLevel, middle) <= BigInteger.valueOf(available)) low = middle else high = middle - 1
        }
        val spent = xpCost(initialLevel, low).toLong()
        return old.copy(level = initialLevel + low, xp = (available - spent).coerceAtLeast(0))
    }

    private fun xpCost(startingLevel: Int, levels: Int): BigInteger {
        if (levels <= 0) return BigInteger.ZERO
        val count = BigInteger.valueOf(levels.toLong())
        val sequence = BigInteger.valueOf(2L * startingLevel + levels - 1L)
        return count.multiply(sequence).multiply(BigInteger.valueOf(50))
    }

    fun buy(old: GameState, id: String): GameState {
        val safe = old.sanitized()
        val upgrade = safe.upgrades.firstOrNull { it.id == id } ?: return safe
        val cost = upgrade.cost()
        if (safe.coins < cost || upgrade.level >= upgrade.maxLevel) return safe
        return safe.copy(
            coins = (safe.coins - cost).coerceAtLeast(0),
            upgrades = safe.upgrades.map { if (it.id == id) it.copy(level = it.level + 1) else it }
        )
    }

    fun startExpedition(old: GameState, civilization: String, now: Long = System.currentTimeMillis()) =
        if (old.expedition.status != ExpeditionStatus.IDLE || civilization !in old.unlockedEpochs) old
        else old.copy(expedition = Expedition(civilization, ExpeditionStatus.IN_PROGRESS, now, now.saturatedAdd(30_000)))

    fun updateTime(old: GameState, now: Long = System.currentTimeMillis()): GameState {
        val expedition = if (old.expedition.status == ExpeditionStatus.IN_PROGRESS && now >= old.expedition.endsAt) {
            old.expedition.copy(status = ExpeditionStatus.COMPLETED)
        } else old.expedition
        return old.copy(expedition = expedition)
    }

    fun claimExpedition(old: GameState): GameState {
        val safe = old.sanitized()
        if (safe.expedition.status != ExpeditionStatus.COMPLETED) return safe
        val artifacts = addFragment(safe.artifacts, safe.expedition.civilizationId)
        return awardXp(safe, 30).copy(
            coins = safe.coins.saturatedAdd(75),
            artifacts = artifacts,
            expedition = Expedition()
        )
    }

    fun daily(old: GameState, today: String = LocalDate.now().toString()): GameState {
        val safe = old.sanitized()
        val claimDate = runCatching { LocalDate.parse(today) }.getOrNull() ?: return old
        if (!canClaimDaily(safe, claimDate)) return safe
        val day = safe.dailyDay
        var artifacts = safe.artifacts
        if (day == 3 || day == 6) artifacts = addFragment(artifacts, safe.unlockedEpochs.randomOrNull())
        return awardXp(safe, 10L * (day + 1)).copy(
            dailyDay = (day + 1) % 7,
            lastDailyClaim = claimDate.toString(),
            timeShards = safe.timeShards.saturatedAdd(50L * (day + 1)),
            coins = safe.coins.saturatedAdd(25L * (day + 1)),
            artifacts = artifacts
        )
    }

    fun canClaimDaily(old: GameState, today: LocalDate = LocalDate.now()): Boolean {
        val previousDate = runCatching { LocalDate.parse(old.lastDailyClaim) }.getOrNull()
        return previousDate == null || today.isAfter(previousDate)
    }

    fun offlineReward(old: GameState, now: Long = System.currentTimeMillis()): Pair<GameState, Long> {
        val safe = old.sanitized(now.coerceAtLeast(0))
        val elapsedMillis = if (now > safe.lastSeenAt) now - safe.lastSeenAt else 0L
        val seconds = (elapsedMillis / 1_000).coerceIn(0, 21_600)
        val reward = seconds.saturatedMultiply(safe.passiveIncomePerSecond.coerceAtLeast(0))
        return safe.copy(
            timeShards = safe.timeShards.saturatedAdd(reward),
            lastSeenAt = now.coerceAtLeast(0)
        ) to reward
    }

    fun passiveTick(old: GameState, now: Long = System.currentTimeMillis()): GameState = old.copy(
        timeShards = old.timeShards.coerceAtLeast(0).saturatedAdd(old.passiveIncomePerSecond.coerceAtLeast(0)),
        lastSeenAt = now.coerceAtLeast(0)
    )

    private fun addFragment(all: List<Artifact>, civilization: String?): List<Artifact> {
        val choices = all.filter { it.civilization == civilization && !it.isCompleted }
        val target = choices.randomOrNull() ?: return all
        return all.map {
            if (it.id == target.id) {
                val required = it.fragmentsRequired.coerceAtLeast(1)
                val owned = it.fragmentsOwned.coerceAtLeast(0).plus(1).coerceAtMost(required)
                it.copy(fragmentsOwned = owned, fragmentsRequired = required, isCompleted = owned >= required)
            } else it
        }
    }
}
