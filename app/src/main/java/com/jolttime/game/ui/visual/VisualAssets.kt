package com.jolttime.game.ui.visual

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.jolttime.game.R
import com.jolttime.game.domain.model.MissionNodeType

/** Stable resource contract between gameplay IDs and replaceable production artwork. */
data class HeroArtAsset(@DrawableRes val fullBody: Int, @DrawableRes val portrait: Int = fullBody)
data class EnemyArtAsset(@DrawableRes val sprite: Int, @DrawableRes val portrait: Int = sprite)
data class BossArtAsset(@DrawableRes val sprite: Int, @DrawableRes val portrait: Int = sprite)
data class ArtifactArtAsset(@DrawableRes val display: Int)
data class EnvironmentArtAsset(@DrawableRes val background: Int)
data class UiDecorationAsset(@DrawableRes val drawable: Int)

enum class EnvironmentScene { ARCHIVE, NILE, MEMPHIS, DESERT, TEMPLE, NECROPOLIS, PYRAMID }

object ArtCatalog {
    private val heroes = mapOf(
        "orian" to HeroArtAsset(R.drawable.hero_orian),
        "nefer" to HeroArtAsset(R.drawable.hero_nefer),
        "seti" to HeroArtAsset(R.drawable.hero_seti),
        "nadiya" to HeroArtAsset(R.drawable.hero_nadiya),
    )
    private val enemies = mapOf(
        "raider" to EnemyArtAsset(R.drawable.enemy_raider),
        "guard" to EnemyArtAsset(R.drawable.enemy_guard),
        "elite_guardian" to EnemyArtAsset(R.drawable.enemy_guard),
        "sand_anomaly" to EnemyArtAsset(R.drawable.enemy_sand_anomaly),
        "fractured_beast" to EnemyArtAsset(R.drawable.enemy_fractured_beast),
        "tomb_echo" to EnemyArtAsset(R.drawable.enemy_sand_anomaly),
        "chronal_priest" to EnemyArtAsset(R.drawable.enemy_chronal_priest),
    )
    private val artifacts = mapOf(
        "ankh" to ArtifactArtAsset(R.drawable.artifact_ankh),
        "palette" to ArtifactArtAsset(R.drawable.artifact_palette),
        "scarab" to ArtifactArtAsset(R.drawable.artifact_scarab),
        "cartouche" to ArtifactArtAsset(R.drawable.artifact_cartouche),
        "fractured_eye" to ArtifactArtAsset(R.drawable.artifact_fractured_eye),
    )
    private val environments = mapOf(
        EnvironmentScene.ARCHIVE to EnvironmentArtAsset(R.drawable.bg_archive),
        EnvironmentScene.NILE to EnvironmentArtAsset(R.drawable.bg_egypt_nile),
        EnvironmentScene.MEMPHIS to EnvironmentArtAsset(R.drawable.bg_egypt_nile),
        EnvironmentScene.DESERT to EnvironmentArtAsset(R.drawable.bg_egypt_map),
        EnvironmentScene.TEMPLE to EnvironmentArtAsset(R.drawable.bg_egypt_nile),
        EnvironmentScene.NECROPOLIS to EnvironmentArtAsset(R.drawable.bg_egypt_map),
        EnvironmentScene.PYRAMID to EnvironmentArtAsset(R.drawable.bg_egypt_map),
    )
    val boss = BossArtAsset(R.drawable.boss_apophis_echo)

    fun hero(id: String) = heroes[id] ?: HeroArtAsset(R.drawable.hero_orian)
    fun enemy(id: String) = enemies[id] ?: EnemyArtAsset(R.drawable.enemy_raider)
    fun artifact(id: String) = artifacts[id] ?: ArtifactArtAsset(R.drawable.artifact_palette)
    fun environment(scene: EnvironmentScene) = environments.getValue(scene)
    fun node(type: MissionNodeType) = UiDecorationAsset(when (type) {
        MissionNodeType.STORY -> R.drawable.node_story
        MissionNodeType.BATTLE -> R.drawable.node_battle
        MissionNodeType.ELITE -> R.drawable.node_elite
        MissionNodeType.ARTIFACT -> R.drawable.node_artifact
        MissionNodeType.EVENT -> R.drawable.node_event
        MissionNodeType.BOSS -> R.drawable.node_boss
    })
}

@Composable
fun EnvironmentVisual(scene: EnvironmentScene, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(ArtCatalog.environment(scene).background),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun HeroVisual(
    heroId: String,
    modifier: Modifier = Modifier,
    facingRight: Boolean = true,
    portrait: Boolean = false,
) {
    val asset = ArtCatalog.hero(heroId)
    Image(
        painter = painterResource(if (portrait) asset.portrait else asset.fullBody),
        contentDescription = null,
        modifier = modifier.graphicsLayer { scaleX = if (facingRight) 1f else -1f },
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun ArtifactVisual(id: String, discovered: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(ArtCatalog.artifact(id).display),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = if (discovered) null else ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFF30343C)),
    )
}

@Composable
fun MissionNodeVisual(type: MissionNodeType, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(ArtCatalog.node(type).drawable),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
