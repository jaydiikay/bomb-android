package com.jaydiikay.bomb.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaydiikay.bomb.game.Card
import com.jaydiikay.bomb.game.GamePhase
import com.jaydiikay.bomb.game.Rules

@Composable
fun PlayerHand(
    hand: List<Card>,
    topCard: Card,
    pendingDraw: Int,
    phase: GamePhase,
    selectedCard: Card?,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Determine which cards are playable or valid seconds
    val playableCards: Set<String> = when (phase) {
        GamePhase.PLAYING -> hand.filter { Rules.canPlay(it, topCard, pendingDraw) }
            .map { it.id }.toSet()
        GamePhase.AWAITING_SECOND -> {
            if (selectedCard != null) {
                Rules.validSecondCards(selectedCard, hand).map { it.id }.toSet()
            } else emptySet()
        }
        else -> emptySet()
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy((-16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.forEach { card ->
            val isSelected = selectedCard?.id == card.id
            val isPlayable = card.id in playableCards
            CardView(
                card = card,
                onClick = { if (isPlayable || phase == GamePhase.PLAYING) onCardClick(card) },
                selected = isSelected,
                enabled = isPlayable,
                faceDown = false
            )
        }
    }
}
