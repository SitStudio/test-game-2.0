package com.jolttime.game.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.jolttime.game.domain.model.*
import kotlin.math.min

class GameBattleView(
    context:Context,
    mission:Mission,
    heroes:List<Hero>,
    artifacts:List<Artifact>,
    private val labels:Labels,
    private val finished:(Boolean)->Unit,
):View(context){
    data class Labels(val attack:String,val skill:String,val ultimate:String,val switch:String,val objective:String,val victory:String,val defeat:String)
    private val engine=ActionBattleEngine(mission,heroes,artifacts)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrame=0L
    private var movement=Vec2(0f,0f)
    private var joystickPointer=-1
    private var reported=false
    init{isFocusable=true;contentDescription=labels.objective}
    override fun onAttachedToWindow(){super.onAttachedToWindow();lastFrame=System.nanoTime();postOnAnimation(::frame)}
    private fun frame(){if(!isAttachedToWindow)return;val now=System.nanoTime();val dt=(now-lastFrame)/1_000_000_000f;lastFrame=now;val state=engine.step(dt,PlayerInput(movement));if(state.result!=BattleResult.IN_PROGRESS&&!reported){reported=true;postDelayed({finished(state.result==BattleResult.VICTORY)},900)};invalidate();postOnAnimation(::frame)}
    override fun onDraw(canvas:Canvas){super.onDraw(canvas);val w=width.toFloat();val h=height.toFloat();paint.style=Paint.Style.FILL;paint.color=Color.rgb(32,24,18);canvas.drawRect(0f,0f,w,h,paint);paint.color=Color.rgb(71,52,31);for(i in 0..8)canvas.drawCircle(w*(i*.14f),h*(.18f+(i%3)*.27f),h*.08f,paint)
        val s=engine.state;s.actors.filter{it.alive}.forEach{actor->val x=actor.position.x*w;val y=actor.position.y*h;val radius=if(actor.boss)h*.075f else h*.045f;paint.color=when{actor.id==s.activeHeroId->Color.rgb(240,190,80);actor.side==BattleSide.HERO->Color.rgb(75,155,205);actor.boss->Color.rgb(145,55,50);else->Color.rgb(190,92,65)};canvas.drawCircle(x,y,radius,paint);paint.color=Color.rgb(20,20,22);canvas.drawRect(x-radius,y-radius-11,x+radius,y-radius-5,paint);paint.color=Color.rgb(77,190,102);canvas.drawRect(x-radius,y-radius-11,x-radius+radius*2*(actor.hp/actor.maxHp.toFloat()),y-radius-5,paint);if(actor.shield>0){paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;paint.color=Color.CYAN;canvas.drawCircle(x,y,radius+5,paint);paint.style=Paint.Style.FILL}}
        drawJoystick(canvas,h);drawButton(canvas,w*.91f,h*.76f,h*.095f,labels.attack,Color.rgb(207,112,55));drawButton(canvas,w*.78f,h*.81f,h*.075f,labels.skill,Color.rgb(73,127,174));drawButton(canvas,w*.67f,h*.79f,h*.068f,labels.ultimate,Color.rgb(190,148,54));drawButton(canvas,w*.91f,h*.19f,h*.055f,labels.switch,Color.rgb(70,73,82));paint.textAlign=Paint.Align.LEFT;paint.textSize=h*.035f;paint.color=Color.WHITE;canvas.drawText(labels.objective,w*.03f,h*.07f,paint);val active=s.actors.firstOrNull{it.id==s.activeHeroId};if(active!=null){canvas.drawText("${active.energy}%",w*.72f,h*.08f,paint)};if(s.result!=BattleResult.IN_PROGRESS){paint.color=Color.argb(210,8,9,12);canvas.drawRect(0f,0f,w,h,paint);paint.textAlign=Paint.Align.CENTER;paint.textSize=h*.1f;paint.color=Color.rgb(231,188,93);canvas.drawText(if(s.result==BattleResult.VICTORY)labels.victory else labels.defeat,w/2,h/2,paint)}}
    private fun drawJoystick(canvas:Canvas,h:Float){val centerX=h*.18f;val centerY=h*.78f;paint.color=Color.argb(90,255,255,255);canvas.drawCircle(centerX,centerY,h*.11f,paint);paint.color=Color.argb(190,220,220,220);canvas.drawCircle(centerX+movement.x*h*.065f,centerY+movement.y*h*.065f,h*.045f,paint)}
    private fun drawButton(c:Canvas,x:Float,y:Float,r:Float,label:String,color:Int){paint.color=color;c.drawCircle(x,y,r,paint);paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=min(r*.36f,22f*resources.displayMetrics.scaledDensity);c.drawText(label,x,y+paint.textSize*.35f,paint)}
    override fun onTouchEvent(e:MotionEvent):Boolean{val h=height.toFloat();val joy=Vec2(h*.18f,h*.78f);when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{val i=e.actionIndex;val x=e.getX(i);val y=e.getY(i);if(x<width*.38f){joystickPointer=e.getPointerId(i);movement=Vec2((x-joy.x)/(h*.11f),(y-joy.y)/(h*.11f)).normalized()}else handleButton(x,y)};MotionEvent.ACTION_MOVE->{val i=e.findPointerIndex(joystickPointer);if(i>=0)movement=Vec2((e.getX(i)-joy.x)/(h*.11f),(e.getY(i)-joy.y)/(h*.11f)).normalized()};MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_CANCEL->{if(e.getPointerId(e.actionIndex)==joystickPointer){joystickPointer=-1;movement=Vec2(0f,0f)}}};return true}
    private fun handleButton(x:Float,y:Float){val w=width.toFloat();val h=height.toFloat();val command=when{y<h*.35f&&x>w*.82f->ActionCommand.SWITCH;x>w*.85f->ActionCommand.ATTACK;x>w*.72f->ActionCommand.SKILL;else->ActionCommand.ULTIMATE};engine.command(command);performClick();invalidate()}
    override fun performClick():Boolean{super.performClick();return true}
}
