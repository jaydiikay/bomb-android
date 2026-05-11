package com.jaydiikay.bomb.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jaydiikay.bomb.ui.GameViewModel
import com.jaydiikay.bomb.ui.theme.GreenTable

@Composable
fun SetupScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("bomb_prefs", Context.MODE_PRIVATE)
    val currentUser = prefs.getString("current_user", "Player 1") ?: "Player 1"

    var playerCount by remember { mutableStateOf(2) }
    val playerNames = remember(playerCount) {
        mutableStateListOf<String>().also { list ->
            list.add(currentUser) // first player is logged-in user
            repeat(playerCount - 1) { i -> list.add("Player ${i + 2}") }
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
            Text(
                text = "Game Setup",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Player count selector
                    Text(
                        text = "Number of Players: $playerCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (2..7).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick = {
                                    playerCount = count
                                    // Resize playerNames
                                    while (playerNames.size < count) {
                                        playerNames.add("Player ${playerNames.size + 1}")
                                    }
                                    while (playerNames.size > count) {
                                        playerNames.removeAt(playerNames.size - 1)
                                    }
                                },
                                label = { Text("$count") }
                            )
                        }
                    }

                    Divider()

                    // Name inputs
                    Text(
                        text = "Player Names",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    playerNames.forEachIndexed { index, name ->
                        OutlinedTextField(
                            value = name,
                            onValueChange = { playerNames[index] = it },
                            label = { Text("Player ${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = index != 0 // first player name is from login
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val names = playerNames.map { it.ifBlank { "Player" } }
                            viewModel.startGame(names)
                            navController.navigate("game") {
                                popUpTo("setup") { inclusive = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenTable)
                    ) {
                        Text(
                            text = "Start Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
