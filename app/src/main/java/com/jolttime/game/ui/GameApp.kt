package com.jolttime.game.ui

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolttime.game.BuildConfig
import com.jolttime.game.R
import com.jolttime.game.domain.model.*
import com.jolttime.game.game.GameBattleView
import com.jolttime.game.ui.theme.*
import com.jolttime.game.ui.visual.*

private enum class Screen { ARCHIVE, MAP, HEROES, MUSEUM, SETTINGS }

@Composable
fun GameApp(vm: GameViewModel) {
    JoltTheme {
        val ui by vm.ui.collectAsStateWithLifecycle()
        var screen by remember { mutableStateOf(Screen.ARCHIVE) }
        val context = LocalContext.current
        LaunchedEffect(ui.loaded, ui.game.languageTag) {
            if (ui.loaded && ui.game.languageTag.isNotBlank() &&
                context.resources.configuration.locales[0].language != ui.game.languageTag
            ) {
                applyAppLanguage(context as? Activity, ui.game.languageTag)
            }
        }
        Box(Modifier.fillMaxSize().background(JoltPalette.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            when {
                !ui.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color=JoltPalette.goldAccent)
                !ui.game.storyIntroSeen -> StorySceneOverlay(
                    scene = GameContent.stories.getValue("intro"),
                    environment = EnvironmentScene.ARCHIVE,
                    finish = vm::introSeen,
                )
                ui.game.activeMissionId != null -> ActionBattleScreen(ui.game, vm)
                else -> when(screen) {
                    Screen.ARCHIVE -> ArchiveHub(ui.game, { screen=Screen.MAP }, {screen=Screen.HEROES}, {screen=Screen.MUSEUM}, {screen=Screen.SETTINGS})
                    Screen.MAP -> EgyptMap(ui.game,vm){screen=Screen.ARCHIVE}
                    Screen.HEROES -> HeroesRoom(ui.game,vm){screen=Screen.ARCHIVE}
                    Screen.MUSEUM -> MuseumRoom(ui.game,vm){screen=Screen.ARCHIVE}
                    Screen.SETTINGS -> SettingsScreen(vm){screen=Screen.ARCHIVE}
                }
            }
            ui.battleResult?.let { ResultScreen(it,ui.game,vm::continueBattleResult) }
            val story=ui.game.pendingStoryId
            val artifact=ui.game.pendingArtifactId
            when {
                story != null -> StorySceneOverlay(
                    scene = GameContent.stories.getValue(story),
                    environment = sceneForMission(ui.game.unlockedMissionIndex),
                    finish = vm::dismissStory,
                )
                artifact!=null -> ArtifactReveal(ui.game.artifacts.first{it.id==artifact},vm::dismissArtifact)
                ui.game.chapterComplete -> ChapterCelebration()
            }
        }
    }
}

@Composable
private fun ArchiveHub(g:GameState,campaign:()->Unit,heroes:()->Unit,museum:()->Unit,settings:()->Unit){
    val palette=JoltPalette
    Box(Modifier.fillMaxSize()){
        EnvironmentVisual(EnvironmentScene.ARCHIVE)
        CompactHud(g,settings,Modifier.align(Alignment.TopCenter))
        val pulse=rememberInfiniteTransition(label="portal").animateFloat(1f,1.07f,infiniteRepeatable(tween(1600),RepeatMode.Reverse),label="portalPulse").value
        Canvas(Modifier.size(260.dp).align(Alignment.Center).scale(pulse)){repeat(3){i->drawCircle(palette.temporalBlue.copy(alpha=.2f-i*.04f),size.minDimension*(.27f+i*.09f),style=Stroke(3f+i));};drawCircle(Brush.radialGradient(listOf(Color.White,palette.temporalBlue,palette.goldAccent.copy(.25f),Color.Transparent)),size.minDimension*.28f)}
        Text(stringResource(R.string.time_archive),Modifier.align(Alignment.Center).offset(y=92.dp),color=JoltPalette.textPrimary,fontSize=18.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
        HubPortal(stringResource(R.string.campaign),HubSymbol.CAMPAIGN,campaign,Modifier.align(Alignment.CenterStart).offset(x=70.dp,y=(-55).dp))
        HubPortal(stringResource(R.string.heroes),HubSymbol.HEROES,heroes,Modifier.align(Alignment.CenterEnd).offset(x=(-70).dp,y=(-55).dp))
        HubPortal(stringResource(R.string.nav_museum),HubSymbol.MUSEUM,museum,Modifier.align(Alignment.BottomCenter).offset(y=(-34).dp))
    }
}

private enum class HubSymbol { CAMPAIGN,HEROES,MUSEUM }
@Composable
private fun HubPortal(
    label: String,
    symbol: HubSymbol,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val nodeType = when (symbol) {
        HubSymbol.CAMPAIGN -> MissionNodeType.BATTLE
        HubSymbol.HEROES -> MissionNodeType.STORY
        HubSymbol.MUSEUM -> MissionNodeType.ARTIFACT
    }
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MissionNodeVisual(
            type = nodeType,
            modifier = Modifier
                .size(86.dp)
                .shadow(16.dp, CircleShape),
        )
        Text(
            text = label.uppercase(),
            color = JoltPalette.textPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable private fun CompactHud(g:GameState,settings:()->Unit,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){HudPill("LV ${g.accountLevel}",JoltPalette.goldAccent);Spacer(Modifier.weight(1f));HudPill("◈ ${g.timeEnergy}",JoltPalette.temporalBlue);HudPill("◆ ${g.upgradeMaterials}",JoltPalette.goldAccent);Box(Modifier.size(42.dp).background(JoltPalette.surface.copy(.85f),CircleShape).clickable(onClick=settings),contentAlignment=Alignment.Center){Text("⚙",fontSize=20.sp,color=JoltPalette.textPrimary)}}}
@Composable private fun HudPill(text:String,color:Color){Text(text,color=JoltPalette.textPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.background(JoltPalette.surface.copy(.88f),CircleShape).border(1.dp,color.copy(.55f),CircleShape).padding(horizontal=13.dp,vertical=8.dp))}

@Composable private fun EgyptMap(g:GameState,vm:GameViewModel,back:()->Unit){val palette=JoltPalette;var selected by remember{mutableStateOf<Mission?>(null)};var story by remember{mutableStateOf<StoryScene?>(null)};Box(Modifier.fillMaxSize()){EnvironmentVisual(EnvironmentScene.DESERT);TopBack(stringResource(R.string.epoch_egypt),back);Canvas(Modifier.fillMaxSize().padding(horizontal=80.dp,vertical=70.dp)){val points=GameContent.missions.indices.map{i->Offset(size.width*(.05f+i*.9f/(GameContent.missions.size-1)),size.height*(if(i%2==0).58f else .35f))};points.zipWithNext().forEach{(a,b)->drawLine(Color(0xFF704E2D),a,b,14f,StrokeCap.Round);drawLine(palette.goldAccent.copy(.7f),a,b,3f,StrokeCap.Round)}};Row(Modifier.fillMaxWidth().align(Alignment.Center).horizontalScroll(rememberScrollState()).padding(horizontal=62.dp),horizontalArrangement=Arrangement.spacedBy(38.dp)){GameContent.missions.forEachIndexed{i,m->val unlocked=i<=g.unlockedMissionIndex;val done=m.id in g.completedMissionIds;MissionNode(m,unlocked,done,i==g.unlockedMissionIndex){if(unlocked){if(m.storyBefore!=null&&!done)story=GameContent.stories[m.storyBefore] else selected=m}}}};selected?.let{TeamPanel(g,it,{selected=null}){vm.startMission(it.id);selected=null}};story?.let{s->StorySceneOverlay(s,sceneForMission(g.unlockedMissionIndex)){story=null;selected=GameContent.missions[g.unlockedMissionIndex]}}}}
@Composable
private fun MissionNode(
    m: Mission,
    unlocked: Boolean,
    done: Boolean,
    current: Boolean,
    onClick: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = m.id).animateFloat(
        initialValue = .92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "node",
    ).value
    Column(
        modifier = Modifier.width(104.dp).clickable(enabled = unlocked, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(if (m.nodeType == MissionNodeType.BOSS) 88.dp else 74.dp)
                .scale(if (current) pulse else 1f)
                .alpha(if (unlocked) 1f else .42f),
            contentAlignment = Alignment.Center,
        ) {
            MissionNodeVisual(m.nodeType, Modifier.fillMaxSize())
            if (done) {
                Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(
            text = keyText(m.locationKey),
            color = if (unlocked) JoltPalette.textPrimary else JoltPalette.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable private fun HeroesRoom(g:GameState,vm:GameViewModel,back:()->Unit){var selectedId by remember{mutableStateOf(g.selectedTeam.first())};val hero=g.heroes.first{it.id==selectedId};Box(Modifier.fillMaxSize()){EnvironmentVisual(EnvironmentScene.ARCHIVE);TopBack(stringResource(R.string.heroes),back);Column(Modifier.align(Alignment.CenterStart).padding(start=32.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){g.heroes.forEach{h->Box(Modifier.size(66.dp).clip(CircleShape).background(if(h.id==selectedId)JoltPalette.goldAccent else JoltPalette.surfaceElevated).border(2.dp,if(h.id in g.selectedTeam)JoltPalette.temporalBlue else Color.Transparent,CircleShape).clickable{selectedId=h.id}){HeroVisual(h.id,Modifier.fillMaxSize().padding(5.dp))}}};HeroVisual(hero.id,Modifier.align(Alignment.Center).size(290.dp));Column(Modifier.align(Alignment.CenterEnd).width(270.dp).padding(end=24.dp).background(JoltPalette.surface.copy(.9f),RoundedCornerShape(24.dp)).padding(18.dp)){Text(keyText(hero.nameKey),fontSize=27.sp,fontWeight=FontWeight.Black,color=JoltPalette.textPrimary);Text(keyText("role_${hero.role.name.lowercase()}"),color=JoltPalette.goldAccent,fontWeight=FontWeight.Bold);Text("LV ${hero.level}  •  PWR ${hero.attack+hero.defense+hero.maxHp/10}",color=JoltPalette.textSecondary,modifier=Modifier.padding(vertical=8.dp));StatBar("HP",hero.maxHp,350,JoltPalette.success);StatBar("ATK",hero.attack,80,JoltPalette.danger);Row(Modifier.padding(top=13.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){hero.abilities().forEach{AbilityOrb(keyText(it.nameKey))}};GameButton(stringResource(R.string.upgrade_cost,hero.level*25),{vm.upgradeHero(hero.id)},g.upgradeMaterials>=hero.level*25);val selected=hero.id in g.selectedTeam;GameButton(if(selected)stringResource(R.string.in_team) else stringResource(R.string.selected_for_battle),{if(!selected)vm.selectTeam((g.selectedTeam+hero.id).takeLast(3))},!selected)}}}
@Composable private fun AbilityOrb(name:String){Box(Modifier.size(54.dp).background(JoltPalette.surfaceElevated,CircleShape).border(1.dp,JoltPalette.temporalBlue,CircleShape),contentAlignment=Alignment.Center){Text(name.take(2).uppercase(),color=JoltPalette.textPrimary,fontWeight=FontWeight.Black)}}
@Composable private fun StatBar(label:String,value:Int,max:Int,color:Color){Row(verticalAlignment=Alignment.CenterVertically){Text(label,color=JoltPalette.textSecondary,fontSize=10.sp,modifier=Modifier.width(28.dp));Box(Modifier.weight(1f).height(8.dp).background(Color.Black.copy(.35f),CircleShape)){Box(Modifier.fillMaxWidth((value/max.toFloat()).coerceIn(0f,1f)).fillMaxHeight().background(color,CircleShape))};Text(value.toString(),color=JoltPalette.textPrimary,fontSize=10.sp,modifier=Modifier.padding(start=6.dp))}}

@Composable
private fun MuseumRoom(
    g: GameState,
    vm: GameViewModel,
    back: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        EnvironmentVisual(EnvironmentScene.TEMPLE)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .34f)),
        )
        TopBack(title = stringResource(R.string.museum_egypt), back = back)
        Text(
            text = "${(g.museumProgress * 100).toInt()}%",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(22.dp),
            color = JoltPalette.goldAccent,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 38.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            g.artifacts.forEach { artifact ->
                Column(
                    modifier = Modifier.width(130.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .background(JoltPalette.surface.copy(alpha = .72f), RoundedCornerShape(18.dp))
                            .border(2.dp, JoltPalette.goldAccent.copy(alpha = .45f), RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArtifactVisual(
                            id = artifact.id,
                            discovered = artifact.isCompleted,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = if (artifact.isCompleted) keyText(artifact.nameKey) else "???",
                        color = if (artifact.isCompleted) JoltPalette.textPrimary else JoltPalette.textMuted,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${artifact.fragmentsOwned}/${artifact.fragmentsRequired}",
                        color = JoltPalette.goldAccent,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StorySceneOverlay(
    scene: StoryScene,
    environment: EnvironmentScene,
    finish: () -> Unit,
) {
    var index by remember(scene.id) { mutableIntStateOf(0) }
    val line = scene.lines[index]
    val heroId = speakerHero(line.speakerKey)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (index < scene.lines.lastIndex) index++ else finish()
            },
    ) {
        EnvironmentVisual(environment)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = .18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = .28f),
                        ),
                    ),
                ),
        )
        AnimatedVisibility(
            visible = heroId != null,
            enter = fadeIn() + slideInHorizontally { it / 4 },
        ) {
            if (heroId != null) {
                HeroVisual(
                    heroId = heroId,
                    modifier = Modifier
                        .fillMaxHeight(.88f)
                        .width(380.dp)
                        .padding(start = 30.dp),
                    facingRight = true,
                    portrait = true,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xF20A0C12)),
                    ),
                )
                .padding(start = 42.dp, end = 42.dp, top = 48.dp, bottom = 20.dp),
        ) {
            Text(
                text = keyText(line.speakerKey),
                color = JoltPalette.goldAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = keyText(line.textKey),
                modifier = Modifier.widthIn(max = 720.dp),
                color = JoltPalette.textPrimary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "▼",
                modifier = Modifier.align(Alignment.End),
                color = JoltPalette.temporalBlue,
            )
        }
    }
}

@Composable
private fun TeamPanel(
    g: GameState,
    m: Mission,
    close: () -> Unit,
    start: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .65f))
            .clickable(onClick = close),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF24202A), Color(0xFF172431))),
                    RoundedCornerShape(28.dp),
                )
                .border(2.dp, JoltPalette.goldAccent.copy(alpha = .6f), RoundedCornerShape(28.dp))
                .clickable(enabled = false) {}
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = keyText(m.titleKey),
                color = JoltPalette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = keyText("objective_${m.objective.name.lowercase()}"),
                color = JoltPalette.textSecondary,
            )
            Row(
                modifier = Modifier.padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                g.selectedTeam.mapNotNull { id -> g.heroes.firstOrNull { it.id == id } }
                    .forEach { hero ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HeroVisual(hero.id, Modifier.size(95.dp))
                            Text(
                                text = keyText(hero.nameKey),
                                color = JoltPalette.textPrimary,
                                fontSize = 11.sp,
                            )
                        }
                    }
            }
            GameButton(label = stringResource(R.string.start_mission), onClick = start, enabled = true)
        }
    }
}

@Composable
private fun ActionBattleScreen(
    g: GameState,
    vm: GameViewModel,
) {
    val mission = GameContent.missions.first { it.id == g.activeMissionId }
    val heroes = g.selectedTeam.mapNotNull { id -> g.heroes.firstOrNull { it.id == id } }
    val labels = GameBattleView.Labels(
        attack = stringResource(R.string.control_attack),
        skill = stringResource(R.string.control_skill),
        ultimate = stringResource(R.string.control_ultimate),
        switch = stringResource(R.string.control_switch),
        objective = keyText(mission.titleKey),
        victory = stringResource(R.string.victory),
        defeat = stringResource(R.string.defeat),
        bossName = stringResource(R.string.enemy_apophis_echo),
        bossPhase = stringResource(R.string.boss_phase),
    )
    AndroidView(
        factory = { context ->
            GameBattleView(context, mission, heroes, g.artifacts, labels, vm::battleEnded)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ResultScreen(
    victory: Boolean,
    g: GameState,
    continueAction: () -> Unit,
) {
    val mission = g.activeMissionId?.let { id -> GameContent.missions.firstOrNull { it.id == id } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0B0D13)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (victory) stringResource(R.string.victory) else stringResource(R.string.defeat),
                color = if (victory) JoltPalette.goldAccent else JoltPalette.danger,
                fontSize = 45.sp,
                fontWeight = FontWeight.Black,
            )
            if (victory) {
                Text(text = "★ ★ ★", color = JoltPalette.goldAccent, fontSize = 38.sp)
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    RewardOrb("XP", mission?.rewardXp ?: 0, JoltPalette.temporalBlue)
                    RewardOrb("◆", mission?.rewardMaterial ?: 0, JoltPalette.goldAccent)
                    RewardOrb("◈", 10, JoltPalette.success)
                }
            }
            GameButton(
                label = if (victory) stringResource(R.string.continue_label) else stringResource(R.string.retry),
                onClick = continueAction,
                enabled = true,
            )
        }
    }
}

@Composable
private fun RewardOrb(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color.copy(alpha = .18f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, color = color, fontWeight = FontWeight.Black)
        }
        Text(text = "+$value", color = JoltPalette.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArtifactReveal(
    a: Artifact,
    close: () -> Unit,
) {
    val palette = JoltPalette
    Box(modifier = Modifier.fillMaxSize()) {
        EnvironmentVisual(EnvironmentScene.TEMPLE)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f)))
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .background(palette.surface.copy(alpha = .8f), RoundedCornerShape(28.dp))
                    .border(2.dp, palette.goldAccent, RoundedCornerShape(28.dp))
                    .padding(18.dp),
            ) {
                ArtifactVisual(a.id, discovered = true, modifier = Modifier.fillMaxSize())
            }
            Text(a.rarity.name, color = JoltPalette.goldAccent, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(keyText(a.nameKey), color = JoltPalette.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(
                keyText(a.descriptionKey),
                Modifier.width(480.dp).padding(8.dp),
                color = JoltPalette.textSecondary,
                textAlign = TextAlign.Center,
            )
            Text(keyText(a.effectKey), color = JoltPalette.temporalBlue, fontWeight = FontWeight.Bold)
            GameButton(stringResource(R.string.add_to_museum), close, true)
        }
    }
}

@Composable
private fun ChapterCelebration() {
    var visible by remember { mutableStateOf(true) }
    if (visible) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xE80A0D12)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("★", color = JoltPalette.goldAccent, fontSize = 80.sp)
                Text(stringResource(R.string.egypt_restored), color = JoltPalette.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(
                    stringResource(R.string.egypt_restored_body),
                    Modifier.width(520.dp),
                    color = JoltPalette.textSecondary,
                    textAlign = TextAlign.Center,
                )
                GameButton(stringResource(R.string.return_archive), { visible = false }, true)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    vm: GameViewModel,
    back: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        EnvironmentVisual(EnvironmentScene.ARCHIVE)
        TopBack(stringResource(R.string.settings), back)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(440.dp)
                .background(JoltPalette.surface.copy(alpha = .92f), RoundedCornerShape(26.dp))
                .border(1.dp, JoltPalette.goldAccent.copy(alpha = .5f), RoundedCornerShape(26.dp))
                .padding(24.dp),
        ) {
            Text("JOLT TIME", color = JoltPalette.goldAccent, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(
                text = stringResource(R.string.language),
                color = JoltPalette.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val activity = LocalContext.current as? Activity
                GameButton(stringResource(R.string.ukrainian), {
                    vm.language("uk")
                    applyAppLanguage(activity, "uk")
                }, true)
                GameButton(stringResource(R.string.english), {
                    vm.language("en")
                    applyAppLanguage(activity, "en")
                }, true)
            }
            BuildRow(stringResource(R.string.app_version), BuildConfig.VERSION_NAME)
            BuildRow(stringResource(R.string.build_number), BuildConfig.VERSION_CODE.toString())
            BuildRow(stringResource(R.string.git_commit), BuildConfig.GIT_COMMIT)
            BuildRow(stringResource(R.string.build_type), BuildConfig.BUILD_TYPE.uppercase())
            TextButton(onClick = vm::reset, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.reset_progress), color = JoltPalette.danger)
            }
        }
    }
}

@Composable
private fun BuildRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = JoltPalette.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, color = JoltPalette.textPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopBack(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).background(JoltPalette.surface.copy(alpha = .85f), CircleShape).clickable(onClick = back),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = JoltPalette.textPrimary, fontSize = 34.sp)
        }
        Text(
            title.uppercase(),
            Modifier.padding(start = 12.dp),
            color = JoltPalette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun GameButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(JoltPalette.goldAccent, Color(0xFFF29D42)))
    } else {
        Brush.horizontalGradient(listOf(Color.DarkGray, Color.Gray))
    }
    Box(
        modifier = Modifier
            .padding(top = 13.dp)
            .heightIn(min = 48.dp)
            .background(brush, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 25.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), color = Color(0xFF21170B), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun keyText(key: String): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(key, "string", context.packageName)
    return if (id == 0) key else stringResource(id)
}

private fun Hero.abilities() = listOf(basic, skill, ultimate)

private fun speakerHero(key: String) = when (key) {
    "speaker_orian" -> "orian"
    "speaker_nefer" -> "nefer"
    "speaker_nadiya" -> "nadiya"
    else -> null
}

private fun sceneForMission(index: Int) = when (index) {
    0 -> EnvironmentScene.ARCHIVE
    1 -> EnvironmentScene.NILE
    2 -> EnvironmentScene.MEMPHIS
    3 -> EnvironmentScene.DESERT
    4 -> EnvironmentScene.NECROPOLIS
    5 -> EnvironmentScene.TEMPLE
    else -> EnvironmentScene.PYRAMID
}

@Suppress("DEPRECATION")
private fun applyAppLanguage(activity: Activity?, tag: String) {
    activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(tag)
    } else {
        val locale = java.util.Locale.forLanguageTag(tag)
        java.util.Locale.setDefault(locale)
        val configuration = activity.resources.configuration
        configuration.setLocale(locale)
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
        activity.recreate()
    }
}
