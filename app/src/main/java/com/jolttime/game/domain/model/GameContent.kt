package com.jolttime.game.domain.model

object GameContent {
    val epochs = listOf(
        Epoch("egypt", "Ancient Egypt", "Monuments, dynasties and the eternal Nile.", 1, 0xFFD5AE62),
        Epoch("greece", "Ancient Greece", "Philosophy, city-states and heroic myth.", 5, 0xFF7EA7C9),
        Epoch("rome", "Ancient Rome", "An empire of roads, law and engineering.", 10, 0xFFB96F62),
        Epoch("vikings", "Vikings", "Northern navigators, traders and storytellers.", 15, 0xFF719B91),
        Epoch("japan", "Ancient Japan", "Courtly craft, warriors and sacred tradition.", 20, 0xFFC78391)
    )
    val upgrades = listOf(
        Upgrade("tap", "Stronger Tap", "Focus more energy through the Time Core.", maxLevel=50, baseCost=50, costMultiplier=1.45, effectType=EffectType.TAP_POWER, effectValue=1.0),
        Upgrade("engine", "Time Engine", "Generate shards while time flows.", maxLevel=40, baseCost=100, costMultiplier=1.55, effectType=EffectType.PASSIVE_INCOME, effectValue=1.0),
        Upgrade("tools", "Archaeologist Tools", "Improve fragment discovery odds.", maxLevel=20, baseCost=180, costMultiplier=1.7, effectType=EffectType.DISCOVERY_CHANCE, effectValue=.015),
        Upgrade("wing", "Museum Wing", "Increase the prestige of every display.", maxLevel=15, baseCost=300, costMultiplier=1.8, effectType=EffectType.MUSEUM_BONUS, effectValue=.05)
    )
    private val names = mapOf(
        "egypt" to listOf("Scarab Seal", "Canopic Jar", "Ankh of Dawn", "Royal Cartouche", "Mask of Eternity"),
        "greece" to listOf("Attic Amphora", "Laurel Crown", "Hoplite Crest", "Oracle Tablet", "Aegis Fragment"),
        "rome" to listOf("Legion Denarius", "Gladius Hilt", "Senate Seal", "Aqueduct Plan", "Imperial Eagle"),
        "vikings" to listOf("Runestone Chip", "Silver Brooch", "Longship Keel", "Seer's Staff", "Odin Pendant"),
        "japan" to listOf("Haniwa Figure", "Bronze Mirror", "Court Fan", "Sacred Magatama", "Dawn Blade")
    )
    private val rarities = Rarity.entries
    val artifacts = epochs.flatMap { epoch -> names.getValue(epoch.id).mapIndexed { i, name ->
        Artifact("${epoch.id}_$i", name, epoch.id, rarities[i], "A preserved trace of ${epoch.name} awaiting restoration.", fragmentsRequired=3+i*2, bonusType=if(i%2==0) BonusType.TAP_POWER else BonusType.PASSIVE_INCOME, bonusValue=(i+1).toDouble())
    }}
}
