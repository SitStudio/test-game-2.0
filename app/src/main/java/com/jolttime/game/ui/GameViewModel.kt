package com.jolttime.game.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jolttime.game.JoltTimeApplication
import com.jolttime.game.domain.model.GameState
import com.jolttime.game.game.GameEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class UiState(val game: GameState=GameState(), val loaded:Boolean=false, val offlineReward:Long=0, val showLevelUp:Boolean=false)
class GameViewModel(application: Application): AndroidViewModel(application) {
    private val repo=(application as JoltTimeApplication).repository
    private val _ui=MutableStateFlow(UiState()); val ui=_ui.asStateFlow()
    private val saves = Channel<GameState>(Channel.CONFLATED)
    private val clearBeforeNextSave = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            for (state in saves) {
                if (clearBeforeNextSave.getAndSet(false)) repo.reset()
                repo.save(state)
            }
        }
        viewModelScope.launch {
            val loaded = repo.state.first()
            val (restored, reward) = GameEngine.offlineReward(GameEngine.updateTime(loaded))
            _ui.value = UiState(restored, loaded = true, offlineReward = reward)
            saves.send(restored)
            while (isActive) {
                delay(1_000)
                mutate { GameEngine.passiveTick(GameEngine.updateTime(it)) }
            }
        }
    }
    fun tap(){ val (next,level)=GameEngine.tap(_ui.value.game); set(next, level) }
    fun buy(id:String)=mutate { GameEngine.buy(it,id) }
    fun startExpedition(id:String)=mutate { GameEngine.startExpedition(it,id) }
    fun claimExpedition()=mutate(GameEngine::claimExpedition)
    fun claimDaily()=mutate(GameEngine::daily)
    fun sound(value:Boolean)=mutate { it.copy(soundEnabled=value) }
    fun vibration(value:Boolean)=mutate { it.copy(vibrationEnabled=value) }
    fun dismissOffline(){ _ui.value=_ui.value.copy(offlineReward=0) }
    fun dismissLevel(){ _ui.value=_ui.value.copy(showLevelUp=false) }
    fun reset() {
        val initial = GameState()
        _ui.value = UiState(initial, loaded = true)
        clearBeforeNextSave.set(true)
        persist(initial)
    }
    fun onBackground(){ if (_ui.value.loaded) mutate { it.copy(lastSeenAt=System.currentTimeMillis()) } }
    private fun mutate(block:(GameState)->GameState)=set(block(_ui.value.game))
    private fun set(game:GameState, level:Boolean=false){ _ui.value=_ui.value.copy(game=game.sanitized(),showLevelUp=_ui.value.showLevelUp||level); persist(_ui.value.game) }
    private fun persist(state: GameState){ saves.trySend(state) }
}
