package com.jolttime.game
import com.jolttime.game.domain.model.*
import com.jolttime.game.game.*
import org.junit.Assert.*
import org.junit.Test

class ActionBattleEngineTest{
 private fun engine(index:Int=1)=ActionBattleEngine(GameContent.missions[index],GameContent.heroes.take(3),GameContent.artifacts)
 @Test fun movementInputMovesControlledHero(){val e=engine();val before=e.state.actors.first{it.id==e.state.activeHeroId}.position;e.step(.05f,PlayerInput(Vec2(1f,0f)));val after=e.state.actors.first{it.id==e.state.activeHeroId}.position;assertTrue(after.x>before.x)}
 @Test fun switchSelectsAnotherLivingHero(){val e=engine();val before=e.state.activeHeroId;e.command(ActionCommand.SWITCH);assertNotEquals(before,e.state.activeHeroId)}
 @Test fun basicAttackDamagesTargetAndBuildsEnergy(){val e=closeCombat();val before=e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp};e.command(ActionCommand.ATTACK);val active=e.state.actors.first{it.id==e.state.activeHeroId};assertTrue(e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp}<before);assertTrue(active.energy>0)}
 @Test fun cooldownPreventsImmediateRepeatedAttack(){val e=closeCombat();e.command(ActionCommand.ATTACK);val hp=e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp};e.command(ActionCommand.ATTACK);assertEquals(hp,e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp})}
 @Test fun activeSkillDealsDamageAndStartsCooldown(){val e=closeCombat();val before=e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp};e.command(ActionCommand.SKILL);val active=e.state.actors.first{it.id==e.state.activeHeroId};assertTrue(e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp}<before);assertTrue(active.skillCooldown>0)}
 @Test fun ultimateRequiresFullEnergy(){val e=closeCombat();val before=e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp};e.command(ActionCommand.ULTIMATE);assertEquals(before,e.state.actors.filter{it.side==BattleSide.ENEMY}.sumOf{it.hp})}
 @Test fun alliesMoveAndAssistWithoutPlayerCommands(){val e=engine();val before=e.state.actors.filter{it.side==BattleSide.HERO&&it.id!=e.state.activeHeroId}.map{it.position};repeat(20){e.step(.05f)};val after=e.state.actors.filter{it.side==BattleSide.HERO&&it.id!=e.state.activeHeroId}.map{it.position};assertNotEquals(before,after)}
 @Test fun enemiesChaseHeroesInRealTime(){val e=engine();val before=e.state.actors.first{it.side==BattleSide.ENEMY}.position;repeat(10){e.step(.05f)};assertNotEquals(before,e.state.actors.first{it.side==BattleSide.ENEMY}.position)}
 @Test fun defenseReducesDamage(){val mission=GameContent.missions[1].copy(enemyIds=listOf("raider"));val e=ActionBattleEngine(mission,listOf(GameContent.heroes[2]),emptyList());val low=e.state.actors.first{it.side==BattleSide.ENEMY};val high=low.copy(defense=low.defense+100);assertTrue(expectedDamage(e.state.actors.first(),low)>expectedDamage(e.state.actors.first(),high))}
 @Test fun selectedTeamIsLimitedToThree(){val state=RpgEngine.selectTeam(GameState(),GameContent.heroes.map{it.id});assertEquals(3,state.selectedTeam.size)}
 @Test fun artifactStatIsApplied(){val hero=GameContent.heroes[0].copy(equippedArtifactId="ankh");val normal=ActionBattleEngine.initial(GameContent.missions[1],listOf(hero),GameContent.artifacts).actors.first();val restored=GameContent.artifacts.map{if(it.id=="ankh")it.copy(fragmentsOwned=2,isCompleted=true)else it};val boosted=ActionBattleEngine.initial(GameContent.missions[1],listOf(hero),restored).actors.first();assertEquals(18,boosted.maxHp-normal.maxHp)}
 @Test fun victoryRewardsAdvanceCampaign(){val old=GameState(activeMissionId="nile",unlockedMissionIndex=1);val next=RpgEngine.finishMission(old,true);assertTrue("nile" in next.completedMissionIds);assertEquals(2,next.unlockedMissionIndex);assertNull(next.activeMissionId)}
 private fun expectedDamage(a:Actor,d:Actor)=(a.attack*100f/(100+d.defense)).toInt()
 private fun closeCombat():ActionBattleEngine{val mission=GameContent.missions[1].copy(enemyIds=listOf("raider"));val e=ActionBattleEngine(mission,listOf(GameContent.heroes[0]),emptyList());repeat(58){e.step(.05f,PlayerInput(Vec2(1f,0f)))};return e}
}
