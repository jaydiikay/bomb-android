package com.jaydiikay.bomb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

    // ── Pass-and-play handoff screen ─────────────────────────────────────────
    if (state.phase == GamePhase.PASS_AND_PLAY) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GreenTable),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("Pass the device to", color = Color.White, fontSize = 18.sp)
                Text(
                    text = currentPlayer.name,
                    color = Color(0xFFFFD700),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.revealHand() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Reveal Hand", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // ── Drew-card pause screen (shown to the player who just drew) ────────────
    if (state.phase == GamePhase.DREW_CARD && !currentPlayer.isBot) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GreenTable),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "${currentPlayer.name} drew:",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.drawnCards) { card ->
                        CardView(
                            card = card,
                            onClick = {},
                            selected = false,
                            enabled = false,
                            faceDown = false
                        )
                    }
                }
                if (state.drawnCards.isEmpty()) {
                    Text("(No cards available to draw)", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.endDrawnTurn() },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenTable),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // ── Main game board ───────────────────────────────────────────────────────
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
                    onDrawClick = {
                        if (state.phase == GamePhase.PLAYING) viewModel.drawCard()
                    }
                )
            }

            // Phase hint for AWAITING_SECOND
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

            // Bottom: current player's hand or bot indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x44000000))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentPlayer.isBot) {
                    // Hide bot's actual cards — show a thinking indicator
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentPlayer.name} is thinking…",
                        color = Color(0xFFBBBBBB),
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Show face-down cards to represent the bot's hand
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-16).dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        repeat(minOf(currentPlayer.hand.size, 7)) {
                            CardView(
                                card = currentPlayer.hand[it],
                                onClick = {},
                                selected = false,
                                enabled = false,
                                faceDown = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
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
                                GamePhase.PLAYING -> viewModel.playCard(card)
                                GamePhase.AWAITING_SECOND -> {
                                    val first = state.selectedCard
                                    if (first != null) viewModel.playPair(card)
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        // Draw card button (PLAYING phase)
                        if (state.phase == GamePhase.PLAYING) {
                            Button(
                                onClick = { viewModel.drawCard() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                val label = if (state.pendingDraw > 0) "Draw ${state.pendingDraw} Cards" else "Draw Card"
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Cancel second card (AWAITING_SECOND phase) — draws 1 card instead
                        if (state.phase == GamePhase.AWAITING_SECOND) {
                            Button(
                                onClick = { viewModel.cancelSecond() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Draw 1 Card Instead", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
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
private fun OtherPlayerIndicator(player: Player) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0x55000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (player.isBot) "🤖 ${player.name}" else player.name,
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
