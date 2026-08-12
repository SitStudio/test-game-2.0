package com.jolttime.game.domain.model

object GameContent {
    private fun basic(id:String,power:Int,range:Float)=Ability(id,"ability_$id",AbilityKind.DAMAGE,power,.65f,range=range)
    val heroes=listOf(
        Hero("orian","hero_orian","hero_orian_story","hero_orian_identity",HeroRole.FIGHTER,Rarity.RARE,baseHp=190,baseAttack=31,baseDefense=14,baseSpeed=74,criticalChance=.14,attackRange=.13f,basic=basic("keeper_blade",100,.13f),skill=Ability("second_step","ability_second_step",AbilityKind.DASH,135,5f,range=.28f),ultimate=Ability("time_break","ability_time_break",AbilityKind.AREA_DAMAGE,225,1f,100,radius=.20f,durationSeconds=1.5f),passiveKey="passive_orian"),
        Hero("nefer","hero_nefer","hero_nefer_story","hero_nefer_identity",HeroRole.TANK,Rarity.EPIC,baseHp=285,baseAttack=22,baseDefense=25,baseSpeed=58,criticalChance=.07,attackRange=.12f,basic=basic("sun_spear",90,.12f),skill=Ability("bastion","ability_bastion",AbilityKind.SHIELD,95,7f,range=.2f,durationSeconds=4f),ultimate=Ability("guardian_roar","ability_guardian_roar",AbilityKind.FREEZE,120,1f,100,radius=.22f,durationSeconds=2f),passiveKey="passive_nefer"),
        Hero("seti","hero_seti","hero_seti_story","hero_seti_identity",HeroRole.RANGED,Rarity.RARE,baseHp=140,baseAttack=37,baseDefense=8,baseSpeed=80,criticalChance=.22,attackRange=.34f,basic=basic("sand_arrow",105,.34f),skill=Ability("hunter_mark","ability_hunter_mark",AbilityKind.DAMAGE,180,5.5f,range=.40f),ultimate=Ability("arrow_storm","ability_arrow_storm",AbilityKind.AREA_DAMAGE,245,1f,100,range=.42f,radius=.18f),passiveKey="passive_seti"),
        Hero("nadiya","hero_nadiya","hero_nadiya_story","hero_nadiya_identity",HeroRole.SUPPORT,Rarity.EPIC,baseHp=155,baseAttack=21,baseDefense=11,baseSpeed=68,criticalChance=.09,attackRange=.28f,basic=basic("chronal_pulse",80,.28f),skill=Ability("rewind_wound","ability_rewind_wound",AbilityKind.HEAL,105,6f,range=.32f),ultimate=Ability("golden_hour","ability_golden_hour",AbilityKind.SLOW,160,1f,100,radius=.30f,durationSeconds=4f),passiveKey="passive_nadiya")
    )
    val enemies=listOf(
        EnemyTemplate("raider","enemy_raider",105,19,6,65,.10f,1.2f),EnemyTemplate("guard","enemy_guard",150,21,15,48,.11f,1.4f),
        EnemyTemplate("sand_anomaly","enemy_sand_anomaly",90,25,4,82,.20f,1.5f),EnemyTemplate("fractured_beast","enemy_fractured_beast",185,29,9,72,.12f,1.1f),
        EnemyTemplate("tomb_echo","enemy_tomb_echo",125,24,8,60,.26f,1.7f),EnemyTemplate("elite_guardian","enemy_elite_guardian",360,34,21,55,.14f,1.05f,elite=true),
        EnemyTemplate("chronal_priest","enemy_chronal_priest",280,38,13,62,.30f,1.45f,elite=true),EnemyTemplate("apophis_echo","enemy_apophis_echo",1050,43,18,58,.20f,1.15f,boss=true)
    ).associateBy{it.id}
    val artifacts=listOf(
        Artifact("ankh","artifact_ankh","artifact_ankh_desc","artifact_ankh_effect",rarity=Rarity.RARE,fragmentsRequired=2,stat=Stat.HP,combatBonus=18,museumBonusPercent=2),
        Artifact("palette","artifact_palette","artifact_palette_desc","artifact_palette_effect",rarity=Rarity.COMMON,fragmentsRequired=2,stat=Stat.ATTACK,combatBonus=7,museumBonusPercent=1),
        Artifact("scarab","artifact_scarab","artifact_scarab_desc","artifact_scarab_effect",rarity=Rarity.EPIC,fragmentsRequired=3,stat=Stat.DEFENSE,combatBonus=9,museumBonusPercent=3),
        Artifact("cartouche","artifact_cartouche","artifact_cartouche_desc","artifact_cartouche_effect",rarity=Rarity.LEGENDARY,fragmentsRequired=3,stat=Stat.SPEED,combatBonus=6,museumBonusPercent=4),
        Artifact("fractured_eye","artifact_eye","artifact_eye_desc","artifact_eye_effect",rarity=Rarity.MYTHIC,fragmentsRequired=1,stat=Stat.CRIT,combatBonus=10,museumBonusPercent=5))
    val missions=listOf(
        Mission("portal","mission_portal","location_portal",MissionNodeType.STORY,MissionType.SURVIVE,listOf("raider","sand_anomaly"),45,70,20,storyAfter="arrival"),
        Mission("nile","mission_nile","location_nile",MissionNodeType.BATTLE,MissionType.DEFEAT_ALL,listOf("raider","guard","tomb_echo"),rewardXp=90,rewardMaterial=30,artifactId="palette",artifactFragments=2),
        Mission("memphis","mission_memphis","location_memphis",MissionNodeType.EVENT,MissionType.DESTROY_ANOMALY,listOf("sand_anomaly","sand_anomaly","guard"),rewardXp=110,rewardMaterial=40,artifactId="ankh",artifactFragments=2,storyAfter="symbol"),
        Mission("desert","mission_desert","location_desert",MissionNodeType.BATTLE,MissionType.DEFEAT_ALL,listOf("fractured_beast","raider","raider"),rewardXp=125,rewardMaterial=45),
        Mission("guardian","mission_guardian","location_necropolis",MissionNodeType.ELITE,MissionType.DEFEAT_ALL,listOf("elite_guardian","tomb_echo"),rewardXp=160,rewardMaterial=65,artifactId="scarab",artifactFragments=3),
        Mission("temple","mission_temple","location_temple",MissionNodeType.ARTIFACT,MissionType.SURVIVE,listOf("chronal_priest","sand_anomaly"),60,180,75,artifactId="cartouche",artifactFragments=3,storyBefore="temple"),
        Mission("pyramid_gate","mission_pyramid_gate","location_pyramid_gate",MissionNodeType.BATTLE,MissionType.DESTROY_ANOMALY,listOf("guard","fractured_beast","chronal_priest"),rewardXp=210,rewardMaterial=90),
        Mission("pyramid","mission_pyramid","location_pyramid",MissionNodeType.BOSS,MissionType.BOSS,listOf("apophis_echo"),rewardXp=320,rewardMaterial=130,artifactId="fractured_eye",artifactFragments=1,storyBefore="boss",storyAfter="ending"))
    val stories=listOf(
        StoryScene("intro",listOf(DialogueLine("speaker_archive","story_intro_1"),DialogueLine("speaker_nadiya","story_intro_2"),DialogueLine("speaker_archive","story_intro_3"))),
        StoryScene("arrival",listOf(DialogueLine("speaker_nadiya","story_arrival_1"),DialogueLine("speaker_orian","story_arrival_2"))),
        StoryScene("symbol",listOf(DialogueLine("speaker_nadiya","story_symbol_1"))),StoryScene("temple",listOf(DialogueLine("speaker_nefer","story_temple_1"))),
        StoryScene("boss",listOf(DialogueLine("speaker_archive","story_boss_1"),DialogueLine("speaker_orian","story_boss_2"))),
        StoryScene("ending",listOf(DialogueLine("speaker_nadiya","story_ending_1"),DialogueLine("speaker_archive","story_ending_2")))).associateBy{it.id}
}
