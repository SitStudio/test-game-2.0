package com.jolttime.game

import com.jolttime.game.domain.model.GameContent
import com.jolttime.game.ui.visual.ArtCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtCatalogTest {
    @Test
    fun everyPlayableHeroHasDrawableBackedArt() {
        GameContent.heroes.forEach { hero ->
            assertTrue(ArtCatalog.hero(hero.id).fullBody != 0)
            assertTrue(ArtCatalog.hero(hero.id).portrait != 0)
        }
    }

    @Test
    fun everyEgyptArtifactHasDisplayArt() {
        GameContent.artifacts.forEach { artifact ->
            assertTrue(ArtCatalog.artifact(artifact.id).display != 0)
        }
    }

    @Test
    fun everyBattleEnemyHasSpriteArt() {
        GameContent.enemies.values.filterNot { it.boss }.forEach { enemy ->
            assertTrue(ArtCatalog.enemy(enemy.id).sprite != 0)
        }
        assertTrue(ArtCatalog.boss.sprite != 0)
    }
}
