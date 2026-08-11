package com.jolttime.game.domain.model

import kotlinx.serialization.Serializable

@Serializable enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
@Serializable enum class HeroRole { VANGUARD, STRIKER, SCOUT, SUPPORT, SCHOLAR }
@Serializable enum class Stat { HP, ATTACK, DEFENSE, SPEED, CRIT }
@Serializable enum class StatusType { STUN, BURN, BLEED, WEAKNESS, SHIELD, TIME_SLOW }
@Serializable enum class AbilityKind { DAMAGE, HEAL, SHIELD, STUN, BURN, SLOW }
@Serializable enum class MissionType { DEFEAT_ALL, SURVIVE_TURNS, BOSS }
@Serializable enum class MissionNodeType { STORY, BATTLE, ELITE, ARTIFACT, NPC, BOSS }
@Serializable enum class BattleSide { HERO, ENEMY }
@Serializable enum class BattleResult { IN_PROGRESS, VICTORY, DEFEAT }

@Serializable
data class Ability(
    val id: String,
    val nameKey: String,
    val kind: AbilityKind,
    val power: Int,
    val energyCost: Int = 0,
    val status: StatusType? = null,
    val statusTurns: Int = 0,
)

@Serializable
data class Hero(
    val id: String,
    val nameKey: String,
    val storyKey: String,
    val role: HeroRole,
    val level: Int = 1,
    val xp: Int = 0,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val criticalChance: Double,
    val basic: Ability,
    val skill: Ability,
    val ultimate: Ability,
    val passiveKey: String,
    val equippedArtifactId: String? = null,
) {
    val xpToNext: Int get() = level * 100
    val maxHp: Int get() = baseHp + (level - 1) * 14
    val attack: Int get() = baseAttack + (level - 1) * 3
    val defense: Int get() = baseDefense + (level - 1) * 2
    val speed: Int get() = baseSpeed + (level - 1) / 3
}

@Serializable
data class EnemyTemplate(
    val id: String,
    val nameKey: String,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val criticalChance: Double = .05,
    val abilities: List<Ability>,
    val elite: Boolean = false,
    val boss: Boolean = false,
)

@Serializable data class TimedStatus(val type: StatusType, val turns: Int, val value: Int = 0)

@Serializable
data class Combatant(
    val instanceId: String,
    val templateId: String,
    val nameKey: String,
    val side: BattleSide,
    val maxHp: Int,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val criticalChance: Double,
    val energy: Int = 0,
    val abilities: List<Ability>,
    val statuses: List<TimedStatus> = emptyList(),
    val boss: Boolean = false,
) { val alive: Boolean get() = hp > 0 }

@Serializable
data class BattleState(
    val missionId: String,
    val objective: MissionType,
    val surviveTurns: Int = 0,
    val combatants: List<Combatant>,
    val order: List<String>,
    val activeIndex: Int = 0,
    val round: Int = 1,
    val result: BattleResult = BattleResult.IN_PROGRESS,
    val logKey: String = "battle_started",
) {
    val activeId: String? get() = order.getOrNull(activeIndex)
    val active: Combatant? get() = combatants.firstOrNull { it.instanceId == activeId }
}

@Serializable
data class Artifact(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val epochId: String = "egypt",
    val rarity: Rarity,
    val fragmentsOwned: Int = 0,
    val fragmentsRequired: Int,
    val isCompleted: Boolean = false,
    val stat: Stat,
    val combatBonus: Int,
    val museumBonusPercent: Int,
)

@Serializable
data class Mission(
    val id: String,
    val titleKey: String,
    val locationKey: String,
    val nodeType: MissionNodeType,
    val objective: MissionType,
    val enemyIds: List<String> = emptyList(),
    val surviveTurns: Int = 0,
    val rewardXp: Int,
    val rewardMaterial: Int,
    val artifactId: String? = null,
    val artifactFragments: Int = 0,
    val storyBefore: String? = null,
    val storyAfter: String? = null,
)

@Serializable data class DialogueLine(val speakerKey: String, val textKey: String)
@Serializable data class StoryScene(val id: String, val lines: List<DialogueLine>)

@Serializable
data class GameState(
    val saveVersion: Int = 2,
    val currentEpochId: String = "egypt",
    val unlockedMissionIndex: Int = 0,
    val completedMissionIds: Set<String> = emptySet(),
    val heroes: List<Hero> = GameContent.heroes,
    val selectedTeam: List<String> = GameContent.heroes.take(4).map { it.id },
    val artifacts: List<Artifact> = GameContent.artifacts,
    val upgradeMaterials: Int = 0,
    val timeEnergy: Int = 0,
    val storyIntroSeen: Boolean = false,
    val activeBattle: BattleState? = null,
    val pendingStoryId: String? = null,
    val pendingArtifactId: String? = null,
    val chapterComplete: Boolean = false,
    val languageTag: String = "",
    // Legacy values intentionally remain optional so version-one clicker saves migrate safely.
    val level: Int = 1,
    val xp: Long = 0,
    val timeShards: Long = 0,
    val coins: Long = 100,
    val totalTaps: Long = 0,
    val lastSeenAt: Long = System.currentTimeMillis(),
) {
    val museumProgress: Float get() = if (artifacts.isEmpty()) 0f else artifacts.count { it.isCompleted } / artifacts.size.toFloat()
    val museumAttackBonusPercent: Int get() = artifacts.filter { it.isCompleted }.sumOf { it.museumBonusPercent }

    fun sanitized(now: Long = System.currentTimeMillis()): GameState {
        val knownHeroes = GameContent.heroes.associateBy { it.id }
        val repairedHeroes = GameContent.heroes.map { default ->
            heroes.firstOrNull { it.id == default.id }?.copy(
                level = heroes.first { it.id == default.id }.level.coerceAtLeast(1),
                xp = heroes.first { it.id == default.id }.xp.coerceAtLeast(0),
            ) ?: knownHeroes.getValue(default.id)
        }
        val repairedArtifacts = GameContent.artifacts.map { default ->
            artifacts.firstOrNull { it.id == default.id }?.let {
                val owned = it.fragmentsOwned.coerceIn(0, default.fragmentsRequired)
                default.copy(fragmentsOwned = owned, isCompleted = owned >= default.fragmentsRequired)
            } ?: default
        }
        val team = selectedTeam.distinct().filter { id -> repairedHeroes.any { it.id == id } }.take(4)
        return copy(
            saveVersion = 2,
            currentEpochId = "egypt",
            unlockedMissionIndex = unlockedMissionIndex.coerceIn(0, GameContent.missions.lastIndex),
            heroes = repairedHeroes,
            selectedTeam = if (team.isEmpty()) repairedHeroes.take(4).map { it.id } else team,
            artifacts = repairedArtifacts,
            upgradeMaterials = upgradeMaterials.coerceAtLeast(0),
            timeEnergy = timeEnergy.coerceAtLeast(0),
            level = level.coerceAtLeast(1), xp = xp.coerceAtLeast(0),
            timeShards = timeShards.coerceAtLeast(0), coins = coins.coerceAtLeast(0),
            totalTaps = totalTaps.coerceAtLeast(0), lastSeenAt = lastSeenAt.coerceIn(0, now),
        )
    }
}
