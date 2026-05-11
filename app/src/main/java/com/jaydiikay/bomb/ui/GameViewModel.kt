package com.jaydiikay.bomb.ui

import androidx.lifecycle.ViewModel
import com.jaydiikay.bomb.game.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()

    fun startGame(playerNames: List<String>) {
        _gameState.value = GameLogic.createGame(playerNames)
    }

    fun playCard(card: Card) {
        val state = _gameState.value ?: return
        val newState = GameLogic.playCard(state, card)
        // Consume reverseOnce after direction is applied
        _gameState.value = if (newState.reverseOnce && newState.currentPlayerIndex != state.currentPlayerIndex) {
            GameLogic.consumeReverseOnce(newState)
        } else {
            newState
        }
    }

    fun playPair(first: Card, second: Card) {
        _gameState.value?.let { state ->
            _gameState.value = GameLogic.playPair(state, first, second)
        }
    }

    fun drawCard() {
        _gameState.value?.let { state ->
            _gameState.value = GameLogic.drawCard(state)
        }
    }

    fun selectCard(card: Card) {
        val state = _gameState.value ?: return
        // Validate: must be playable
        if (!Rules.canPlay(card, state.topCard, state.pendingDraw)) return

        val currentPlayer = state.players[state.currentPlayerIndex]
        if (!currentPlayer.hand.any { it.id == card.id }) return

        // For 8/J: check if there are valid second cards
        if (Rules.requiresSecondCard(card)) {
            val handWithoutCard = currentPlayer.hand.filter { it.id != card.id }
            val validSeconds = Rules.validSecondCards(card, handWithoutCard)
            if (validSeconds.isEmpty()) {
                // No valid second card: play the card normally (it will trigger draw logic)
                _gameState.value = GameLogic.playCard(state, card)
            } else {
                // First: play the primary card (moves to AWAITING_SECOND)
                _gameState.value = GameLogic.playCard(state, card)
            }
        } else {
            _gameState.value = GameLogic.playCard(state, card)
        }
    }

    fun resetGame() {
        _gameState.value = null
    }
}
