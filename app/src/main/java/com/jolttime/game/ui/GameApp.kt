package com.jolttime.game.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolttime.game.domain.model.*
import com.jolttime.game.game.GameEngine
import com.jolttime.game.ui.theme.*
import kotlinx.coroutines.delay

private enum class Tab(val title:String){ HOME("Home"),UPGRADES("Upgrades"),MUSEUM("Museum"),EPOCHS("Epochs"),EXPEDITIONS("Expeditions"),SETTINGS("Settings") }

@Composable
fun GameApp(vm: GameViewModel) {
    JoltTheme {
        val ui by vm.ui.collectAsStateWithLifecycle()
        var tab by remember { mutableStateOf(Tab.HOME) }
        var daily by remember { mutableStateOf(false) }

        if (!ui.loaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0E1117)) {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = {
                                    Icon(
                                        imageVector = when (item) {
                                            Tab.HOME -> Icons.Outlined.Bolt
                                            Tab.UPGRADES -> Icons.Outlined.TrendingUp
                                            Tab.MUSEUM -> Icons.Outlined.AccountBalance
                                            Tab.EPOCHS -> Icons.Outlined.Public
                                            Tab.EXPEDITIONS -> Icons.Outlined.Explore
                                            Tab.SETTINGS -> Icons.Outlined.Settings
                                        },
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title.take(5), fontSize = 9.sp) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    when (tab) {
                        Tab.HOME -> Home(ui.game, vm::tap, daily = { daily = true })
                        Tab.UPGRADES -> Upgrades(ui.game, vm::buy)
                        Tab.MUSEUM -> Museum(ui.game)
                        Tab.EPOCHS -> Epochs(ui.game)
                        Tab.EXPEDITIONS -> Expeditions(
                            ui.game,
                            vm::startExpedition,
                            vm::claimExpedition
                        )
                        Tab.SETTINGS -> Settings(ui.game, vm)
                    }
                }
            }

            if (daily) {
                DailyDialog(
                    g = ui.game,
                    close = { daily = false },
                    claim = {
                        vm.claimDaily()
                        daily = false
                    }
                )
            }

            if (ui.offlineReward > 0) {
                AlertDialog(
                    onDismissRequest = vm::dismissOffline,
                    icon = { Icon(Icons.Outlined.NightsStay, contentDescription = null) },
                    title = { Text("Welcome back, Keeper") },
                    text = {
                        Text(
                            "Your Time Engine recovered ${ui.offlineReward} shards " +
                                "while you were away."
                        )
                    },
                    confirmButton = {
                        Button(onClick = vm::dismissOffline) {
                            Text("Collect")
                        }
                    }
                )
            }

            if (ui.showLevelUp) {
                AlertDialog(
                    onDismissRequest = vm::dismissLevel,
                    title = { Text("Timeline expanded") },
                    text = {
                        Text(
                            "You reached level ${ui.game.level}. " +
                                "New history may now be within reach."
                        )
                    },
                    confirmButton = {
                        Button(onClick = vm::dismissLevel) {
                            Text("Continue")
                        }
                    }
                )
            }
        }
    }
}

@Composable private fun Header(kicker:String,title:String,body:String=""){Column(Modifier.padding(horizontal=20.dp,vertical=18.dp)){Text(kicker.uppercase(),color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold);Text(title,fontSize=28.sp,fontWeight=FontWeight.Bold);if(body.isNotEmpty())Text(body,color=Muted,fontSize=14.sp)}}
@Composable
private fun Glass(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = Column(
    modifier = modifier
        .background(Card, RoundedCornerShape(20.dp))
        .border(1.dp, Color(0xFF282E38), RoundedCornerShape(20.dp))
        .padding(16.dp),
    content = content
)
@Composable private fun Home(g:GameState,tap:()->Unit,daily:()->Unit){val haptic=LocalHapticFeedback.current;var pressed by remember{mutableStateOf(false)};var float by remember{mutableIntStateOf(0)};val scale by animateFloatAsState(if(pressed).92f else 1f,label="core")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Header("Keeper level ${g.level}","The Time Core")};IconButton(onClick=daily){Icon(Icons.Outlined.CardGiftcard,"Daily reward",tint=Gold)}}
        Column(Modifier.padding(horizontal=20.dp)){Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("TIME SHARDS",g.timeShards.toString(),Modifier.weight(1f));Stat("COINS",g.coins.toString(),Modifier.weight(1f))};Spacer(Modifier.height(14.dp));Text("XP  ${g.xp} / ${g.xpToNextLevel}",fontSize=12.sp,color=Muted);LinearProgressIndicator(progress={(g.xp/g.xpToNextLevel.toFloat()).coerceIn(0f,1f)},Modifier.fillMaxWidth().padding(top=7.dp).height(7.dp).clip(CircleShape))
            Box(Modifier.fillMaxWidth().height(330.dp),contentAlignment=Alignment.Center){Box(Modifier.size(238.dp).scale(scale).background(Brush.radialGradient(listOf(Color(0xFF39475B),Color(0xFF171D27),Color(0xFF0C0F14))),CircleShape).border(1.dp,Gold.copy(.55f),CircleShape).clickable{pressed=true;float++;tap();if(g.vibrationEnabled)haptic.performHapticFeedback(HapticFeedbackType.LongPress)},contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.HourglassTop,null,tint=Gold,modifier=Modifier.size(50.dp));Text("RESTORE",fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text("+${g.tapPower} shards",color=Muted,fontSize=12.sp)}};LaunchedEffect(pressed){if(pressed){delay(100);pressed=false}};AnimatedContent(float,label="float"){if(it>0)Text("+${g.tapPower}",color=Gold,fontWeight=FontWeight.Bold,modifier=Modifier.offset(y=(-135).dp))}}
            Glass(Modifier.fillMaxWidth()){Text("TIME ENGINE",fontSize=11.sp,color=Muted);Text("${g.passiveIncomePerSecond} shards / second",fontSize=18.sp,fontWeight=FontWeight.SemiBold);Text("Offline recovery is capped at 6 hours.",color=Muted,fontSize=12.sp)}}}}
@Composable private fun Stat(label:String,value:String,modifier:Modifier){Glass(modifier){Text(label,fontSize=10.sp,color=Muted);Text(value,fontSize=22.sp,fontWeight=FontWeight.Bold)}}

@Composable private fun Upgrades(g:GameState,buy:(String)->Unit){Column{Header("Workshop","Upgrades","Invest coins in permanent timeline infrastructure.");LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(g.upgrades,key={it.id}){u->Glass(Modifier.fillMaxWidth()){Row{Column(Modifier.weight(1f)){Text(u.name,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(u.description,color=Muted,fontSize=13.sp);Text("Level ${u.level} / ${u.maxLevel}",color=Gold,fontSize=12.sp)};Button(onClick={buy(u.id)},enabled=g.coins>=u.cost()&&u.level<u.maxLevel){Text(if(u.level==u.maxLevel)"MAX" else "${u.cost()} ◉")}}}}}}}
@Composable private fun Museum(g:GameState){var selected by remember{mutableStateOf(g.unlockedEpochs.firstOrNull()?:"egypt")};Column{Header("Archive","Museum","${g.completedArtifacts} of ${g.artifacts.size} artifacts restored");ScrollableTabRow(selectedTabIndex=GameContent.epochs.indexOfFirst{it.id==selected}.coerceAtLeast(0),containerColor=Ink,edgePadding=16.dp){GameContent.epochs.forEach{Tab(selected=selected==it.id,onClick={selected=it.id},enabled=it.id in g.unlockedEpochs,text={Text(it.name.substringAfter("Ancient "))})}};LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(g.artifacts.filter{it.civilization==selected}){a->ArtifactCard(a)}}}}
@Composable private fun ArtifactCard(a:Artifact){val rarityColor=when(a.rarity){Rarity.COMMON->Muted;Rarity.RARE->Color(0xFF76A7D1);Rarity.EPIC->Color(0xFFA58BD4);Rarity.LEGENDARY->Gold;Rarity.MYTHIC->Color(0xFFD18484)};Glass(Modifier.fillMaxWidth()){Row{Box(Modifier.size(48.dp).background(rarityColor.copy(.12f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(if(a.isCompleted)Icons.Outlined.AutoAwesome else Icons.Outlined.Lock,null,tint=rarityColor)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(a.name,fontWeight=FontWeight.Bold);Text(a.rarity.name,color=rarityColor,fontSize=10.sp);Text(a.description,color=Muted,fontSize=12.sp);LinearProgressIndicator(progress={(a.fragmentsOwned/a.fragmentsRequired.coerceAtLeast(1).toFloat()).coerceIn(0f,1f)},Modifier.fillMaxWidth().padding(top=8.dp));Text("${a.fragmentsOwned.coerceAtLeast(0)} / ${a.fragmentsRequired.coerceAtLeast(1)} fragments",fontSize=11.sp,color=Muted)}}}}
@Composable private fun Epochs(g:GameState){Column{Header("Timeline","Epochs","Level up to restore five chapters of human history.");LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(GameContent.epochs){e->val unlocked=e.id in g.unlockedEpochs;val arts=g.artifacts.filter{it.civilization==e.id};val progress=arts.count{it.isCompleted}/5f;Glass(Modifier.fillMaxWidth()){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(unlocked)Icons.Outlined.Public else Icons.Outlined.Lock,null,tint=Color(e.accent));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(e.name,fontWeight=FontWeight.Bold);Text(e.description,color=Muted,fontSize=12.sp);Text(if(unlocked)"${(progress*100).toInt()}% collection restored" else "Unlocks at level ${e.unlockLevel}",color=if(unlocked)Gold else Muted,fontSize=12.sp);LinearProgressIndicator(progress={if(unlocked)progress else (g.level/e.unlockLevel.toFloat()).coerceAtMost(1f)},Modifier.fillMaxWidth().padding(top=8.dp))}}}}}}}
@Composable private fun Expeditions(g:GameState,start:(String)->Unit,claim:()->Unit){var now by remember{mutableLongStateOf(System.currentTimeMillis())};LaunchedEffect(Unit){while(true){now=System.currentTimeMillis();delay(1000)}};Column{Header("Field work","Expeditions","Send a team on a 30-second search for coins, XP and fragments.");LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(GameContent.epochs.filter{it.id in g.unlockedEpochs}){e->val active=g.expedition.civilizationId==e.id;val remaining=((g.expedition.endsAt-now)/1000).coerceAtLeast(0);Glass(Modifier.fillMaxWidth()){Text(e.name,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("Reward: 75 coins · 30 XP · 1 fragment",color=Muted,fontSize=12.sp);Spacer(Modifier.height(12.dp));when{active&&g.expedition.status==ExpeditionStatus.COMPLETED->Button(onClick=claim,Modifier.fillMaxWidth()){Text("Claim findings")};active->Column{LinearProgressIndicator(progress={(1-(remaining/30f)).coerceIn(0f,1f)},Modifier.fillMaxWidth());Text("Team returns in ${remaining}s",color=Gold,modifier=Modifier.padding(top=8.dp))};else->Button(onClick={start(e.id)},enabled=g.expedition.status==ExpeditionStatus.IDLE,modifier=Modifier.fillMaxWidth()){Text("Start expedition")}}}}}}}
@Composable private fun Settings(g:GameState,vm:GameViewModel){var confirm by remember{mutableStateOf(false)};Column{Header("Local profile","Settings","Your game is stored privately on this device.");Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Glass(Modifier.fillMaxWidth()){SettingSwitch("Sound effects","Reserved for future sound assets",g.soundEnabled,vm::sound);HorizontalDivider(Modifier.padding(vertical=10.dp));SettingSwitch("Haptic feedback","Vibrate when the Time Core is tapped",g.vibrationEnabled,vm::vibration)};Glass(Modifier.fillMaxWidth()){Text("Jolt Time",fontWeight=FontWeight.Bold);Text("Version 1.0.0 · Offline-first",color=Muted,fontSize=12.sp);Spacer(Modifier.height(14.dp));OutlinedButton(onClick={confirm=true},Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Reset all progress")}}}};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Reset timeline?")},text={Text("All currencies, artifacts and upgrades will be permanently erased.")},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}},confirmButton={TextButton(onClick={vm.reset();confirm=false}){Text("Reset")}})}
@Composable private fun SettingSwitch(title:String,body:String,checked:Boolean,on:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(body,color=Muted,fontSize=12.sp)};Switch(checked,onCheckedChange=on)}}
@Composable private fun DailyDialog(g:GameState,close:()->Unit,claim:()->Unit){val canClaim=GameEngine.canClaimDaily(g);AlertDialog(onDismissRequest=close,title={Text("Daily chronology")},text={Column{Text("Return each day to stabilize the timeline.",color=Muted);Spacer(Modifier.height(14.dp));LazyVerticalGrid(columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),modifier=Modifier.height(150.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(7){i->val reached=i<g.dailyDay||!canClaim&&i==g.dailyDay-1;Box(Modifier.background(if(reached)Gold.copy(.18f) else Card,RoundedCornerShape(12.dp)).padding(8.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("D${i+1}",fontWeight=FontWeight.Bold);Text("${50*(i+1)}",fontSize=10.sp,color=Gold)}}}}}},dismissButton={TextButton(onClick=close){Text("Later")}},confirmButton={Button(onClick=claim,enabled=canClaim){Text(if(!canClaim)"Already claimed" else "Claim day ${g.dailyDay+1}")}})}
