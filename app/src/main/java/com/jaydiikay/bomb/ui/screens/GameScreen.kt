package com.jaydiikay.bomb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jaydiikay.bomb.game.*
import com.jaydiikay.bomb.ui.GameViewModel
import com.jaydiikay.bomb.ui.components.*
import com.jaydiikay.bomb.ui.theme.BombOrange
import com.jaydiikay.bomb.ui.theme.GreenTable

@Composable
fun GameScreen(navController: NavController, viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    var showBombAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(gameState?.phase) {
        when (gameState?.phase) {
            GamePhase.BOMB -> showBombAnimation = true
            GamePhase.GAME_OVER -> {
                navController.navigate("scores") {
                    popUpTo("game") { inclusive = true }
                }
            }
            else -> {}
        }
    }

    val state = gameState
    if (state == null) {
        Box(modifier = Modifier.fillMaxSize().background(GreenTable), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val currentPlayer = state.players[state.currentPlayerIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenTable)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Turn indicator
            TurnIndicator(
                playerName = currentPlayer.name,
                direction = state.direction,
                pendingDraw = state.pendingDraw,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Other players' card counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.players.forEachIndexed { index, player ->
                    if (index != state.currentPlayerIndex) {
                        OtherPlayerIndicator(player = player)
                    }
                }
            }

            // Center: Draw + Discard piles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DrawDiscardPile(
                    topCard = state.topCard,
                    drawPileCount = state.drawPile.size,
                    onDrawClick = { viewModel.drawCard() }
                )
            }

            // Phase hint
            if (state.phase == GamePhase.AWAITING_SECOND) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC000000))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Play a second card (same suit or rank as ${state.selectedCard?.rank?.display}${state.selectedCard?.suitSymbol})",
                        color = BombOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom: Current player's hand
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x44000000))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentPlayer.name}'s hand (${currentPlayer.hand.size} cards)",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                PlayerHand(
                    hand = currentPlayer.hand,
                    topCard = state.topCard,
                    pendingDraw = state.pendingDraw,
                    phase = state.phase,
                    selectedCard = state.selectedCard,
                    onCardClick = { card ->
                        when (state.phase) {
                            GamePhase.PLAYING -> {
                                if (Rules.requiresSecondCard(card)) {
                                    viewModel.selectCard(card)
                                } else {
                                    viewModel.playCard(card)
                                }
                            }
                            GamePhase.AWAITING_SECOND -> {
                                val first = state.selectedCard
                                if (first != null) {
                                    viewModel.playPair(first, card)
                                }
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Draw card button
                Button(
                    onClick = { viewModel.drawCard() },
                    enabled = state.phase == GamePhase.PLAYING,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    val label = if (state.pendingDraw > 0) "Draw ${state.pendingDraw} Cards" else "Draw Card"
                    Text(label, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bomb animation overlay
        if (showBombAnimation) {
            BombAnimation(onAnimationEnd = {
                showBombAnimation = false
                navController.navigate("scores") {
                    popUpTo("game") { inclusive = true }
                }
            })
        }
    }
}

@Composable
private fun OtherPlayerIndicator(player: com.jaydiikay.bomb.game.Player) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0x55000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = player.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(minOf(player.hand.size, 5)) {
                Box(
                    modifier = Modifier
                        .size(12.dp, 18.dp)
                        .background(Color(0xFF1565C0), RoundedCornerShape(2.dp))
                )
            }
            if (player.hand.size > 5) {
                Text(
                    text = "+${player.hand.size - 5}",
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
        Text(
            text = "${player.hand.size} cards",
            color = Color(0xFFBBBBBB),
            fontSize = 10.sp
        )
    }
}
