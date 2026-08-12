package com.jolttime.game.ui.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jolttime.game.ui.theme.JoltPalette

fun interface HeroVisualAsset { fun DrawScope.draw(center: Offset, scale: Float) }
fun interface EnemyVisualAsset { fun DrawScope.draw(center: Offset, scale: Float, boss: Boolean) }
fun interface EnvironmentVisualAsset { fun DrawScope.drawEnvironment() }
fun interface ArtifactVisualAsset { fun DrawScope.draw(center: Offset, scale: Float, discovered: Boolean) }

enum class EnvironmentScene { ARCHIVE, NILE, MEMPHIS, DESERT, TEMPLE, NECROPOLIS, PYRAMID }

@Composable
fun EnvironmentVisual(scene: EnvironmentScene, modifier: Modifier = Modifier) {
    val palette = JoltPalette
    Canvas(modifier.fillMaxSize()) {
        when (scene) {
            EnvironmentScene.ARCHIVE -> drawArchive(palette.background, palette.surfaceElevated, palette.goldAccent, palette.temporalBlue)
            else -> drawEgypt(scene, palette.sand, palette.nile, palette.goldAccent)
        }
    }
}

@Composable
fun HeroVisual(heroId: String, modifier: Modifier = Modifier, facingRight: Boolean = true) {
    Canvas(modifier) {
        val scale = size.minDimension / 220f
        withTransform({ if (!facingRight) { scale(-1f, 1f, pivot = center) } }) {
            heroAsset(heroId).run { draw(center, scale) }
        }
    }
}

fun heroAsset(id: String): HeroVisualAsset = HeroVisualAsset { center, s ->
    val palette = when (id) {
        "orian" -> listOf(Color(0xFF234C78), Color(0xFFE9B94E), Color(0xFF74D5EA))
        "nefer" -> listOf(Color(0xFFF1E1B7), Color(0xFFC88935), Color(0xFF24899D))
        "seti" -> listOf(Color(0xFF8D4F2F), Color(0xFFE8B85C), Color(0xFF283C58))
        else -> listOf(Color(0xFF343B55), Color(0xFF62C6C9), Color(0xFFE0AE58))
    }
    drawOval(Color(0x55000000), topLeft = center + Offset(-50f*s, 72f*s), size = androidx.compose.ui.geometry.Size(100f*s, 28f*s))
    drawCircle(Color(0xFFC68E67), 23f*s, center + Offset(0f, -65f*s))
    val body = Path().apply { moveTo(center.x-34*s,center.y-38*s);lineTo(center.x+32*s,center.y-38*s);lineTo(center.x+49*s,center.y+70*s);lineTo(center.x-43*s,center.y+70*s);close() }
    drawPath(body,palette[0]);drawPath(body,palette[1],style=Stroke(5f*s))
    when(id) {
        "orian" -> { drawLine(palette[1],center+Offset(22*s,-25*s),center+Offset(75*s,58*s),9f*s,StrokeCap.Round);drawCircle(palette[2],10f*s,center+Offset(76*s,58*s)) }
        "nefer" -> { drawRoundRect(palette[2],topLeft=center+Offset(-73*s,-24*s),size=androidx.compose.ui.geometry.Size(48*s,88*s),cornerRadius=androidx.compose.ui.geometry.CornerRadius(18*s));drawCircle(palette[1],11*s,center+Offset(-49*s,19*s)) }
        "seti" -> { drawArc(palette[1],-75f,150f,false,topLeft=center+Offset(20*s,-42*s),size=androidx.compose.ui.geometry.Size(70*s,112*s),style=Stroke(7*s));drawLine(palette[1],center+Offset(60*s,-38*s),center+Offset(60*s,68*s),3*s) }
        else -> { drawLine(palette[2],center+Offset(31*s,-27*s),center+Offset(55*s,69*s),8*s,StrokeCap.Round);drawCircle(palette[1],12*s,center+Offset(55*s,69*s)) }
    }
    drawCircle(palette[1], 6*s, center + Offset(0f,-65*s))
}

fun enemyAsset(id: String): EnemyVisualAsset = EnemyVisualAsset { c,s,boss ->
    drawOval(Color(0x55000000), topLeft=c+Offset(-52*s,33*s), size=androidx.compose.ui.geometry.Size(104*s,24*s))
    if (boss) {
        val serpent=Path().apply{moveTo(c.x-65*s,c.y+48*s);cubicTo(c.x-20*s,c.y-90*s,c.x+70*s,c.y-75*s,c.x+55*s,c.y+52*s);close()}
        drawPath(serpent,Color(0xFF2B1730));drawPath(serpent,Color(0xFFE5574D),style=Stroke(6*s));
        repeat(5){i->drawCircle(Color(0xFFFFA33C),4*s,c+Offset((-32+i*16)*s,(-25+i%2*13)*s))}
    } else {
        val color=when { id.contains("anomaly")->Color(0xFF46B9C7);id.contains("guard")->Color(0xFF8F5A3D);id.contains("beast")->Color(0xFF823E3E);else->Color(0xFF73506F) }
        drawCircle(color,30*s,c+Offset(0f,-8*s));drawPath(Path().apply{moveTo(c.x-31*s,c.y+5*s);lineTo(c.x,c.y-52*s);lineTo(c.x+31*s,c.y+5*s);close()},color.copy(alpha=.8f));drawCircle(Color(0xFFFFC45B),5*s,c+Offset(-9*s,-12*s));drawCircle(Color(0xFFFFC45B),5*s,c+Offset(9*s,-12*s))
    }
}

fun artifactAsset(id: String): ArtifactVisualAsset = ArtifactVisualAsset { c,s,discovered ->
    val color=if(discovered)Color(0xFFE7B956) else Color(0xFF3B3F48)
    when(id){
        "ankh"->{drawCircle(color,24*s,c+Offset(0f,-28*s),style=Stroke(9*s));drawLine(color,c+Offset(0f,-4*s),c+Offset(0f,60*s),10*s);drawLine(color,c+Offset(-31*s,20*s),c+Offset(31*s,20*s),9*s)}
        "scarab"->{drawOval(color, topLeft=c+Offset(-42*s,-55*s), size=androidx.compose.ui.geometry.Size(84*s,110*s));drawLine(Color(0xFF171920),c+Offset(0f,-44*s),c+Offset(0f,44*s),5*s);drawCircle(Color(0xFF1B8394),9*s,c)}
        else->{val p=Path().apply{moveTo(c.x,c.y-62*s);lineTo(c.x+46*s,c.y);lineTo(c.x,c.y+62*s);lineTo(c.x-46*s,c.y);close()};drawPath(p,color);drawCircle(Color(0xFF24899D),12*s,c)}
    }
}

private fun DrawScope.drawArchive(bg:Color,surface:Color,gold:Color,blue:Color){
    drawRect(Brush.verticalGradient(listOf(Color(0xFF080A10),bg,Color(0xFF17131A))))
    repeat(7){i->val x=size.width*i/6f;drawRect(surface.copy(alpha=.72f),Offset(x-26f,size.height*.15f),androidx.compose.ui.geometry.Size(52f,size.height*.72f));drawLine(gold.copy(alpha=.28f),Offset(x,size.height*.17f),Offset(x,size.height*.82f),3f)}
    drawOval(Color(0xCC080A0E), topLeft=Offset(size.width*.19f,size.height*.79f), size=androidx.compose.ui.geometry.Size(size.width*.62f,size.height*.2f))
    repeat(4){i->drawCircle(if(i%2==0)blue.copy(alpha=.38f)else gold.copy(alpha=.34f),size.minDimension*(.12f+i*.045f),Offset(size.width*.5f,size.height*.47f),style=Stroke(3f+i))}
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(.85f),blue.copy(.85f),Color.Transparent)),size.minDimension*.13f,Offset(size.width*.5f,size.height*.47f))
    repeat(18){i->val angle=i*20f*Math.PI/180;drawCircle(gold.copy(alpha=.55f),2.5f,Offset(size.width*.5f+kotlin.math.cos(angle).toFloat()*size.width*.27f,size.height*.47f+kotlin.math.sin(angle).toFloat()*size.height*.32f))}
}

private fun DrawScope.drawEgypt(scene:EnvironmentScene,sand:Color,nile:Color,gold:Color){
    drawRect(Brush.verticalGradient(listOf(Color(0xFF4B6A81),Color(0xFFF09A4A),Color(0xFFD7954C))))
    drawCircle(Color(0xFFFFDA78),size.height*.13f,Offset(size.width*.76f,size.height*.2f))
    drawPath(Path().apply{moveTo(0f,size.height*.62f);quadraticBezierTo(size.width*.25f,size.height*.48f,size.width*.5f,size.height*.65f);quadraticBezierTo(size.width*.75f,size.height*.78f,size.width,size.height*.57f);lineTo(size.width,size.height);lineTo(0f,size.height);close()},sand)
    if(scene==EnvironmentScene.NILE||scene==EnvironmentScene.MEMPHIS)drawPath(Path().apply{moveTo(0f,size.height*.74f);cubicTo(size.width*.28f,size.height*.62f,size.width*.55f,size.height*.94f,size.width,size.height*.72f);lineTo(size.width,size.height);lineTo(0f,size.height);close()},nile)
    if(scene==EnvironmentScene.PYRAMID||scene==EnvironmentScene.DESERT)repeat(3){i->val x=size.width*(.2f+i*.24f);val base=size.height*(.72f+i*.04f);drawPath(Path().apply{moveTo(x,base-size.height*(.28f-i*.05f));lineTo(x-size.width*.12f,base);lineTo(x+size.width*.12f,base);close()},Color(0xFFB87539));drawLine(gold.copy(.5f),Offset(x,base-size.height*(.28f-i*.05f)),Offset(x+size.width*.12f,base),3f)}
    if(scene==EnvironmentScene.TEMPLE||scene==EnvironmentScene.NECROPOLIS||scene==EnvironmentScene.MEMPHIS)repeat(5){i->val x=size.width*(.18f+i*.16f);drawRect(Color(0xFF8E6744),Offset(x,size.height*.31f),androidx.compose.ui.geometry.Size(size.width*.055f,size.height*.42f));drawRect(gold.copy(.55f),Offset(x-8f,size.height*.28f),androidx.compose.ui.geometry.Size(size.width*.07f,size.height*.05f))}
}
