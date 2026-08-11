package com.jolttime.game.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jolttime.game.JoltTimeApplication
import com.jolttime.game.domain.model.GameState
import com.jolttime.game.game.RpgEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(val game: GameState=GameState(), val loaded:Boolean=false)

class GameViewModel(application: Application): AndroidViewModel(application) {
    private val repo=(application as JoltTimeApplication).repository
    private val _ui=MutableStateFlow(UiState()); val ui=_ui.asStateFlow()
    private val saves=Channel<GameState>(Channel.CONFLATED)
    init {
        viewModelScope.launch { for(state in saves) repo.save(state) }
        viewModelScope.launch { val state=repo.state.first().sanitized(); _ui.value=UiState(state,true); saves.send(state) }
    }
    fun introSeen()=mutate(RpgEngine::markIntroSeen)
    fun selectTeam(ids:List<String>)=mutate { RpgEngine.selectTeam(it,ids) }
    fun startMission(id:String)=mutate { RpgEngine.startMission(it,id) }
    fun act(ability:Int,target:String)=mutate { RpgEngine.battleAction(it,ability,target) }
    fun enemyAct()=mutate(RpgEngine::enemyAction)
    fun claimVictory()=mutate(RpgEngine::claimVictory)
    fun retry()=mutate(RpgEngine::retry)
    fun dismissArtifact()=mutate(RpgEngine::dismissArtifact)
    fun dismissStory()=mutate(RpgEngine::dismissStory)
    fun upgradeHero(id:String)=mutate { RpgEngine.upgradeHero(it,id) }
    fun equip(heroId:String,artifactId:String)=mutate { RpgEngine.equipArtifact(it,heroId,artifactId) }
    fun reset(){ viewModelScope.launch { repo.reset(); val state=GameState(); _ui.value=UiState(state,true); repo.save(state) } }
    private fun mutate(block:(GameState)->GameState){ val next=block(_ui.value.game).sanitized();_ui.value=_ui.value.copy(game=next);saves.trySend(next) }
}
