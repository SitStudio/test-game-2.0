package com.jolttime.game
import android.app.Application
import com.jolttime.game.data.local.GameStore
import com.jolttime.game.data.repository.GameRepository
class JoltTimeApplication: Application() { val repository by lazy { GameRepository(GameStore(this)) } }
