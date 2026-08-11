package com.jolttime.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jolttime.game.ui.GameApp
import com.jolttime.game.ui.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<GameViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GameApp(viewModel) } }
    override fun onStop() { viewModel.onBackground(); super.onStop() }
}
