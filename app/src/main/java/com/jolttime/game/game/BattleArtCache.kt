package com.jolttime.game.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.jolttime.game.ui.visual.ArtCatalog
import com.jolttime.game.ui.visual.EnvironmentScene

/** Rasterizes Android drawable assets once; the 60 FPS battle loop only blits cached bitmaps. */
class BattleArtCache(private val context: Context) {
    private data class Key(@DrawableRes val resource: Int, val width: Int, val height: Int)
    private val bitmaps = HashMap<Key, Bitmap>()

    fun hero(id: String) = bitmap(ArtCatalog.hero(id).fullBody, 256, 256)
    fun enemy(id: String, boss: Boolean) = bitmap(
        if (boss) ArtCatalog.boss.sprite else ArtCatalog.enemy(id).sprite,
        if (boss) 384 else 256,
        if (boss) 384 else 256,
    )
    fun environment() = bitmap(ArtCatalog.environment(EnvironmentScene.NILE).background, 960, 540)

    private fun bitmap(@DrawableRes resource: Int, width: Int, height: Int): Bitmap =
        bitmaps.getOrPut(Key(resource, width, height)) {
            val drawable = requireNotNull(ContextCompat.getDrawable(context, resource))
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { target ->
                drawable.setBounds(0, 0, width, height)
                drawable.draw(Canvas(target))
            }
        }
}
