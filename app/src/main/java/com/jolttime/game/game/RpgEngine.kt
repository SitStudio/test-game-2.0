package com.jolttime.game.game
import com.jolttime.game.domain.model.*
object RpgEngine{
 fun markIntroSeen(old:GameState)=old.copy(storyIntroSeen=true)
 fun dismissArtifact(old:GameState)=old.copy(pendingArtifactId=null)
 fun dismissStory(old:GameState)=old.copy(pendingStoryId=null)
 fun selectTeam(old:GameState,ids:List<String>):GameState{val valid=ids.distinct().filter{id->old.heroes.any{it.id==id}}.take(3);return if(valid.isEmpty())old else old.copy(selectedTeam=valid)}
 fun startMission(old:GameState,id:String):GameState{val index=GameContent.missions.indexOfFirst{it.id==id};return if(index<0||index>old.unlockedMissionIndex||old.activeMissionId!=null)old else old.copy(activeMissionId=id)}
 fun finishMission(old:GameState,victory:Boolean):GameState{val id=old.activeMissionId?:return old;if(!victory)return old.copy(activeMissionId=null);val mission=GameContent.missions.first{it.id==id};val heroes=old.heroes.map{if(it.id in old.selectedTeam)addHeroXp(it,mission.rewardXp)else it};var pending:String?=null;val artifacts=old.artifacts.map{a->if(a.id!=mission.artifactId)a else{val owned=(a.fragmentsOwned+mission.artifactFragments).coerceAtMost(a.fragmentsRequired);if(!a.isCompleted&&owned>=a.fragmentsRequired)pending=a.id;a.copy(fragmentsOwned=owned,isCompleted=owned>=a.fragmentsRequired)}};val index=GameContent.missions.indexOf(mission);return old.copy(accountXp=old.accountXp+mission.rewardXp,heroes=heroes,artifacts=artifacts,upgradeMaterials=old.upgradeMaterials+mission.rewardMaterial,timeEnergy=old.timeEnergy+10,completedMissionIds=old.completedMissionIds+id,unlockedMissionIndex=(index+1).coerceAtMost(GameContent.missions.lastIndex),activeMissionId=null,pendingStoryId=mission.storyAfter,pendingArtifactId=pending,chapterComplete=mission.nodeType==MissionNodeType.BOSS)}
 fun addHeroXp(hero:Hero,amount:Int):Hero{var level=hero.level;var xp=hero.xp+amount.coerceAtLeast(0);while(xp>=level*100){xp-=level*100;level++};return hero.copy(level=level,xp=xp)}
 fun upgradeHero(old:GameState,id:String):GameState{val hero=old.heroes.firstOrNull{it.id==id}?:return old;val cost=hero.level*25;if(old.upgradeMaterials<cost)return old;return old.copy(upgradeMaterials=old.upgradeMaterials-cost,heroes=old.heroes.map{if(it.id==id)it.copy(level=it.level+1)else it})}
 fun equipArtifact(old:GameState,heroId:String,artifactId:String):GameState=if(old.artifacts.none{it.id==artifactId&&it.isCompleted})old else old.copy(heroes=old.heroes.map{if(it.id==heroId)it.copy(equippedArtifactId=artifactId)else it})
}
