package com.jaydiikay.bomb.game

import com.jaydiikay.bomb.game.Deck.shuffled

data class Player(
    val id: Int,
    val name: String,
    val hand: MutableList<Card> = mutableListOf()
)

enum class GamePhase { PLAYING, AWAITING_SECOND, BOMB, GAME_OVER }
enum class EndReason { NORMAL, BOMB }

data class PlayerScore(
    val player: Player,
    val score: Int,
    val isWinner: Boolean,
    val isLoser: Boolean
)

data class GameState(
    val players: List<Player>,
    val drawPile: MutableList<Card>,
    val discardPile: MutableList<Card>,
    val topCard: Card,
    val currentPlayerIndex: Int = 0,
    val direction: Int = 1,        // 1 = anti-clockwise (index+1), -1 = clockwise (index-1)
    val reverseOnce: Boolean = false,
    val pendingDraw: Int = 0,
    val phase: GamePhase = GamePhase.PLAYING,
    val selectedCard: Card? = null,
    val endReason: EndReason? = null,
    val scores: List<PlayerScore> = emptyList(),
    val winnerIndex: Int? = null
)

object GameLogic {

    // -----------------------------------------------------------------------
    // Game creation
    // -----------------------------------------------------------------------

    fun createGame(playerNames: List<String>): GameState {
        val deck = Deck.create().toMutableList().also { it.shuffle() }

        // Deal 7 cards to each player
        val players = playerNames.mapIndexed { i, name ->
            val hand = mutableListOf<Card>()
            repeat(7) { hand.add(deck.removeFirst()) }
            Player(i, name, hand)
        }

        // Ensure starting card is not special
        var topCard: Card
        do {
            topCard = deck.removeFirst()
        } while (
            topCard.rank in listOf(Rank.TWO, Rank.FOUR, Rank.EIGHT, Rank.JACK) || topCard.isBomb
        )

        val discardPile = mutableListOf(topCard)
        return GameState(
            players = players,
            drawPile = deck,
            discardPile = discardPile,
            topCard = topCard
        )
    }

    // -----------------------------------------------------------------------
    // Play a single card
    // -----------------------------------------------------------------------

    fun playCard(state: GameState, card: Card): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val currentPlayer = state.players[state.currentPlayerIndex]

        // Validate the play
        if (!Rules.canPlay(card, state.topCard, state.pendingDraw)) return state
        if (!currentPlayer.hand.any { it.id == card.id }) return state

        // Remove card from hand
        val newHand = currentPlayer.hand.toMutableList()
        newHand.removeIf { it.id == card.id }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = state.players.toMutableList()
        newPlayers[state.currentPlayerIndex] = updatedPlayer

        // Add card to discard pile
        val newDiscard = state.discardPile.toMutableList()
        newDiscard.add(card)

        // Check for bomb
        if (card.isBomb) {
            // Check if it is the last card (hand now empty)
            val isLastCard = newHand.isEmpty()
            val endReason = EndReason.BOMB
            val winnerIdx = if (isLastCard) state.currentPlayerIndex else null
            val scores = calculateScores(
                state.copy(players = newPlayers, discardPile = newDiscard, topCard = card),
                endReason,
                winnerIdx
            )
            return state.copy(
                players = newPlayers,
                discardPile = newDiscard,
                topCard = card,
                phase = GamePhase.BOMB,
                endReason = endReason,
                scores = scores,
                winnerIndex = winnerIdx
            )
        }

        // Check for normal win (hand empty)
        if (newHand.isEmpty()) {
            val scores = calculateScores(
                state.copy(players = newPlayers, discardPile = newDiscard, topCard = card),
                EndReason.NORMAL,
                state.currentPlayerIndex
            )
            return state.copy(
                players = newPlayers,
                discardPile = newDiscard,
                topCard = card,
                phase = GamePhase.GAME_OVER,
                endReason = EndReason.NORMAL,
                scores = scores,
                winnerIndex = state.currentPlayerIndex
            )
        }

        // Apply card effects
        var newPendingDraw = state.pendingDraw
        var newDirection = state.direction
        var newReverseOnce = state.reverseOnce
        var newPhase = GamePhase.PLAYING

        when (card.rank) {
            Rank.TWO -> {
                newPendingDraw += 2
            }
            Rank.FOUR -> {
                // Reverse direction for one turn
                if (state.players.size == 2) {
                    // With 2 players: current player goes again (skip next)
                    newReverseOnce = true
                } else {
                    newReverseOnce = true
                }
            }
            Rank.EIGHT, Rank.JACK -> {
                // Requires a second card
                val validSeconds = Rules.validSecondCards(card, newHand)
                if (validSeconds.isEmpty()) {
                    // Must draw if no valid second card
                    val withDraw = drawCardForCurrentPlayer(
                        state.copy(
                            players = newPlayers,
                            discardPile = newDiscard,
                            topCard = card
                        )
                    )
                    val nextIdx = nextTurn(withDraw)
                    return withDraw.copy(currentPlayerIndex = nextIdx, phase = GamePhase.PLAYING)
                } else {
                    newPhase = GamePhase.AWAITING_SECOND
                    return state.copy(
                        players = newPlayers,
                        discardPile = newDiscard,
                        topCard = card,
                        phase = GamePhase.AWAITING_SECOND,
                        selectedCard = card,
                        pendingDraw = newPendingDraw,
                        direction = newDirection,
                        reverseOnce = newReverseOnce
                    )
                }
            }
            else -> { /* no special effect */ }
        }

        // Advance turn
        val tempState = state.copy(
            players = newPlayers,
            discardPile = newDiscard,
            topCard = card,
            pendingDraw = newPendingDraw,
            direction = newDirection,
            reverseOnce = newReverseOnce,
            phase = newPhase
        )
        val nextIdx = nextTurn(tempState)
        var finalState = tempState.copy(currentPlayerIndex = nextIdx)

        // If next player has a pendingDraw and cannot stack, they auto-draw
        // (handled in drawCard call from UI)
        return finalState
    }

    // -----------------------------------------------------------------------
    // Play a pair (for 8 / J)
    // -----------------------------------------------------------------------

    fun playPair(state: GameState, first: Card, second: Card): GameState {
        if (state.phase != GamePhase.AWAITING_SECOND) return state

        val currentPlayer = state.players[state.currentPlayerIndex]

        // Validate second card
        if (!Rules.validSecondCards(first, currentPlayer.hand).any { it.id == second.id }) {
            return state
        }

        // Remove second card from hand
        val newHand = currentPlayer.hand.toMutableList()
        newHand.removeIf { it.id == second.id }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = state.players.toMutableList()
        newPlayers[state.currentPlayerIndex] = updatedPlayer

        // Add second card to discard pile
        val newDiscard = state.discardPile.toMutableList()
        newDiscard.add(second)

        // Check for normal win
        if (newHand.isEmpty()) {
            val scores = calculateScores(
                state.copy(players = newPlayers, discardPile = newDiscard, topCard = second),
                EndReason.NORMAL,
                state.currentPlayerIndex
            )
            return state.copy(
                players = newPlayers,
                discardPile = newDiscard,
                topCard = second,
                phase = GamePhase.GAME_OVER,
                endReason = EndReason.NORMAL,
                scores = scores,
                winnerIndex = state.currentPlayerIndex
            )
        }

        val tempState = state.copy(
            players = newPlayers,
            discardPile = newDiscard,
            topCard = second,
            phase = GamePhase.PLAYING,
            selectedCard = null
        )
        val nextIdx = nextTurn(tempState)
        return tempState.copy(currentPlayerIndex = nextIdx)
    }

    // -----------------------------------------------------------------------
    // Draw a card (player action)
    // -----------------------------------------------------------------------

    fun drawCard(state: GameState): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        val drawCount = if (state.pendingDraw > 0) state.pendingDraw else 1

        var workingState = state
        val newHand = currentPlayer.hand.toMutableList()

        repeat(drawCount) {
            workingState = ensureDrawPile(workingState)
            if (workingState.drawPile.isNotEmpty()) {
                newHand.add(workingState.drawPile.removeFirst())
            }
        }

        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = workingState.players.toMutableList()
        newPlayers[workingState.currentPlayerIndex] = updatedPlayer

        val tempState = workingState.copy(
            players = newPlayers,
            pendingDraw = 0,
            phase = GamePhase.PLAYING
        )
        val nextIdx = nextTurn(tempState)
        return tempState.copy(currentPlayerIndex = nextIdx)
    }

    // -----------------------------------------------------------------------
    // Internal: draw cards for current player without advancing turn
    // -----------------------------------------------------------------------

    private fun drawCardForCurrentPlayer(state: GameState): GameState {
        val currentPlayer = state.players[state.currentPlayerIndex]
        var workingState = ensureDrawPile(state)
        val newHand = currentPlayer.hand.toMutableList()
        if (workingState.drawPile.isNotEmpty()) {
            newHand.add(workingState.drawPile.removeFirst())
        }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = workingState.players.toMutableList()
        newPlayers[workingState.currentPlayerIndex] = updatedPlayer
        return workingState.copy(players = newPlayers)
    }

    // -----------------------------------------------------------------------
    // Compute next player index
    // -----------------------------------------------------------------------

    fun nextTurn(state: GameState): Int {
        val n = state.players.size
        return if (state.reverseOnce) {
            // For card 4: one turn clockwise (index - 1), then back to anti-clockwise
            // The reverseOnce flag is consumed here
            val clockwiseNext = ((state.currentPlayerIndex - 1) + n) % n
            clockwiseNext
        } else {
            // Anti-clockwise: index + 1
            (state.currentPlayerIndex + 1) % n
        }
    }

    // After direction reversal is applied, clear the flag
    fun consumeReverseOnce(state: GameState): GameState {
        return if (state.reverseOnce) state.copy(reverseOnce = false) else state
    }

    // -----------------------------------------------------------------------
    // Scoring
    // -----------------------------------------------------------------------

    private fun calculateScores(
        state: GameState,
        endReason: EndReason,
        winnerIndex: Int?
    ): List<PlayerScore> {
        val scores = state.players.map { player ->
            Scoring.scoreHand(player.hand)
        }

        return when (endReason) {
            EndReason.NORMAL -> {
                // Normal win: first to empty hand wins; highest scorer loses
                val maxScore = scores.max()
                state.players.mapIndexed { i, player ->
                    val isWinner = i == winnerIndex
                    val isLoser = !isWinner && scores[i] == maxScore &&
                            scores.count { it == maxScore } == 1 ||
                            (!isWinner && scores[i] == maxScore)
                    PlayerScore(
                        player = player,
                        score = scores[i],
                        isWinner = isWinner,
                        isLoser = !isWinner && scores[i] == maxScore
                    )
                }
            }
            EndReason.BOMB -> {
                if (winnerIndex != null) {
                    // Bomb as last card: that player wins; highest scorer among rest loses
                    val otherScores = scores.filterIndexed { i, _ -> i != winnerIndex }
                    val maxOtherScore = if (otherScores.isNotEmpty()) otherScores.max() else 0
                    state.players.mapIndexed { i, player ->
                        val isWinner = i == winnerIndex
                        val isLoser = !isWinner && scores[i] == maxOtherScore
                        PlayerScore(
                            player = player,
                            score = scores[i],
                            isWinner = isWinner,
                            isLoser = isLoser
                        )
                    }
                } else {
                    // Bomb mid-game: lowest scorer wins, highest loses
                    val minScore = scores.min()
                    val maxScore = scores.max()
                    state.players.mapIndexed { i, player ->
                        PlayerScore(
                            player = player,
                            score = scores[i],
                            isWinner = scores[i] == minScore,
                            isLoser = scores[i] == maxScore
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Recycle discard pile into draw pile
    // -----------------------------------------------------------------------

    private fun recycleDeck(state: GameState): GameState {
        if (state.discardPile.size <= 1) return state
        val top = state.discardPile.last()
        val toShuffle = state.discardPile.dropLast(1).toMutableList()
        toShuffle.shuffle()
        val newDrawPile = (state.drawPile + toShuffle).toMutableList()
        val newDiscard = mutableListOf(top)
        return state.copy(drawPile = newDrawPile, discardPile = newDiscard)
    }

    private fun ensureDrawPile(state: GameState): GameState {
        return if (state.drawPile.isEmpty()) recycleDeck(state) else state
    }
}
