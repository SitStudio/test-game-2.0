package com.jolttime.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolttime.game.R
import com.jolttime.game.domain.model.*
import com.jolttime.game.game.GameBattleView
import com.jolttime.game.ui.theme.*

private enum class Screen(val label:Int,val icon:ImageVector){ ARCHIVE(R.string.nav_archive,Icons.Outlined.AccountBalance),MAP(R.string.nav_map,Icons.Outlined.Map),HEROES(R.string.nav_heroes,Icons.Outlined.Groups),MUSEUM(R.string.nav_museum,Icons.Outlined.Museum) }

@Composable fun GameApp(vm:GameViewModel){
    JoltTheme {
        val ui by vm.ui.collectAsStateWithLifecycle()
        var screen by remember { mutableStateOf(Screen.ARCHIVE) }
        if(!ui.loaded) Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){CircularProgressIndicator()}
        else if(!ui.game.storyIntroSeen) StoryOverlay(GameContent.stories.getValue("intro"),vm::introSeen)
        else if(ui.game.activeMissionId!=null) ActionBattleScreen(ui.game,vm)
        else Row(Modifier.fillMaxSize().background(Ink)) {
            NavigationRail(containerColor=Color(0xFF0D1015)) { Spacer(Modifier.weight(1f));Screen.entries.forEach{NavigationRailItem(selected=screen==it,onClick={screen=it},icon={Icon(it.icon,null)},label={Text(stringResource(it.label),fontSize=10.sp)})};Spacer(Modifier.weight(1f)) }
            Box(Modifier.weight(1f).fillMaxHeight()){when(screen){Screen.ARCHIVE->ArchiveScreen(ui.game,{screen=Screen.MAP},{screen=Screen.HEROES});Screen.MAP->MapScreen(ui.game,vm);Screen.HEROES->HeroesScreen(ui.game,vm);Screen.MUSEUM->MuseumScreen(ui.game,vm)}}
        }
        val pendingStory = ui.game.pendingStoryId
        val pendingArtifact = ui.game.pendingArtifactId
        when {
            pendingStory != null -> StoryOverlay(GameContent.stories.getValue(pendingStory), vm::dismissStory)
            pendingArtifact != null -> ArtifactReveal(ui.game.artifacts.first { it.id == pendingArtifact }, vm::dismissArtifact)
            ui.game.chapterComplete -> ChapterComplete()
        }
    }
}

@Composable private fun ArchiveScreen(g:GameState,map:()->Unit,heroes:()->Unit){
    Row(Modifier.fillMaxSize().padding(24.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)){
        Column(Modifier.weight(1.2f).fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween){
            Column{Kicker(stringResource(R.string.archive_kicker));Text(stringResource(R.string.archive_title),fontSize=30.sp,fontWeight=FontWeight.Bold);Text(stringResource(R.string.archive_body),color=Muted,modifier=Modifier.padding(top=8.dp))}
            Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF121720),RoundedCornerShape(24.dp)).border(1.dp,Gold.copy(.35f),RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){Canvas(Modifier.size(110.dp)){drawCircle(Gold.copy(.16f));drawCircle(Gold,style=androidx.compose.ui.graphics.drawscope.Stroke(3f));drawLine(Gold,center,Offset(center.x,size.height*.12f),4f,StrokeCap.Round);drawLine(Gold,center,Offset(size.width*.72f,size.height*.62f),4f,StrokeCap.Round)};Text(stringResource(R.string.time_core_portal),modifier=Modifier.align(Alignment.BottomCenter).padding(14.dp),color=Gold,fontWeight=FontWeight.Bold)}
            Button(onClick=map,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.enter_egypt))}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            SectionTitle(stringResource(R.string.archive_status));InfoCard(stringResource(R.string.current_epoch),stringResource(R.string.epoch_egypt));InfoCard(stringResource(R.string.mission_progress),"${g.completedMissionIds.size} / ${GameContent.missions.size}");InfoCard(stringResource(R.string.time_energy),g.timeEnergy.toString());InfoCard(stringResource(R.string.upgrade_materials),g.upgradeMaterials.toString());OutlinedButton(onClick=heroes,Modifier.fillMaxWidth()){Text(stringResource(R.string.manage_team))}
        }
    }
}

@Composable private fun MapScreen(g:GameState,vm:GameViewModel){
    var selected by remember { mutableStateOf<Mission?>(null) };var story by remember { mutableStateOf<StoryScene?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Kicker(stringResource(R.string.chapter_one));Text(stringResource(R.string.epoch_egypt),fontSize=28.sp,fontWeight=FontWeight.Bold)};Text("${g.completedMissionIds.size}/${GameContent.missions.size} ${stringResource(R.string.restored)}",color=Gold)}
        Box(Modifier.weight(1f).fillMaxWidth().padding(top=10.dp)){
            Canvas(Modifier.fillMaxSize()){val y=size.height*.53f;drawLine(Color(0xFF5E5239),Offset(size.width*.07f,y),Offset(size.width*.93f,y),5f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(12f,10f)))}
            Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){GameContent.missions.forEachIndexed{i,m->val unlocked=i<=g.unlockedMissionIndex;val done=m.id in g.completedMissionIds;MapNode(m,unlocked,done){if(unlocked){if(m.storyBefore!=null&&!done)story=GameContent.stories[m.storyBefore] else selected=m}}}}
        }
    }
    selected?.let { m->TeamDialog(g,m,onClose={selected=null},onStart={vm.startMission(m.id);selected=null}) }
    story?.let { scene->StoryOverlay(scene){story=null;selected=GameContent.missions[g.unlockedMissionIndex]} }
}

@Composable private fun MapNode(m:Mission,unlocked:Boolean,done:Boolean,on:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(86.dp).clickable(enabled=unlocked,onClick=on)){Surface(shape=CircleShape,color=when{done->Gold;unlocked->Color(0xFF252B35);else->Color(0xFF14171D)},border=androidx.compose.foundation.BorderStroke(1.dp,if(unlocked)Gold.copy(.7f) else Color.DarkGray),modifier=Modifier.size(if(m.nodeType==MissionNodeType.BOSS)64.dp else 52.dp)){Box(contentAlignment=Alignment.Center){Icon(when(m.nodeType){MissionNodeType.BOSS->Icons.Outlined.Warning;MissionNodeType.ELITE->Icons.Outlined.Shield;MissionNodeType.STORY->Icons.Outlined.AutoStories;else->Icons.Outlined.Place},null,tint=if(done)Ink else if(unlocked)Gold else Muted)}};Text(keyText(m.locationKey),fontSize=10.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp),color=if(unlocked)Text else Muted)} }

@Composable private fun TeamDialog(g:GameState,m:Mission,onClose:()->Unit,onStart:()->Unit){AlertDialog(onDismissRequest=onClose,title={Text(keyText(m.titleKey))},text={Column{Text(stringResource(R.string.objective_format,keyText("objective_${m.objective.name.lowercase()}")),color=Muted);Spacer(Modifier.height(10.dp));Text(stringResource(R.string.selected_team),fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){g.selectedTeam.mapNotNull{id->g.heroes.firstOrNull{it.id==id}}.forEach{HeroChip(it)}}}},dismissButton={TextButton(onClick=onClose){Text(stringResource(R.string.cancel))}},confirmButton={Button(onClick=onStart){Text(stringResource(R.string.start_mission))}})}

@Composable private fun HeroesScreen(g:GameState,vm:GameViewModel){var selected by remember{mutableStateOf<Hero?>(null)};Column(Modifier.fillMaxSize().padding(20.dp)){Row{Column(Modifier.weight(1f)){Kicker(stringResource(R.string.roster));Text(stringResource(R.string.heroes),fontSize=28.sp,fontWeight=FontWeight.Bold)};Text("${g.selectedTeam.size}/3 ${stringResource(R.string.in_team)}",color=Gold)};Row(Modifier.weight(1f).fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){g.heroes.forEach{hero->val inTeam=hero.id in g.selectedTeam;Card(Modifier.weight(1f).fillMaxHeight().clickable{selected=hero},colors=CardDefaults.cardColors(containerColor=Card),border=androidx.compose.foundation.BorderStroke(1.dp,if(inTeam)Gold else Outline)){Column(Modifier.padding(14.dp)){HeroSilhouette(hero.role);Text(keyText(hero.nameKey),fontWeight=FontWeight.Bold,fontSize=18.sp);Text(keyText("role_${hero.role.name.lowercase()}"),color=Gold,fontSize=11.sp);Text("${stringResource(R.string.level)} ${hero.level}",modifier=Modifier.padding(top=8.dp));LinearProgressIndicator(progress={hero.xp/hero.xpToNext.toFloat()},Modifier.fillMaxWidth().padding(top=5.dp));Text("HP ${hero.maxHp}  ATK ${hero.attack}",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))}}}}}
    selected?.let{hero->HeroDetails(g,hero,{selected=null},{vm.upgradeHero(hero.id)},{checked->val next=if(checked)(g.selectedTeam+hero.id).takeLast(3) else g.selectedTeam-hero.id;vm.selectTeam(next)})}}

@Composable private fun HeroDetails(g:GameState,h:Hero,close:()->Unit,upgrade:()->Unit,team:(Boolean)->Unit){val selected=h.id in g.selectedTeam;AlertDialog(onDismissRequest=close,title={Text(keyText(h.nameKey))},text={Column(Modifier.verticalScroll(rememberScrollState())){Text(keyText(h.storyKey),color=Muted);Text(keyText(h.identityKey),color=Gold,fontSize=11.sp,modifier=Modifier.padding(top=6.dp));Spacer(Modifier.height(10.dp));Text("HP ${h.maxHp} · ATK ${h.attack} · DEF ${h.defense} · SPD ${h.speed}");Text(keyText(h.passiveKey),color=Gold,modifier=Modifier.padding(top=8.dp));h.abilities().forEach{Text("• ${keyText(it.nameKey)}",modifier=Modifier.padding(top=5.dp))};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(selected,onCheckedChange=team,enabled=!selected);Text(stringResource(R.string.selected_for_battle))}}},dismissButton={TextButton(onClick=close){Text(stringResource(R.string.close))}},confirmButton={Button(onClick=upgrade,enabled=g.upgradeMaterials>=h.level*25){Text(stringResource(R.string.upgrade_cost,h.level*25))}})}

@Composable private fun MuseumScreen(g:GameState,vm:GameViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Row{Column(Modifier.weight(1f)){Kicker(stringResource(R.string.time_archive));Text(stringResource(R.string.museum_egypt),fontSize=28.sp,fontWeight=FontWeight.Bold)};Text("${(g.museumProgress*100).toInt()}%",fontSize=24.sp,color=Gold,fontWeight=FontWeight.Bold)};LinearProgressIndicator(progress={g.museumProgress},Modifier.fillMaxWidth().padding(vertical=10.dp));Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp)){g.artifacts.forEach{a->ArtifactCard(a,g.selectedTeam.firstOrNull()?.let{id->g.heroes.first{it.id==id}}){heroId->vm.equip(heroId,a.id)}}}}}

@Composable private fun RowScope.ArtifactCard(a:Artifact,hero:Hero?,equip:(String)->Unit){Card(Modifier.weight(1f).fillMaxHeight(),colors=CardDefaults.cardColors(containerColor=Card),border=androidx.compose.foundation.BorderStroke(1.dp,rarityColor(a.rarity).copy(.55f))){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(if(a.isCompleted)Icons.Outlined.Diamond else Icons.Outlined.Lock,null,tint=rarityColor(a.rarity),modifier=Modifier.size(36.dp));Text(keyText(a.nameKey),fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=2);Text(a.rarity.name,fontSize=9.sp,color=rarityColor(a.rarity));Text("${a.fragmentsOwned}/${a.fragmentsRequired}",color=Muted);Text(keyText(a.descriptionKey),fontSize=10.sp,color=Muted,maxLines=3);Text(keyText(a.effectKey),fontSize=9.sp,color=Gold,maxLines=3,modifier=Modifier.padding(top=4.dp));Spacer(Modifier.weight(1f));Text("+${a.combatBonus} ${a.stat}",color=Gold,fontSize=10.sp);Text("+${a.museumBonusPercent}% ${stringResource(R.string.account_bonus)}",fontSize=9.sp);if(a.isCompleted&&hero!=null)TextButton(onClick={equip(hero.id)}){Text(stringResource(R.string.equip_to, keyText(hero.nameKey)),fontSize=9.sp)}}}}

@Composable private fun ActionBattleScreen(g:GameState,vm:GameViewModel){val mission=GameContent.missions.first{it.id==g.activeMissionId};val heroes=g.selectedTeam.mapNotNull{id->g.heroes.firstOrNull{it.id==id}};val labels=GameBattleView.Labels(stringResource(R.string.control_attack),stringResource(R.string.control_skill),stringResource(R.string.control_ultimate),stringResource(R.string.control_switch),keyText(mission.titleKey),stringResource(R.string.victory),stringResource(R.string.defeat));AndroidView(factory={context->GameBattleView(context,mission,heroes,g.artifacts,labels,vm::finishMission)},modifier=Modifier.fillMaxSize())}

@Composable private fun StoryOverlay(scene:StoryScene,finish:()->Unit){var index by remember(scene.id){mutableIntStateOf(0)};val line=scene.lines[index];Box(Modifier.fillMaxSize().background(Color(0xFF080A0D)).clickable{if(index<scene.lines.lastIndex)index++ else finish()}){Canvas(Modifier.fillMaxSize()){drawCircle(Gold.copy(.08f),radius=size.minDimension*.35f,center=Offset(size.width*.78f,size.height*.36f))};Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xE6181B21)).padding(horizontal=38.dp,vertical=22.dp)){Kicker(keyText(line.speakerKey));Text(keyText(line.textKey),fontSize=20.sp,lineHeight=28.sp);Text(stringResource(R.string.tap_to_continue),fontSize=11.sp,color=Muted,modifier=Modifier.align(Alignment.End).padding(top=12.dp))}}}
@Composable private fun ArtifactReveal(a:Artifact,close:()->Unit){AlertDialog(onDismissRequest={},icon={Icon(Icons.Outlined.AutoAwesome,null,tint=rarityColor(a.rarity),modifier=Modifier.size(48.dp))},title={Column(horizontalAlignment=Alignment.CenterHorizontally){Kicker(stringResource(R.string.artifact_restored));Text(keyText(a.nameKey))}},text={Column(horizontalAlignment=Alignment.CenterHorizontally){Text(a.rarity.name,color=rarityColor(a.rarity));Text(keyText(a.descriptionKey),modifier=Modifier.padding(vertical=10.dp));Text("+${a.combatBonus} ${a.stat} · +${a.museumBonusPercent}% ${stringResource(R.string.museum_bonus)}",color=Gold)}},confirmButton={Button(onClick=close){Text(stringResource(R.string.add_to_museum))}})}
@Composable private fun ChapterComplete(){var visible by remember{mutableStateOf(true)};if(visible)AlertDialog(onDismissRequest={},icon={Icon(Icons.Outlined.EmojiEvents,null,tint=Gold)},title={Text(stringResource(R.string.egypt_restored))},text={Text(stringResource(R.string.egypt_restored_body))},confirmButton={Button(onClick={visible=false}){Text(stringResource(R.string.return_archive))}})}

@Composable private fun HeroChip(h:Hero){Surface(shape=RoundedCornerShape(10.dp),color=Card){Text(keyText(h.nameKey),Modifier.padding(9.dp),fontSize=12.sp)}}
@Composable private fun HeroSilhouette(role:HeroRole,size:Int=66){Box(Modifier.size(size.dp).background(Gold.copy(.1f),CircleShape),contentAlignment=Alignment.Center){Icon(when(role){HeroRole.TANK->Icons.Outlined.Shield;HeroRole.FIGHTER->Icons.Outlined.Bolt;HeroRole.RANGED->Icons.Outlined.GpsFixed;HeroRole.SUPPORT->Icons.Outlined.Favorite;HeroRole.CONTROLLER->Icons.Outlined.AutoStories},null,tint=Gold)}}
@Composable private fun InfoCard(label:String,value:String){Surface(shape=RoundedCornerShape(14.dp),color=Card,border=androidx.compose.foundation.BorderStroke(1.dp,Outline)){Row(Modifier.fillMaxWidth().padding(14.dp)){Text(label,color=Muted);Spacer(Modifier.weight(1f));Text(value,fontWeight=FontWeight.Bold,color=Gold)}}}
@Composable private fun Kicker(text:String){Text(text.uppercase(),fontSize=10.sp,letterSpacing=1.5.sp,color=Gold,fontWeight=FontWeight.Bold)}
@Composable private fun SectionTitle(text:String){Text(text,fontSize=16.sp,fontWeight=FontWeight.Bold)}
@Composable private fun keyText(key:String):String{val context=LocalContext.current;val id=context.resources.getIdentifier(key,"string",context.packageName);return if(id==0)key else stringResource(id)}
private fun Hero.abilities()=listOf(basic,skill,ultimate)
private fun rarityColor(r:Rarity)=when(r){Rarity.COMMON->Muted;Rarity.RARE->Color(0xFF7FA8C9);Rarity.EPIC->Color(0xFFA78BC5);Rarity.LEGENDARY->Gold;Rarity.MYTHIC->Color(0xFFC77C70)}
