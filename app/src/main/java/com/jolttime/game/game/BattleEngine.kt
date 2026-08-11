package com.jolttime.game.game

import com.jolttime.game.domain.model.*
import kotlin.math.max

object BattleEngine {
    fun create(mission: Mission, heroes: List<Hero>, artifacts: List<Artifact>): BattleState {
        val artifactMap = artifacts.filter { it.isCompleted }.associateBy { it.id }
        val heroUnits = heroes.map { hero ->
            var hp = hero.maxHp; var attack = hero.attack; var defense = hero.defense; var speed = hero.speed; var crit = hero.criticalChance
            artifactMap[hero.equippedArtifactId]?.let { artifact -> when (artifact.stat) {
                Stat.HP -> hp += artifact.combatBonus; Stat.ATTACK -> attack += artifact.combatBonus
                Stat.DEFENSE -> defense += artifact.combatBonus; Stat.SPEED -> speed += artifact.combatBonus
                Stat.CRIT -> crit += artifact.combatBonus / 100.0
            } }
            Combatant("hero_${hero.id}", hero.id, hero.nameKey, BattleSide.HERO, hp, hp, attack, defense, speed, crit, abilities=listOf(hero.basic, hero.skill, hero.ultimate))
        }
        val enemyUnits = mission.enemyIds.mapIndexed { index, id ->
            val e = GameContent.enemies.getValue(id)
            Combatant("enemy_${id}_$index", id, e.nameKey, BattleSide.ENEMY, e.hp, e.hp, e.attack, e.defense, e.speed, e.criticalChance, abilities=e.abilities, boss=e.boss)
        }
        val all = heroUnits + enemyUnits
        return BattleState(mission.id, mission.objective, mission.surviveTurns, all, turnOrder(all))
    }

    fun turnOrder(units: List<Combatant>): List<String> = units.filter { it.alive }.sortedWith(compareByDescending<Combatant> { effectiveSpeed(it) }.thenBy { it.instanceId }).map { it.instanceId }

    fun damage(attacker: Combatant, defender: Combatant, power: Int, roll: Double): Pair<Int, Boolean> {
        val critical = roll < attacker.criticalChance.coerceIn(0.0, 1.0)
        val raw = attacker.attack * power / 100.0 * if (critical) 1.5 else 1.0
        val weakness = if (attacker.statuses.any { it.type == StatusType.WEAKNESS }) .75 else 1.0
        val amount = max(1, (raw * weakness * 100.0 / (100 + defender.defense.coerceAtLeast(0))).toInt())
        return amount to critical
    }

    fun act(state: BattleState, abilityIndex: Int, targetId: String, roll: Double = .99): BattleState {
        if (state.result != BattleResult.IN_PROGRESS) return state
        val actor = state.active ?: return state
        if (!actor.alive || actor.statuses.any { it.type == StatusType.STUN }) return advance(state.copy(logKey="battle_stunned"))
        val ability = actor.abilities.getOrNull(abilityIndex) ?: return state
        if (ability.energyCost > actor.energy) return state
        val target = state.combatants.firstOrNull { it.instanceId == targetId && it.alive } ?: return state
        val friendlyAbility = ability.kind == AbilityKind.HEAL || ability.kind == AbilityKind.SHIELD
        if (friendlyAbility != (target.side == actor.side)) return state
        var units = state.combatants
        val updatedActor = actor.copy(energy = (actor.energy - ability.energyCost + if (abilityIndex == 0) 25 else 12).coerceIn(0, 100))
        units = units.replace(updatedActor)
        when (ability.kind) {
            AbilityKind.HEAL -> units = units.update(targetId) { it.copy(hp=(it.hp + ability.power).coerceAtMost(it.maxHp)) }
            AbilityKind.SHIELD -> units = units.update(targetId) { it.withStatus(StatusType.SHIELD, ability.statusTurns, ability.power) }
            else -> {
                val liveActor = units.first { it.instanceId == actor.instanceId }
                val (baseDamage, critical) = damage(liveActor, target, ability.power, roll)
                val shield = target.statuses.firstOrNull { it.type == StatusType.SHIELD }?.value ?: 0
                val dealt = (baseDamage - shield).coerceAtLeast(0)
                units = units.update(targetId) {
                    var changed = it.copy(hp=(it.hp - dealt).coerceAtLeast(0), statuses=if(shield > 0) it.statuses.filterNot { s -> s.type == StatusType.SHIELD } else it.statuses)
                    if (ability.status != null && changed.alive) changed = changed.withStatus(ability.status, ability.statusTurns, max(4, actor.attack / 5))
                    changed
                }
                return advance(state.copy(combatants=units, logKey=if(critical) "battle_critical" else "battle_hit"))
            }
        }
        return advance(state.copy(combatants=units, logKey="battle_support"))
    }

    fun enemyAction(state: BattleState): BattleState {
        val actor = state.active ?: return state
        if (actor.side != BattleSide.ENEMY) return state
        val heroes = state.combatants.filter { it.side == BattleSide.HERO && it.alive }
        if (heroes.isEmpty()) return state.copy(result=BattleResult.DEFEAT)
        val ability = if (actor.boss && actor.hp <= actor.maxHp / 2 && actor.abilities.size > 1) 1 else 0
        return act(state, ability, heroes.minBy { it.hp }.instanceId, .9)
    }

    private fun advance(state: BattleState): BattleState {
        var units = state.combatants
        val actingId = state.activeId
        var round = state.round
        var order = state.order.filter { id -> units.any { it.instanceId == id && it.alive } }
        var nextIndex = order.indexOf(actingId).let { if (it < 0) state.activeIndex else it + 1 }
        if (nextIndex >= order.size) {
            round++
            units = units.map { unit ->
                val dot = unit.statuses.filter { it.type == StatusType.BURN || it.type == StatusType.BLEED }.sumOf { it.value }
                unit.copy(hp=(unit.hp-dot).coerceAtLeast(0), statuses=unit.statuses.mapNotNull { if(it.turns<=1)null else it.copy(turns=it.turns-1) })
            }
            order = turnOrder(units); nextIndex = 0
        }
        val heroesAlive = units.any { it.side == BattleSide.HERO && it.alive }
        val enemiesAlive = units.any { it.side == BattleSide.ENEMY && it.alive }
        val result = when {
            !heroesAlive -> BattleResult.DEFEAT
            state.objective == MissionType.SURVIVE_TURNS && round > state.surviveTurns -> BattleResult.VICTORY
            !enemiesAlive -> BattleResult.VICTORY
            else -> BattleResult.IN_PROGRESS
        }
        return state.copy(combatants=units, order=order, activeIndex=nextIndex.coerceAtMost((order.size-1).coerceAtLeast(0)), round=round, result=result)
    }

    private fun effectiveSpeed(unit: Combatant) = if (unit.statuses.any { it.type == StatusType.TIME_SLOW }) unit.speed / 2 else unit.speed
    private fun List<Combatant>.replace(unit: Combatant) = map { if(it.instanceId==unit.instanceId) unit else it }
    private fun List<Combatant>.update(id:String, block:(Combatant)->Combatant) = map { if(it.instanceId==id) block(it) else it }
    private fun Combatant.withStatus(type:StatusType, turns:Int, value:Int=0) = copy(statuses=statuses.filterNot { it.type==type } + TimedStatus(type, turns.coerceAtLeast(1), value))
}
