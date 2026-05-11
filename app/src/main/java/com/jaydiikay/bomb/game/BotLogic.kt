package com.jaydiikay.bomb.game

sealed class BotAction {
    object RevealHand : BotAction()
    object DrawCard : BotAction()
    object EndDrawnTurn : BotAction()
    object CancelSecond : BotAction()
    data class PlayCard(val card: Card) : BotAction()
    data class PlayPair(val second: Card) : BotAction()
}

object BotLogic {
    fun getAction(state: GameState, botIndex: Int): BotAction? {
        if (botIndex != state.currentPlayerIndex) return null

        return when (state.phase) {
            GamePhase.PASS_AND_PLAY -> BotAction.RevealHand
            GamePhase.DREW_CARD -> BotAction.EndDrawnTurn
            GamePhase.PLAYING -> {
                val bot = state.players[botIndex]
                val playable = bot.hand.filter { Rules.canPlay(it, state.topCard, state.pendingDraw) }
                if (playable.isEmpty()) {
                    BotAction.DrawCard
                } else {
                    // Prefer non-special, non-bomb cards; fall back to any playable card
                    val preferred = playable.filter { !Rules.requiresSecondCard(it) && !it.isBomb }
                    val card = if (preferred.isNotEmpty()) preferred.first() else playable.first()
                    BotAction.PlayCard(card)
                }
            }
            GamePhase.AWAITING_SECOND -> {
                val first = state.selectedCard ?: return BotAction.CancelSecond
                val bot = state.players[botIndex]
                val validSeconds = Rules.validSecondCards(first, bot.hand)
                if (validSeconds.isEmpty()) BotAction.CancelSecond
                else BotAction.PlayPair(validSeconds.first())
            }
            else -> null
        }
    }
}
