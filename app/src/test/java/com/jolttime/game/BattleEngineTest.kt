package com.jolttime.game

import com.jolttime.game.domain.model.*
import com.jolttime.game.game.BattleEngine
import com.jolttime.game.game.RpgEngine
import org.junit.Assert.*
import org.junit.Test

class BattleEngineTest {
    private val mission = GameContent.missions[1]
    private fun battle() = BattleEngine.create(mission, GameContent.heroes, GameContent.artifacts)

    @Test fun turnOrderUsesSpeedAndStableTieBreak(){val b=battle();val speeds=b.order.map{id->b.combatants.first{it.instanceId==id}.speed};assertEquals(speeds.sortedDescending(),speeds)}
    @Test fun defenseReducesDamage(){val b=battle();val a=b.combatants.first{it.side==BattleSide.HERO};val low=b.combatants.first{it.side==BattleSide.ENEMY};val high=low.copy(defense=low.defense+100);assertTrue(BattleEngine.damage(a,low,100,.9).first>BattleEngine.damage(a,high,100,.9).first)}
    @Test fun criticalDamageIsGreater(){val b=battle();val a=b.combatants.first{it.side==BattleSide.HERO}.copy(criticalChance=.5);val d=b.combatants.first{it.side==BattleSide.ENEMY};assertTrue(BattleEngine.damage(a,d,100,.1).first>BattleEngine.damage(a,d,100,.9).first)}
    @Test fun basicAttackBuildsEnergy(){val b=battle();val actor=b.active!!;val target=b.combatants.first{it.side!=actor.side};val next=BattleEngine.act(b,0,target.instanceId);assertEquals(25,next.combatants.first{it.instanceId==actor.instanceId}.energy)}
    @Test fun statusEffectIsApplied(){val custom=GameContent.missions[0].copy(enemyIds=listOf("guard"));val base=BattleEngine.create(custom,listOf(GameContent.heroes[2]),GameContent.artifacts);val actor=base.active!!;val charged=base.copy(combatants=base.combatants.map{if(it.instanceId==actor.instanceId)it.copy(energy=100)else it});val target=charged.combatants.first{it.side!=actor.side};val next=BattleEngine.act(charged,1,target.instanceId);assertTrue(next.combatants.first{it.instanceId==target.instanceId}.statuses.any{it.type==StatusType.STUN})}
    @Test fun enemyAiDamagesWeakestLivingHero(){var b=battle();while(b.active?.side==BattleSide.HERO){val t=b.combatants.first{it.side==BattleSide.ENEMY};b=BattleEngine.act(b,0,t.instanceId)};val before=b.combatants.filter{it.side==BattleSide.HERO}.minBy{it.hp};val after=BattleEngine.enemyAction(b);assertTrue(after.combatants.first{it.instanceId==before.instanceId}.hp<before.hp)}
    @Test fun heroXpCanLevelMultipleTimes(){val h=RpgEngine.addHeroXp(GameContent.heroes[0],500);assertTrue(h.level>=3);assertTrue(h.xp>=0)}
    @Test fun teamSelectionPersistsValidMaximum(){val state=RpgEngine.selectTeam(GameState(),listOf("kael","mira","sahir","nadiya","bad"));assertEquals(4,state.selectedTeam.size);assertFalse("bad" in state.selectedTeam)}
    @Test fun completedArtifactChangesBattleStats(){val hero=GameContent.heroes[0].copy(equippedArtifactId="ankh");val plain=BattleEngine.create(mission,listOf(hero),GameContent.artifacts).combatants.first();val restored=GameContent.artifacts.map{if(it.id=="ankh")it.copy(fragmentsOwned=2,isCompleted=true)else it};val boosted=BattleEngine.create(mission,listOf(hero),restored).combatants.first();assertEquals(12,boosted.maxHp-plain.maxHp)}
    @Test fun victoryClaimCompletesMissionAndBossChapter(){val state=GameState(unlockedMissionIndex=GameContent.missions.lastIndex,activeBattle=BattleEngine.create(GameContent.missions.last(),GameContent.heroes,GameContent.artifacts).copy(result=BattleResult.VICTORY));val next=RpgEngine.claimVictory(state);assertTrue(GameContent.missions.last().id in next.completedMissionIds);assertTrue(next.chapterComplete)}
}
