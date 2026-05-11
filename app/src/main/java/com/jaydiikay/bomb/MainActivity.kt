package com.jaydiikay.bomb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaydiikay.bomb.ui.GameViewModel
import com.jaydiikay.bomb.ui.screens.*
import com.jaydiikay.bomb.ui.theme.BombTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BombTheme {
                val navController = rememberNavController()
                // Shared ViewModel across screens
                val gameViewModel: GameViewModel = viewModel()
                NavHost(navController, startDestination = "auth") {
                    composable("auth") { AuthScreen(navController) }
                    composable("setup") { SetupScreen(navController, gameViewModel) }
                    composable("game") { GameScreen(navController, gameViewModel) }
                    composable("scores") { ScoreScreen(navController, gameViewModel) }
                }
            }
        }
    }
}
