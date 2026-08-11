package com.jolttime.game.game

import com.jolttime.game.domain.model.*

object RpgEngine {
    fun selectTeam(old: GameState, ids: List<String>): GameState {
        val valid = ids.distinct().filter { id -> old.heroes.any { it.id == id } }.take(4)
        return if (valid.isEmpty()) old else old.copy(selectedTeam=valid)
    }

    fun startMission(old: GameState, missionId: String): GameState {
        val mission = GameContent.missions.firstOrNull { it.id == missionId } ?: return old
        val index = GameContent.missions.indexOf(mission)
        if (index > old.unlockedMissionIndex || old.activeBattle != null) return old
        val team = old.selectedTeam.mapNotNull { id -> old.heroes.firstOrNull { it.id == id } }
        return old.copy(activeBattle=BattleEngine.create(mission, team, old.artifacts))
    }

    fun battleAction(old: GameState, ability: Int, target: String, roll: Double=.99): GameState {
        val battle = old.activeBattle ?: return old
        return old.copy(activeBattle=BattleEngine.act(battle, ability, target, roll))
    }

    fun enemyAction(old: GameState): GameState = old.activeBattle?.let { old.copy(activeBattle=BattleEngine.enemyAction(it)) } ?: old

    fun claimVictory(old: GameState): GameState {
        val battle = old.activeBattle?.takeIf { it.result == BattleResult.VICTORY } ?: return old
        val mission = GameContent.missions.first { it.id == battle.missionId }
        val heroes = old.heroes.map { hero -> if(hero.id in old.selectedTeam) addHeroXp(hero, mission.rewardXp) else hero }
        var pending: String? = null
        val artifacts = old.artifacts.map { artifact ->
            if (artifact.id != mission.artifactId) artifact else {
                val owned = (artifact.fragmentsOwned + mission.artifactFragments).coerceAtMost(artifact.fragmentsRequired)
                if (!artifact.isCompleted && owned >= artifact.fragmentsRequired) pending = artifact.id
                artifact.copy(fragmentsOwned=owned, isCompleted=owned>=artifact.fragmentsRequired)
            }
        }
        val missionIndex = GameContent.missions.indexOf(mission)
        return old.copy(
            heroes=heroes, artifacts=artifacts, upgradeMaterials=old.upgradeMaterials + mission.rewardMaterial,
            timeEnergy=old.timeEnergy + 10, completedMissionIds=old.completedMissionIds + mission.id,
            unlockedMissionIndex=(missionIndex+1).coerceAtMost(GameContent.missions.lastIndex), activeBattle=null,
            pendingStoryId=mission.storyAfter, pendingArtifactId=pending, chapterComplete=mission.nodeType==MissionNodeType.BOSS,
        )
    }

    fun retry(old: GameState) = old.copy(activeBattle=null)
    fun dismissArtifact(old: GameState) = old.copy(pendingArtifactId=null)
    fun dismissStory(old: GameState) = old.copy(pendingStoryId=null)
    fun markIntroSeen(old: GameState) = old.copy(storyIntroSeen=true)
    fun equipArtifact(old:GameState, heroId:String, artifactId:String):GameState {
        if(old.artifacts.none { it.id==artifactId && it.isCompleted }) return old
        return old.copy(heroes=old.heroes.map { if(it.id==heroId) it.copy(equippedArtifactId=artifactId) else it })
    }
    fun upgradeHero(old:GameState, heroId:String):GameState {
        val hero=old.heroes.firstOrNull { it.id==heroId } ?: return old
        val cost=hero.level*25
        if(old.upgradeMaterials<cost) return old
        return old.copy(upgradeMaterials=old.upgradeMaterials-cost,heroes=old.heroes.map { if(it.id==heroId) it.copy(level=it.level+1) else it })
    }

    fun addHeroXp(hero: Hero, amount: Int): Hero {
        var level=hero.level; var xp=hero.xp + amount.coerceAtLeast(0)
        while(xp >= level*100) { xp -= level*100; level++ }
        return hero.copy(level=level,xp=xp)
    }
}
