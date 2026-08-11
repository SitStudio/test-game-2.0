package com.jolttime.game.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
@Serializable enum class BonusType { TAP_POWER, PASSIVE_INCOME, COINS, MUSEUM }
@Serializable enum class EffectType { TAP_POWER, PASSIVE_INCOME, DISCOVERY_CHANCE, MUSEUM_BONUS }
@Serializable enum class ExpeditionStatus { IDLE, IN_PROGRESS, COMPLETED }

@Serializable data class Epoch(val id: String, val name: String, val description: String, val unlockLevel: Int, val accent: Long)
@Serializable data class Artifact(
    val id: String, val name: String, val civilization: String, val rarity: Rarity,
    val description: String, val fragmentsOwned: Int = 0, val fragmentsRequired: Int,
    val isCompleted: Boolean = false, val bonusType: BonusType, val bonusValue: Double
)
@Serializable data class Upgrade(
    val id: String, val name: String, val description: String, val level: Int = 0,
    val maxLevel: Int, val baseCost: Long, val costMultiplier: Double,
    val effectType: EffectType, val effectValue: Double
) {
    fun cost(): Long {
        val calculated = baseCost.coerceAtLeast(0).toDouble() * costMultiplier.coerceAtLeast(1.0).pow(level.coerceAtLeast(0))
        return if (calculated.isFinite()) calculated.toLong().coerceAtLeast(0) else Long.MAX_VALUE
    }
}
@Serializable data class Expedition(
    val civilizationId: String = "", val status: ExpeditionStatus = ExpeditionStatus.IDLE,
    val startedAt: Long = 0, val endsAt: Long = 0
)
@Serializable data class GameState(
    val level: Int = 1, val xp: Long = 0, val timeShards: Long = 0, val coins: Long = 100,
    val totalTaps: Long = 0, val upgrades: List<Upgrade> = GameContent.upgrades,
    val artifacts: List<Artifact> = GameContent.artifacts, val expedition: Expedition = Expedition(),
    val dailyDay: Int = 0, val lastDailyClaim: String = "", val soundEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true, val lastSeenAt: Long = System.currentTimeMillis()
) {
    val xpToNextLevel get() = level.coerceAtLeast(1).toLong().saturatedMultiply(100L)
    val unlockedEpochs get() = GameContent.epochs.filter { level >= it.unlockLevel }.map { it.id }
    val completedArtifacts get() = artifacts.count { it.isCompleted }
    val museumProgress get() = if (artifacts.isEmpty()) 0f else (completedArtifacts / artifacts.size.toFloat()).coerceIn(0f, 1f)
    val tapPower: Long get() = upgrades.asSequence()
        .filter { it.effectType == EffectType.TAP_POWER }
        .map { (it.level.coerceAtLeast(0) * it.effectValue.coerceAtLeast(0.0)).toSafeLong() }
        .plus(artifacts.asSequence().filter { it.isCompleted && it.bonusType == BonusType.TAP_POWER }.map { it.bonusValue.toSafeLong() })
        .fold(1L) { total, bonus -> total.saturatedAdd(bonus) }
    val passiveIncomePerSecond: Long get() = upgrades.asSequence()
        .filter { it.effectType == EffectType.PASSIVE_INCOME }
        .map { (it.level.coerceAtLeast(0) * it.effectValue.coerceAtLeast(0.0)).toSafeLong() }
        .plus(artifacts.asSequence().filter { it.isCompleted && it.bonusType == BonusType.PASSIVE_INCOME }.map { it.bonusValue.toSafeLong() })
        .fold(0L) { total, bonus -> total.saturatedAdd(bonus) }
    val discoveryChance get() = (.08 + upgrades.filter { it.effectType == EffectType.DISCOVERY_CHANCE }.sumOf { it.level.coerceAtLeast(0) * it.effectValue }).coerceIn(0.0, 1.0)

    /** Repairs impossible values from old, manually edited, or partially migrated saves. */
    fun sanitized(now: Long = System.currentTimeMillis()): GameState = copy(
        level = level.coerceAtLeast(1),
        xp = xp.coerceAtLeast(0),
        timeShards = timeShards.coerceAtLeast(0),
        coins = coins.coerceAtLeast(0),
        totalTaps = totalTaps.coerceAtLeast(0),
        upgrades = upgrades.map { it.copy(level = it.level.coerceIn(0, it.maxLevel.coerceAtLeast(0))) },
        artifacts = artifacts.map {
            val required = it.fragmentsRequired.coerceAtLeast(1)
            val owned = it.fragmentsOwned.coerceIn(0, required)
            it.copy(fragmentsRequired = required, fragmentsOwned = owned, isCompleted = owned >= required)
        },
        dailyDay = dailyDay.coerceIn(0, 6),
        lastSeenAt = lastSeenAt.coerceAtLeast(0).coerceAtMost(now)
    )
}

internal fun Long.saturatedAdd(other: Long): Long =
    if (other > 0 && this > Long.MAX_VALUE - other) Long.MAX_VALUE
    else if (other < 0 && this < Long.MIN_VALUE - other) Long.MIN_VALUE
    else this + other

internal fun Long.saturatedMultiply(other: Long): Long = when {
    this == 0L || other == 0L -> 0L
    this > 0 && other > 0 && this > Long.MAX_VALUE / other -> Long.MAX_VALUE
    else -> this * other
}

private fun Double.toSafeLong(): Long = when {
    isNaN() || this <= 0.0 -> 0L
    this >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
    else -> toLong()
}
