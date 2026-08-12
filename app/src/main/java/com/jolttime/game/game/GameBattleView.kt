package com.jolttime.game.game

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.jolttime.game.domain.model.*
import kotlin.math.*

class GameBattleView(
    context: Context,
    mission: Mission,
    heroes: List<Hero>,
    artifacts: List<Artifact>,
    private val labels: Labels,
    private val finished: (Boolean) -> Unit,
) : View(context) {
    data class Labels(
        val attack: String,
        val skill: String,
        val ultimate: String,
        val switch: String,
        val objective: String,
        val victory: String,
        val defeat: String,
        val bossName: String,
        val bossPhase: String,
    )
    private data class FloatText(val text:String,val x:Float,val y:Float,val color:Int,val critical:Boolean,val born:Long)
    private data class Burst(val x:Float,val y:Float,val color:Int,val born:Long)

    private val engine = ActionBattleEngine(mission, heroes, artifacts)
    private val art = BattleArtCache(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND }
    private val path = Path()
    private val rect = RectF()
    private val floats = ArrayList<FloatText>(16)
    private val bursts = ArrayList<Burst>(12)
    private val previousHp = HashMap<String,Int>()
    private var lastFrame = 0L
    private var movement = Vec2(0f,0f)
    private var smoothedMovement = Vec2(0f,0f)
    private var joystickPointer = -1
    private var reported = false
    private var flashUntil = 0L

    init { isFocusable=true;contentDescription=labels.objective;engine.state.actors.forEach{previousHp[it.id]=it.hp} }
    override fun onAttachedToWindow(){super.onAttachedToWindow();lastFrame=System.nanoTime();postOnAnimation(::frame)}
    override fun onDetachedFromWindow(){removeCallbacks(::frame);super.onDetachedFromWindow()}
    private fun frame(){if(!isAttachedToWindow)return;val now=System.nanoTime();val dt=((now-lastFrame)/1_000_000_000f).coerceAtMost(.05f);lastFrame=now;smoothedMovement=Vec2(smoothedMovement.x+(movement.x-smoothedMovement.x)*min(1f,dt*12f),smoothedMovement.y+(movement.y-smoothedMovement.y)*min(1f,dt*12f));val state=engine.step(dt,PlayerInput(smoothedMovement));captureFeedback(state);if(state.result!=BattleResult.IN_PROGRESS&&!reported){reported=true;postDelayed({finished(state.result==BattleResult.VICTORY)},1200)};invalidate();postOnAnimation(::frame)}

    private fun captureFeedback(state:ActionBattleState){val now=System.currentTimeMillis();state.actors.forEach{a->val before=previousHp[a.id]?:a.hp;if(a.hp<before){val amount=before-a.hp;floats.add(FloatText("-$amount",a.position.x,a.position.y,if(a.side==BattleSide.HERO)Color.rgb(255,116,91)else Color.WHITE,amount>35,now));bursts.add(Burst(a.position.x,a.position.y,Color.rgb(255,184,74),now));flashUntil=now+55};previousHp[a.id]=a.hp};floats.removeAll{now-it.born>850};bursts.removeAll{now-it.born>380}}

    override fun onDraw(c:Canvas){super.onDraw(c);val w=width.toFloat();val h=height.toFloat();drawEnvironment(c,w,h);val state=engine.state;drawTelegraph(c,state,w,h);state.actors.filter{it.alive}.sortedBy{it.position.y}.forEach{drawActor(c,it,state.activeHeroId,w,h)};drawEffects(c,w,h);drawBossBar(c,state,w,h);drawHud(c,state,w,h);drawControls(c,state,w,h);if(System.currentTimeMillis()<flashUntil){paint.color=Color.argb(32,255,226,155);c.drawRect(0f,0f,w,h,paint)};if(state.result!=BattleResult.IN_PROGRESS)drawFinish(c,state,w,h)}

    private fun drawEnvironment(c: Canvas, w: Float, h: Float) {
        paint.alpha = 255
        c.drawBitmap(art.environment(), null, RectF(0f, 0f, w, h), paint)
        paint.color = Color.argb(24, 255, 238, 195)
        repeat(14) { index ->
            c.drawCircle(w * ((index * 73) % 100) / 100f, h * ((index * 37) % 80) / 100f + 10f, 2f + index % 3, paint)
        }
    }

    private fun drawActor(c:Canvas,a:Actor,activeId:String,w:Float,h:Float){val x=a.position.x*w;val y=a.position.y*h;val s=if(a.boss)1.45f else 1f;paint.color=Color.argb(80,0,0,0);rect.set(x-34*s,y+24*s,x+34*s,y+43*s);c.drawOval(rect,paint);drawActorAsset(c, a, x, y, s);if(a.id==activeId){stroke.color=Color.rgb(255,208,94);stroke.strokeWidth=4f;stroke.pathEffect=DashPathEffect(floatArrayOf(9f,7f),0f);c.drawCircle(x,y+5,43*s,stroke);stroke.pathEffect=null};drawUnitBar(c,a,x,y-52*s,if(a.boss)100*s else 58*s);if(a.shield>0){stroke.color=Color.rgb(83,215,239);stroke.strokeWidth=4f;c.drawCircle(x,y,39*s,stroke)}}
    private fun drawActorAsset(c: Canvas, actor: Actor, x: Float, y: Float, scale: Float) {
        val bitmap = if (actor.side == BattleSide.HERO) art.hero(actor.templateId) else art.enemy(actor.templateId, actor.boss)
        val halfWidth = (if (actor.boss) 76f else 48f) * scale
        val height = (if (actor.boss) 142f else 94f) * scale
        rect.set(x - halfWidth, y - height * .72f, x + halfWidth, y + height * .28f)
        paint.alpha = if (actor.frozen > 0f) 175 else 255
        c.drawBitmap(bitmap, null, rect, paint)
        paint.alpha = 255
    }

    private fun drawUnitBar(c:Canvas,a:Actor,x:Float,y:Float,width:Float){rect.set(x-width/2,y,x+width/2,y+7);paint.color=Color.argb(180,20,18,18);c.drawRoundRect(rect,4f,4f,paint);rect.right=rect.left+width*(a.hp/a.maxHp.toFloat());paint.color=if(a.side==BattleSide.HERO)Color.rgb(75,207,125)else Color.rgb(231,76,68);c.drawRoundRect(rect,4f,4f,paint)}
    private fun drawBossBar(c:Canvas,s:ActionBattleState,w:Float,h:Float){val boss=s.actors.firstOrNull{it.boss&&it.alive}?:return;val barW=w*.52f;rect.set(w*.24f,h*.045f,w*.76f,h*.075f);paint.color=Color.argb(210,24,12,20);c.drawRoundRect(rect,9f,9f,paint);rect.right=rect.left+barW*(boss.hp/boss.maxHp.toFloat());paint.color=if(s.bossStage==1)Color.rgb(200,58,64)else Color.rgb(239,86,43);c.drawRoundRect(rect,9f,9f,paint);paint.textAlign=Paint.Align.CENTER;paint.textSize=h*.027f;paint.typeface=Typeface.DEFAULT_BOLD;paint.color=Color.WHITE;c.drawText("${labels.bossName}  •  ${labels.bossPhase} ${s.bossStage}",w*.5f,h*.105f,paint)}
    private fun drawTelegraph(c:Canvas,s:ActionBattleState,w:Float,h:Float){val boss=s.actors.firstOrNull{it.boss&&it.alive}?:return;if(s.bossStage==2&&s.elapsed%3.2f>2.35f){val active=s.actors.firstOrNull{it.id==s.activeHeroId}?:return;val alpha=((s.elapsed%3.2f-2.35f)/.85f*120).toInt().coerceIn(30,120);paint.color=Color.argb(alpha,255,70,35);c.drawCircle(active.position.x*w,active.position.y*h,h*.16f,paint);stroke.color=Color.rgb(255,132,48);stroke.strokeWidth=5f;c.drawCircle(active.position.x*w,active.position.y*h,h*.16f,stroke)}}
    private fun drawEffects(c:Canvas,w:Float,h:Float){val now=System.currentTimeMillis();bursts.forEach{b->val t=(now-b.born)/380f;stroke.color=b.color;stroke.strokeWidth=5f*(1-t);c.drawCircle(b.x*w,b.y*h,12f+t*45f,stroke)};floats.forEach{f->val t=(now-f.born)/850f;paint.color=f.color;paint.alpha=(255*(1-t)).toInt().coerceIn(0,255);paint.textAlign=Paint.Align.CENTER;paint.textSize=(if(f.critical)38f else 25f)*resources.displayMetrics.scaledDensity/2f;paint.typeface=Typeface.DEFAULT_BOLD;c.drawText(f.text,f.x*w,f.y*h-35f-t*55f,paint);paint.alpha=255}}
    private fun drawHud(c:Canvas,s:ActionBattleState,w:Float,h:Float){paint.color=Color.argb(170,12,14,19);rect.set(16f,14f,w*.42f,h*.10f);c.drawRoundRect(rect,18f,18f,paint);paint.textAlign=Paint.Align.LEFT;paint.textSize=h*.035f;paint.typeface=Typeface.DEFAULT_BOLD;paint.color=Color.WHITE;c.drawText(labels.objective,31f,h*.071f,paint);val heroes=s.actors.filter{it.side==BattleSide.HERO};heroes.forEachIndexed{i,a->val x=w*.62f+i*h*.12f;val y=h*.13f;paint.color=if(a.id==s.activeHeroId)Color.rgb(234,185,76)else Color.argb(190,30,34,42);c.drawCircle(x,y,h*.047f,paint);drawActorAsset(c,a,x,y+4,h/620f);drawUnitBar(c,a,x,y+h*.052f,h*.085f)}}
    private fun drawControls(c:Canvas,s:ActionBattleState,w:Float,h:Float){drawJoystick(c,h);val active=s.actors.firstOrNull{it.id==s.activeHeroId};drawButton(c,w*.91f,h*.78f,h*.098f,labels.attack,Color.rgb(221,105,49),active?.cooldown?:0f,.65f);drawButton(c,w*.78f,h*.82f,h*.074f,labels.skill,Color.rgb(48,137,181),active?.skillCooldown?:0f,6f);drawButton(c,w*.67f,h*.79f,h*.069f,labels.ultimate,Color.rgb(226,169,52),if((active?.energy?:0)<100)1f else 0f,1f);drawButton(c,w*.91f,h*.26f,h*.057f,labels.switch,Color.rgb(74,78,91),0f,1f)}
    private fun drawJoystick(c:Canvas,h:Float){val cx=h*.18f;val cy=h*.79f;paint.color=Color.argb(90,12,16,22);c.drawCircle(cx,cy,h*.125f,paint);stroke.color=Color.argb(170,213,225,234);stroke.strokeWidth=4f;c.drawCircle(cx,cy,h*.11f,stroke);paint.color=Color.argb(220,78,170,196);c.drawCircle(cx+smoothedMovement.x*h*.065f,cy+smoothedMovement.y*h*.065f,h*.044f,paint)}
    private fun drawButton(c:Canvas,x:Float,y:Float,r:Float,label:String,color:Int,cooldown:Float,maxCooldown:Float){paint.color=Color.argb(100,0,0,0);c.drawCircle(x+4,y+7,r,paint);paint.color=color;c.drawCircle(x,y,r,paint);stroke.color=Color.argb(210,255,255,255);stroke.strokeWidth=3f;c.drawCircle(x,y,r,stroke);if(cooldown>0){paint.color=Color.argb(150,10,12,16);rect.set(x-r,y-r,x+r,y+r);c.drawArc(rect,-90f,360f*(cooldown/maxCooldown).coerceIn(0f,1f),true,paint)};paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=min(r*.30f,18f*resources.displayMetrics.scaledDensity);c.drawText(label,x,y+paint.textSize*.35f,paint)}
    private fun drawFinish(c:Canvas,s:ActionBattleState,w:Float,h:Float){paint.color=Color.argb(190,8,9,13);c.drawRect(0f,0f,w,h,paint);paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=h*.11f;paint.color=if(s.result==BattleResult.VICTORY)Color.rgb(235,187,79)else Color.rgb(235,85,74);c.drawText(if(s.result==BattleResult.VICTORY)labels.victory else labels.defeat,w/2,h/2,paint)}

    override fun onTouchEvent(e:MotionEvent):Boolean{val h=height.toFloat();val joy=Vec2(h*.18f,h*.79f);when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{val i=e.actionIndex;val x=e.getX(i);val y=e.getY(i);if(x<width*.38f){joystickPointer=e.getPointerId(i);setMovement(x,y,joy,h)}else handleButton(x,y)};MotionEvent.ACTION_MOVE->{val i=e.findPointerIndex(joystickPointer);if(i>=0)setMovement(e.getX(i),e.getY(i),joy,h)};MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_CANCEL->if(e.getPointerId(e.actionIndex)==joystickPointer){joystickPointer=-1;movement=Vec2(0f,0f)}};return true}
    private fun setMovement(x:Float,y:Float,joy:Vec2,h:Float){val raw=Vec2((x-joy.x)/(h*.11f),(y-joy.y)/(h*.11f));movement=if(raw.distance(Vec2(0f,0f))<.16f)Vec2(0f,0f)else raw.normalized()}
    private fun handleButton(x:Float,y:Float){val w=width.toFloat();val h=height.toFloat();val command=when{y<h*.39f&&x>w*.83f->ActionCommand.SWITCH;x>w*.85f->ActionCommand.ATTACK;x>w*.72f->ActionCommand.SKILL;else->ActionCommand.ULTIMATE};engine.command(command);performClick();invalidate()}
    override fun performClick():Boolean{super.performClick();return true}
}
