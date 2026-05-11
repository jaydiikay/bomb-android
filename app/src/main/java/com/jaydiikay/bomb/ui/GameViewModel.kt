package com.jaydiikay.bomb.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaydiikay.bomb.game.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()
    private var botJob: Job? = null

    fun startGame(players: List<PlayerConfig>) {
        botJob?.cancel()
        _gameState.value = GameLogic.createGame(players)
        scheduleBotTurn()
    }

    // Called when a human taps "Reveal Hand" on the pass-and-play screen
    fun revealHand() {
        val state = _gameState.value ?: return
        if (state.phase != GamePhase.PASS_AND_PLAY) return
        _gameState.value = state.copy(phase = GamePhase.PLAYING)
        scheduleBotTurn()
    }

    fun playCard(card: Card) {
        val state = _gameState.value ?: return
        _gameState.value = GameLogic.playCard(state, card)
        scheduleBotTurn()
    }

    fun playPair(second: Card) {
        val state = _gameState.value ?: return
        _gameState.value = GameLogic.playPair(state, second)
        scheduleBotTurn()
    }

    fun cancelSecond() {
        val state = _gameState.value ?: return
        _gameState.value = GameLogic.cancelSecond(state)
        scheduleBotTurn()
    }

    fun drawCard() {
        val state = _gameState.value ?: return
        _gameState.value = GameLogic.drawCard(state)
        scheduleBotTurn()
    }

    fun endDrawnTurn() {
        val state = _gameState.value ?: return
        _gameState.value = GameLogic.endDrawnTurn(state)
        scheduleBotTurn()
    }

    fun resetGame() {
        botJob?.cancel()
        _gameState.value = null
    }

    // -----------------------------------------------------------------------
    // Bot auto-dispatch with delay
    // -----------------------------------------------------------------------

    private fun scheduleBotTurn() {
        botJob?.cancel()
        val state = _gameState.value ?: return
        val currentPlayer = state.players[state.currentPlayerIndex]
        if (!currentPlayer.isBot) return
        if (state.phase == GamePhase.GAME_OVER || state.phase == GamePhase.BOMB) return

        val action = BotLogic.getAction(state, state.currentPlayerIndex) ?: return
        val isTransitional = state.phase == GamePhase.PASS_AND_PLAY || state.phase == GamePhase.DREW_CARD
        val delayMs = if (isTransitional) 600L else 1500L

        botJob = viewModelScope.launch {
            delay(delayMs)
            when (action) {
                is BotAction.RevealHand -> revealHand()
                is BotAction.DrawCard -> drawCard()
                is BotAction.EndDrawnTurn -> endDrawnTurn()
                is BotAction.CancelSecond -> cancelSecond()
                is BotAction.PlayCard -> playCard(action.card)
                is BotAction.PlayPair -> playPair(action.second)
            }
        }
    }
}
