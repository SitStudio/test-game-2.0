package com.jolttime.game.game

import com.jolttime.game.domain.model.*
import kotlin.math.sqrt

data class Vec2(val x:Float,val y:Float){fun distance(o:Vec2)=sqrt((x-o.x)*(x-o.x)+(y-o.y)*(y-o.y));fun normalized():Vec2{val d=sqrt(x*x+y*y);return if(d<.001f)Vec2(0f,0f)else Vec2(x/d,y/d)};operator fun plus(o:Vec2)=Vec2(x+o.x,y+o.y);operator fun minus(o:Vec2)=Vec2(x-o.x,y-o.y);operator fun times(v:Float)=Vec2(x*v,y*v)}
data class Actor(val id:String,val templateId:String,val nameKey:String,val side:BattleSide,val position:Vec2,val hp:Int,val maxHp:Int,val attack:Int,val defense:Int,val speed:Int,val range:Float,val crit:Double,val artifactId:String?=null,val cooldown:Float=0f,val skillCooldown:Float=0f,val ultimateCooldown:Float=0f,val energy:Int=0,val shield:Int=0,val frozen:Float=0f,val slowed:Float=0f,val boss:Boolean=false){val alive get()=hp>0}
data class ActionBattleState(val missionId:String,val actors:List<Actor>,val activeHeroId:String,val elapsed:Float=0f,val result:BattleResult=BattleResult.IN_PROGRESS,val bossStage:Int=1,val message:String="battle_started")
data class PlayerInput(val movement:Vec2=Vec2(0f,0f))
enum class ActionCommand{ATTACK,SKILL,ULTIMATE,SWITCH}

class ActionBattleEngine(private val mission:Mission,heroes:List<Hero>,artifacts:List<Artifact>){
    var state:ActionBattleState=initial(mission,heroes,artifacts);private set
    fun step(dtValue:Float,input:PlayerInput=PlayerInput()):ActionBattleState{
        if(state.result!=BattleResult.IN_PROGRESS)return state
        val dt=dtValue.coerceIn(0f,.05f);var actors=state.actors.map{it.copy(cooldown=(it.cooldown-dt).coerceAtLeast(0f),skillCooldown=(it.skillCooldown-dt).coerceAtLeast(0f),ultimateCooldown=(it.ultimateCooldown-dt).coerceAtLeast(0f),frozen=(it.frozen-dt).coerceAtLeast(0f),slowed=(it.slowed-dt).coerceAtLeast(0f))}
        val active=actors.firstOrNull{it.id==state.activeHeroId&&it.alive}
        if(active!=null){val velocity=input.movement.normalized()*(active.speed/420f*dt);actors=actors.replace(active.copy(position=(active.position+velocity).bounded()))}
        actors=alliesAssist(actors,dt);actors=enemiesAct(actors,dt)
        val elapsed=state.elapsed+dt;val heroesAlive=actors.any{it.side==BattleSide.HERO&&it.alive};val enemiesAlive=actors.any{it.side==BattleSide.ENEMY&&it.alive}
        val result=when{!heroesAlive->BattleResult.DEFEAT;mission.objective==MissionType.SURVIVE&&elapsed>=mission.durationSeconds->BattleResult.VICTORY;!enemiesAlive->BattleResult.VICTORY;else->BattleResult.IN_PROGRESS}
        val activeId=actors.firstOrNull{it.id==state.activeHeroId&&it.alive}?.id?:actors.firstOrNull{it.side==BattleSide.HERO&&it.alive}?.id.orEmpty()
        val boss=actors.firstOrNull{it.boss};val stage=if(boss!=null&&boss.hp<=boss.maxHp/2)2 else state.bossStage
        state=state.copy(actors=actors,activeHeroId=activeId,elapsed=elapsed,result=result,bossStage=stage);return state
    }
    fun command(command:ActionCommand,roll:Double=.99):ActionBattleState{
        if(state.result!=BattleResult.IN_PROGRESS)return state
        if(command==ActionCommand.SWITCH){val living=state.actors.filter{it.side==BattleSide.HERO&&it.alive};val index=living.indexOfFirst{it.id==state.activeHeroId};state=state.copy(activeHeroId=living[(index+1).mod(living.size)].id,message="battle_switched");return state}
        val actor=state.actors.first{it.id==state.activeHeroId};val hero=GameContent.heroes.first{it.id==actor.templateId};val ability=when(command){ActionCommand.ATTACK->hero.basic;ActionCommand.SKILL->hero.skill;ActionCommand.ULTIMATE->hero.ultimate;else->hero.basic}
        if(actor.frozen>0||command==ActionCommand.ATTACK&&actor.cooldown>0||command==ActionCommand.SKILL&&actor.skillCooldown>0||command==ActionCommand.ULTIMATE&&(actor.energy<100||actor.ultimateCooldown>0))return state
        var actors=state.actors;val enemies=actors.filter{it.side==BattleSide.ENEMY&&it.alive};val target=enemies.minByOrNull{it.position.distance(actor.position)}
        when(ability.kind){
            AbilityKind.HEAL->{actors=actors.map{if(it.side==BattleSide.HERO&&it.alive)it.copy(hp=(it.hp+ability.power).coerceAtMost(it.maxHp))else it}}
            AbilityKind.SHIELD->{actors=actors.map{if(it.side==BattleSide.HERO&&it.alive)it.copy(shield=it.shield+ability.power)else it}}
            AbilityKind.DASH->{if(target!=null){val direction=(target.position-actor.position).normalized();actors=actors.replace(actor.copy(position=(actor.position+direction*.18f).bounded()));actors=hit(actors,actor.id,target.id,ability,roll)}}
            AbilityKind.AREA_DAMAGE,AbilityKind.FREEZE,AbilityKind.SLOW->{val center=target?.position?:actor.position;actors.filter{it.side==BattleSide.ENEMY&&it.alive&&it.position.distance(center)<=ability.radius}.forEach{enemy->actors=hit(actors,actor.id,enemy.id,ability,roll)}}
            else->{if(target!=null&&target.position.distance(actor.position)<=ability.range)actors=hit(actors,actor.id,target.id,ability,roll)}
        }
        val currentActor=actors.first{it.id==actor.id};val refreshed=currentActor.copy(cooldown=if(command==ActionCommand.ATTACK)ability.cooldownSeconds else currentActor.cooldown,skillCooldown=if(command==ActionCommand.SKILL)ability.cooldownSeconds else currentActor.skillCooldown,ultimateCooldown=if(command==ActionCommand.ULTIMATE)ability.cooldownSeconds else currentActor.ultimateCooldown,energy=if(command==ActionCommand.ULTIMATE)0 else (currentActor.energy+if(command==ActionCommand.ATTACK)18 else 8).coerceAtMost(100),shield=currentActor.shield+if(command==ActionCommand.ULTIMATE&&currentActor.artifactId=="ankh")40 else 0)
        state=state.copy(actors=actors.replace(refreshed),message="battle_${command.name.lowercase()}");return state
    }
    private fun alliesAssist(source:List<Actor>,dt:Float):List<Actor>{var actors=source;source.filter{it.side==BattleSide.HERO&&it.alive&&it.id!=state.activeHeroId}.forEach{ally->val target=actors.filter{it.side==BattleSide.ENEMY&&it.alive}.minByOrNull{it.position.distance(ally.position)}?:return@forEach;val distance=ally.position.distance(target.position);if(distance>ally.range){val followTarget=actors.firstOrNull{it.id==state.activeHeroId}?.position?:target.position;actors=actors.replace(ally.copy(position=(ally.position+(followTarget-ally.position).normalized()*(ally.speed/520f*dt)).bounded()))}else if(ally.cooldown<=0){actors=hit(actors,ally.id,target.id,GameContent.heroes.first{it.id==ally.templateId}.basic,.85);actors=actors.replace(actors.first{it.id==ally.id}.copy(cooldown=.9f))}};return actors}
    private fun enemiesAct(source:List<Actor>,dt:Float):List<Actor>{var actors=source;source.filter{it.side==BattleSide.ENEMY&&it.alive}.forEach{enemy->if(enemy.frozen>0)return@forEach;val target=actors.filter{it.side==BattleSide.HERO&&it.alive}.minByOrNull{it.position.distance(enemy.position)}?:return@forEach;val distance=enemy.position.distance(target.position);if(distance>enemy.range){val multiplier=if(enemy.slowed>0).5f else 1f;actors=actors.replace(enemy.copy(position=(enemy.position+(target.position-enemy.position).normalized()*(enemy.speed/500f*dt*multiplier)).bounded()))}else if(enemy.cooldown<=0){val template=GameContent.enemies.getValue(enemy.templateId);if(enemy.boss&&state.bossStage==2){actors.filter{it.side==BattleSide.HERO&&it.alive}.forEach{hero->actors=damage(actors,enemy.id,hero.id,135,.9)}}else actors=damage(actors,enemy.id,target.id,100,.9);actors=actors.replace(actors.first{it.id==enemy.id}.copy(cooldown=template.attackCooldown))}};return actors}
    private fun hit(source:List<Actor>,attackerId:String,targetId:String,ability:Ability,roll:Double):List<Actor>{var out=damage(source,attackerId,targetId,ability.power,roll);if(ability.kind==AbilityKind.FREEZE)out=out.update(targetId){it.copy(frozen=ability.durationSeconds)};if(ability.kind==AbilityKind.SLOW)out=out.update(targetId){it.copy(slowed=ability.durationSeconds)};return out}
    private fun damage(source:List<Actor>,attackerId:String,targetId:String,power:Int,roll:Double):List<Actor>{val a=source.first{it.id==attackerId};val d=source.first{it.id==targetId};val critical=roll<a.crit;val raw=(a.attack*power/100f*(if(critical)1.5f else 1f)*100/(100+d.defense)).toInt().coerceAtLeast(1);val absorbed=minOf(d.shield,raw);return source.update(targetId){it.copy(hp=(it.hp-(raw-absorbed)).coerceAtLeast(0),shield=(it.shield-absorbed).coerceAtLeast(0))}}
    companion object{
        fun initial(mission:Mission,heroes:List<Hero>,artifacts:List<Artifact>):ActionBattleState{val completed=artifacts.filter{it.isCompleted}.associateBy{it.id};val heroActors=heroes.mapIndexed{i,h->var hp=h.maxHp;var atk=h.attack;var def=h.defense;var speed=h.speed;var crit=h.criticalChance;completed[h.equippedArtifactId]?.let{when(it.stat){Stat.HP->hp+=it.combatBonus;Stat.ATTACK->atk+=it.combatBonus;Stat.DEFENSE->def+=it.combatBonus;Stat.SPEED->speed+=it.combatBonus;Stat.CRIT->crit+=it.combatBonus/100.0}};Actor("hero_${h.id}",h.id,h.nameKey,BattleSide.HERO,Vec2(.18f,.3f+i*.18f),hp,hp,atk,def,speed,h.attackRange,crit,h.equippedArtifactId)};val enemies=mission.enemyIds.mapIndexed{i,id->val e=GameContent.enemies.getValue(id);Actor("enemy_${id}_$i",id,e.nameKey,BattleSide.ENEMY,Vec2(.75f,.25f+(i%4)*.17f),e.hp,e.hp,e.attack,e.defense,e.speed,e.attackRange,.06,boss=e.boss)};return ActionBattleState(mission.id,heroActors+enemies,heroActors.first().id)}
    }
}
private fun Vec2.bounded()=Vec2(x.coerceIn(.05f,.95f),y.coerceIn(.1f,.9f))
private fun List<Actor>.replace(actor:Actor)=map{if(it.id==actor.id)actor else it}
private fun List<Actor>.update(id:String,block:(Actor)->Actor)=map{if(it.id==id)block(it)else it}
