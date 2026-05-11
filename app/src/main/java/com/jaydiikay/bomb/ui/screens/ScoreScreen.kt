package com.jaydiikay.bomb.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jaydiikay.bomb.data.AppDatabase
import com.jaydiikay.bomb.data.GameResult
import com.jaydiikay.bomb.game.EndReason
import com.jaydiikay.bomb.game.PlayerScore
import com.jaydiikay.bomb.ui.GameViewModel
import com.jaydiikay.bomb.ui.theme.BombOrange
import com.jaydiikay.bomb.ui.theme.GreenTable
import kotlinx.coroutines.launch

@Composable
fun ScoreScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }

    val scores = gameState?.scores ?: emptyList()
    val endReason = gameState?.endReason
    val isBomb = endReason == EndReason.BOMB

    // Save result to DB once
    LaunchedEffect(scores) {
        if (scores.isNotEmpty() && !saved) {
            saved = true
            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val winner = scores.firstOrNull { it.isWinner }?.player?.name ?: "Unknown"
                val loser = scores.firstOrNull { it.isLoser }?.player?.name ?: "Unknown"
                val playerNames = scores.joinToString(",") { it.player.name }
                val scoresJson = scores.joinToString(",") { "\"${it.player.name}\":${it.score}" }
                db.gameResultDao().insert(
                    GameResult(
                        date = System.currentTimeMillis(),
                        players = playerNames,
                        scores = "{$scoresJson}",
                        winner = winner,
                        loser = loser,
                        endReason = if (isBomb) "bomb" else "normal"
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenTable),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            if (isBomb) {
                Text(
                    text = "💣 BOMB!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BombOrange,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Game Over",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            val winner = scores.firstOrNull { it.isWinner }
            val loser = scores.firstOrNull { it.isLoser }

            winner?.let {
                Text(
                    text = "Winner: ${it.player.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF66BB6A)
                )
            }

            loser?.let {
                Text(
                    text = "Loser: ${it.player.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF5350)
                )
            }

            // Score table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Final Scores",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Player", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("Score", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                        Text("Cards", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                        Text("Result", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    scores.sortedBy { it.score }.forEach { ps ->
                        val rowBg = when {
                            ps.isWinner -> Color(0x3366BB6A)
                            ps.isLoser -> Color(0x33EF5350)
                            else -> Color.Transparent
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg, RoundedCornerShape(4.dp))
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ps.player.name,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ps.score}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                text = "${ps.player.hand.size}",
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                text = when {
                                    ps.isWinner -> "Winner"
                                    ps.isLoser -> "Loser"
                                    else -> "-"
                                },
                                fontSize = 12.sp,
                                color = when {
                                    ps.isWinner -> Color(0xFF388E3C)
                                    ps.isLoser -> Color(0xFFD32F2F)
                                    else -> Color.Gray
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Buttons
            Button(
                onClick = {
                    viewModel.resetGame()
                    navController.navigate("setup") {
                        popUpTo("auth") { inclusive = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenTable)
            ) {
                Text("Play Again", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    viewModel.resetGame()
                    navController.navigate("auth") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Home", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
