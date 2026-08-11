package com.jolttime.game.domain.model

object GameContent {
    private fun hit(id: String, power: Int) = Ability(id, "ability_$id", AbilityKind.DAMAGE, power)
    val heroes = listOf(
        Hero("kael", "hero_kael", "hero_kael_story", HeroRole.VANGUARD, baseHp=180, baseAttack=24, baseDefense=18, baseSpeed=9, criticalChance=.08,
            basic=hit("shield_bash", 95), skill=Ability("aegis", "ability_aegis", AbilityKind.SHIELD, 45, 25, StatusType.SHIELD, 2), ultimate=Ability("last_hour", "ability_last_hour", AbilityKind.STUN, 130, 80, StatusType.STUN, 1), passiveKey="passive_kael"),
        Hero("mira", "hero_mira", "hero_mira_story", HeroRole.STRIKER, baseHp=115, baseAttack=39, baseDefense=8, baseSpeed=15, criticalChance=.22,
            basic=hit("twin_cut", 105), skill=Ability("bleeding_arc", "ability_bleeding_arc", AbilityKind.DAMAGE, 145, 30, StatusType.BLEED, 2), ultimate=Ability("fracture_strike", "ability_fracture_strike", AbilityKind.DAMAGE, 260, 80), passiveKey="passive_mira"),
        Hero("sahir", "hero_sahir", "hero_sahir_story", HeroRole.SCHOLAR, baseHp=125, baseAttack=28, baseDefense=10, baseSpeed=12, criticalChance=.10,
            basic=hit("chronal_bolt", 90), skill=Ability("time_lock", "ability_time_lock", AbilityKind.STUN, 75, 35, StatusType.STUN, 1), ultimate=Ability("stolen_second", "ability_stolen_second", AbilityKind.SLOW, 165, 80, StatusType.TIME_SLOW, 2), passiveKey="passive_sahir"),
        Hero("nadiya", "hero_nadiya", "hero_nadiya_story", HeroRole.SUPPORT, baseHp=140, baseAttack=21, baseDefense=12, baseSpeed=11, criticalChance=.08,
            basic=hit("staff_strike", 80), skill=Ability("rewind_wound", "ability_rewind_wound", AbilityKind.HEAL, 85, 30), ultimate=Ability("golden_hour", "ability_golden_hour", AbilityKind.HEAL, 170, 80), passiveKey="passive_nadiya"),
    )

    val enemies = listOf(
        enemy("raider", 105, 24, 7, 12), enemy("guard", 145, 20, 15, 8),
        enemy("sand_anomaly", 90, 29, 5, 16, Ability("sand_burn", "enemy_sand_burn", AbilityKind.BURN, 90, status=StatusType.BURN, statusTurns=2)),
        enemy("fractured_beast", 175, 31, 9, 10), enemy("tomb_echo", 120, 26, 10, 13, Ability("weakening_cry", "enemy_weakening_cry", AbilityKind.DAMAGE, 80, status=StatusType.WEAKNESS, statusTurns=2)),
        enemy("elite_guardian", 290, 36, 20, 11, elite=true), enemy("chronal_priest", 240, 42, 14, 14, elite=true, special=Ability("time_slow", "enemy_time_slow", AbilityKind.SLOW, 100, status=StatusType.TIME_SLOW, statusTurns=2)),
        EnemyTemplate("apophis_echo", "enemy_apophis_echo", 780, 47, 18, 13, .12,
            listOf(hit("void_bite", 110), Ability("eclipse", "enemy_eclipse", AbilityKind.BURN, 95, status=StatusType.BURN, statusTurns=2), Ability("fracture_roar", "enemy_fracture_roar", AbilityKind.STUN, 80, status=StatusType.STUN, statusTurns=1)), boss=true),
    ).associateBy { it.id }

    private fun enemy(id:String,hp:Int,attack:Int,defense:Int,speed:Int,special:Ability?=null,elite:Boolean=false) = EnemyTemplate(
        id, "enemy_$id", hp, attack, defense, speed, abilities=listOfNotNull(hit("enemy_strike",100),special), elite=elite)

    val artifacts = listOf(
        Artifact("ankh", "artifact_ankh", "artifact_ankh_desc", rarity=Rarity.RARE, fragmentsRequired=2, stat=Stat.HP, combatBonus=12, museumBonusPercent=2),
        Artifact("scribe_palette", "artifact_palette", "artifact_palette_desc", rarity=Rarity.COMMON, fragmentsRequired=2, stat=Stat.ATTACK, combatBonus=6, museumBonusPercent=1),
        Artifact("scarab", "artifact_scarab", "artifact_scarab_desc", rarity=Rarity.EPIC, fragmentsRequired=3, stat=Stat.DEFENSE, combatBonus=8, museumBonusPercent=3),
        Artifact("cartouche", "artifact_cartouche", "artifact_cartouche_desc", rarity=Rarity.LEGENDARY, fragmentsRequired=3, stat=Stat.SPEED, combatBonus=4, museumBonusPercent=4),
        Artifact("fractured_eye", "artifact_eye", "artifact_eye_desc", rarity=Rarity.MYTHIC, fragmentsRequired=1, stat=Stat.CRIT, combatBonus=10, museumBonusPercent=5),
    )

    val missions = listOf(
        Mission("portal", "mission_portal", "location_portal", MissionNodeType.STORY, MissionType.SURVIVE_TURNS, listOf("raider", "sand_anomaly"), 3, 80, 25, storyAfter="arrival"),
        Mission("nile", "mission_nile", "location_nile", MissionNodeType.BATTLE, MissionType.DEFEAT_ALL, listOf("raider", "guard", "tomb_echo"), rewardXp=100, rewardMaterial=35, artifactId="scribe_palette", artifactFragments=2),
        Mission("memphis", "mission_memphis", "location_memphis", MissionNodeType.BATTLE, MissionType.DEFEAT_ALL, listOf("guard", "sand_anomaly", "fractured_beast"), rewardXp=120, rewardMaterial=45, artifactId="ankh", artifactFragments=2, storyAfter="symbol"),
        Mission("desert", "mission_desert", "location_desert", MissionNodeType.ELITE, MissionType.DEFEAT_ALL, listOf("elite_guardian", "raider"), rewardXp=150, rewardMaterial=60, artifactId="scarab", artifactFragments=3),
        Mission("temple", "mission_temple", "location_temple", MissionNodeType.BATTLE, MissionType.SURVIVE_TURNS, listOf("chronal_priest", "sand_anomaly"), surviveTurns=5, rewardXp=175, rewardMaterial=70, artifactId="cartouche", artifactFragments=3, storyBefore="temple"),
        Mission("pyramid", "mission_pyramid", "location_pyramid", MissionNodeType.BOSS, MissionType.BOSS, listOf("apophis_echo"), rewardXp=300, rewardMaterial=120, artifactId="fractured_eye", artifactFragments=1, storyBefore="boss", storyAfter="ending"),
    )

    val stories = listOf(
        StoryScene("intro", listOf(DialogueLine("speaker_archive", "story_intro_1"), DialogueLine("speaker_nadiya", "story_intro_2"), DialogueLine("speaker_archive", "story_intro_3"))),
        StoryScene("arrival", listOf(DialogueLine("speaker_sahir", "story_arrival_1"), DialogueLine("speaker_mira", "story_arrival_2"))),
        StoryScene("symbol", listOf(DialogueLine("speaker_sahir", "story_symbol_1"))),
        StoryScene("temple", listOf(DialogueLine("speaker_nadiya", "story_temple_1"))),
        StoryScene("boss", listOf(DialogueLine("speaker_archive", "story_boss_1"), DialogueLine("speaker_kael", "story_boss_2"))),
        StoryScene("ending", listOf(DialogueLine("speaker_sahir", "story_ending_1"), DialogueLine("speaker_archive", "story_ending_2"))),
    ).associateBy { it.id }
}
