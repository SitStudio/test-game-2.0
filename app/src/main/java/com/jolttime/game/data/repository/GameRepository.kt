package com.jolttime.game.data.repository

import com.jolttime.game.data.local.GameStore
import com.jolttime.game.domain.model.GameState

class GameRepository(private val store: GameStore) {
    val state = store.state
    suspend fun save(state: GameState) = store.save(state)
    suspend fun reset() = store.reset()
}
