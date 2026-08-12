package com.jolttime.game.domain.model

import kotlinx.serialization.Serializable

@Serializable enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
@Serializable enum class HeroRole { TANK, FIGHTER, RANGED, SUPPORT, CONTROLLER }
@Serializable enum class Stat { HP, ATTACK, DEFENSE, SPEED, CRIT }
@Serializable enum class AbilityKind { DAMAGE, HEAL, SHIELD, FREEZE, SLOW, DASH, AREA_DAMAGE }
@Serializable enum class MissionType { DEFEAT_ALL, SURVIVE, PROTECT_TARGET, CAPTURE_POINT, RECOVER_ARTIFACT, DESTROY_ANOMALY, BOSS }
@Serializable enum class MissionNodeType { STORY, BATTLE, ELITE, ARTIFACT, EVENT, BOSS }
@Serializable enum class BattleSide { HERO, ENEMY }
@Serializable enum class BattleResult { IN_PROGRESS, VICTORY, DEFEAT }

@Serializable data class Ability(
    val id: String,
    val nameKey: String,
    val kind: AbilityKind,
    val power: Int,
    val cooldownSeconds: Float,
    val energyCost: Int = 0,
    val range: Float = 0.22f,
    val radius: Float = 0.12f,
    val durationSeconds: Float = 0f,
)

@Serializable data class Hero(
    val id: String,
    val nameKey: String,
    val storyKey: String,
    val identityKey: String,
    val role: HeroRole,
    val rarity: Rarity,
    val level: Int = 1,
    val xp: Int = 0,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val criticalChance: Double,
    val attackRange: Float,
    val basic: Ability,
    val skill: Ability,
    val ultimate: Ability,
    val passiveKey: String,
    val equippedArtifactId: String? = null,
) {
    val xpToNext get() = level * 100
    val maxHp get() = baseHp + (level - 1) * 16
    val attack get() = baseAttack + (level - 1) * 3
    val defense get() = baseDefense + (level - 1) * 2
    val speed get() = baseSpeed + (level - 1) / 4
}

@Serializable data class EnemyTemplate(
    val id: String,
    val nameKey: String,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val attackRange: Float,
    val attackCooldown: Float,
    val elite: Boolean = false,
    val boss: Boolean = false,
)

@Serializable data class Artifact(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val effectKey: String,
    val epochId: String = "egypt",
    val rarity: Rarity,
    val fragmentsOwned: Int = 0,
    val fragmentsRequired: Int,
    val isCompleted: Boolean = false,
    val stat: Stat,
    val combatBonus: Int,
    val museumBonusPercent: Int,
)

@Serializable data class Mission(
    val id: String,
    val titleKey: String,
    val locationKey: String,
    val nodeType: MissionNodeType,
    val objective: MissionType,
    val enemyIds: List<String> = emptyList(),
    val durationSeconds: Int = 0,
    val rewardXp: Int,
    val rewardMaterial: Int,
    val artifactId: String? = null,
    val artifactFragments: Int = 0,
    val storyBefore: String? = null,
    val storyAfter: String? = null,
)

@Serializable data class DialogueLine(val speakerKey: String, val textKey: String)
@Serializable data class StoryScene(val id: String, val lines: List<DialogueLine>)

@Serializable data class GameState(
    val saveVersion: Int = 3,
    val accountLevel: Int = 1,
    val accountXp: Int = 0,
    val currentEpochId: String = "egypt",
    val unlockedMissionIndex: Int = 0,
    val completedMissionIds: Set<String> = emptySet(),
    val heroes: List<Hero> = GameContent.heroes,
    val selectedTeam: List<String> = GameContent.heroes.take(3).map { it.id },
    val artifacts: List<Artifact> = GameContent.artifacts,
    val upgradeMaterials: Int = 0,
    val timeEnergy: Int = 0,
    val storyIntroSeen: Boolean = false,
    val activeMissionId: String? = null,
    val pendingStoryId: String? = null,
    val pendingArtifactId: String? = null,
    val chapterComplete: Boolean = false,
    val languageTag: String = "",
    // Version-one values are retained solely for safe migration.
    val level: Int = 1, val xp: Long = 0, val timeShards: Long = 0, val coins: Long = 100,
    val totalTaps: Long = 0, val lastSeenAt: Long = System.currentTimeMillis(),
) {
    val museumProgress get() = if (artifacts.isEmpty()) 0f else artifacts.count { it.isCompleted } / artifacts.size.toFloat()
    fun sanitized(now: Long = System.currentTimeMillis()): GameState {
        val repairedHeroes = GameContent.heroes.map { base -> heroes.firstOrNull { it.id == base.id }?.let {
            base.copy(level=it.level.coerceAtLeast(1),xp=it.xp.coerceAtLeast(0),equippedArtifactId=it.equippedArtifactId)
        } ?: base }
        val repairedArtifacts = GameContent.artifacts.map { base -> artifacts.firstOrNull { it.id==base.id }?.let {
            val owned=it.fragmentsOwned.coerceIn(0,base.fragmentsRequired);base.copy(fragmentsOwned=owned,isCompleted=owned>=base.fragmentsRequired)
        } ?: base }
        val validTeam=selectedTeam.distinct().filter { id->repairedHeroes.any{it.id==id} }.take(3)
        val team=(validTeam+repairedHeroes.map{it.id}.filterNot{it in validTeam}).take(3)
        return copy(saveVersion=3,accountLevel=accountLevel.coerceAtLeast(1),accountXp=accountXp.coerceAtLeast(0),
            unlockedMissionIndex=unlockedMissionIndex.coerceIn(0,GameContent.missions.lastIndex),heroes=repairedHeroes,
            selectedTeam=team,artifacts=repairedArtifacts,
            upgradeMaterials=upgradeMaterials.coerceAtLeast(0),timeEnergy=timeEnergy.coerceAtLeast(0),activeMissionId=activeMissionId?.takeIf{id->GameContent.missions.any{it.id==id}},
            level=level.coerceAtLeast(1),xp=xp.coerceAtLeast(0),timeShards=timeShards.coerceAtLeast(0),coins=coins.coerceAtLeast(0),totalTaps=totalTaps.coerceAtLeast(0),lastSeenAt=lastSeenAt.coerceIn(0,now))
    }
}
