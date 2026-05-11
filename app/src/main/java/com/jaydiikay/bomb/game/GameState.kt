package com.jaydiikay.bomb.game

data class PlayerConfig(val name: String, val isBot: Boolean = false)

data class Player(
    val id: Int,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),
    val isBot: Boolean = false
)

enum class GamePhase { PASS_AND_PLAY, PLAYING, AWAITING_SECOND, DREW_CARD, BOMB, GAME_OVER }
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
    val phase: GamePhase = GamePhase.PASS_AND_PLAY,
    val selectedCard: Card? = null,
    val isChained: Boolean = false,
    val drawnCards: List<Card> = emptyList(),
    val endReason: EndReason? = null,
    val scores: List<PlayerScore> = emptyList(),
    val winnerIndex: Int? = null
)

object GameLogic {

    // -----------------------------------------------------------------------
    // Game creation
    // -----------------------------------------------------------------------

    fun createGame(players: List<PlayerConfig>): GameState {
        val deck = Deck.create().toMutableList().also { it.shuffle() }

        val gamePlayers = players.mapIndexed { i, config ->
            val hand = mutableListOf<Card>()
            repeat(7) { hand.add(deck.removeFirst()) }
            Player(i, config.name, hand, config.isBot)
        }

        // Ensure starting card is not special
        var topCard: Card
        do {
            topCard = deck.removeFirst()
        } while (
            topCard.rank in listOf(Rank.TWO, Rank.FOUR, Rank.EIGHT, Rank.JACK) || topCard.isBomb
        )

        // topCard is placed as the initial discard; the pile tracks all played cards
        val discardPile = mutableListOf(topCard)
        val firstPhase = if (gamePlayers[0].isBot) GamePhase.PLAYING else GamePhase.PASS_AND_PLAY
        return GameState(
            players = gamePlayers,
            drawPile = deck,
            discardPile = discardPile,
            topCard = topCard,
            phase = firstPhase
        )
    }

    // -----------------------------------------------------------------------
    // Advance turn helper
    // -----------------------------------------------------------------------

    private fun advanceTurn(state: GameState): GameState {
        val n = state.players.size
        val dir = if (state.reverseOnce) -state.direction else state.direction
        val nextIdx = ((state.currentPlayerIndex + dir) % n + n) % n
        val nextPhase = if (state.players[nextIdx].isBot) GamePhase.PLAYING else GamePhase.PASS_AND_PLAY
        return state.copy(
            currentPlayerIndex = nextIdx,
            reverseOnce = false,
            phase = nextPhase,
            drawnCards = emptyList()
        )
    }

    // -----------------------------------------------------------------------
    // Play a single card
    // -----------------------------------------------------------------------

    fun playCard(state: GameState, card: Card): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        if (!Rules.canPlay(card, state.topCard, state.pendingDraw)) return state
        if (!currentPlayer.hand.any { it.id == card.id }) return state

        val newHand = currentPlayer.hand.toMutableList()
        newHand.removeIf { it.id == card.id }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = state.players.toMutableList()
        newPlayers[state.currentPlayerIndex] = updatedPlayer

        // Add played card to discard pile; it also becomes topCard
        val newDiscard = state.discardPile.toMutableList()
        newDiscard.add(card)

        var newState = state.copy(
            players = newPlayers,
            discardPile = newDiscard,
            topCard = card
        )

        // Check for bomb (7 of hearts)
        if (card.isBomb) {
            return handleBombEnd(newState, state.currentPlayerIndex)
        }

        // Check for normal win (hand empty) — before special card handling so 8/J as last card wins
        if (newHand.isEmpty()) {
            return handleNormalWin(newState, state.currentPlayerIndex)
        }

        // Apply card effects
        when (card.rank) {
            Rank.TWO -> {
                // Neutralize a pending draw-2 penalty; fresh 2 forces next player to draw 2
                val newPending = if (state.pendingDraw > 0) 0 else 2
                newState = newState.copy(pendingDraw = newPending)
                return advanceTurn(newState)
            }
            Rank.FOUR -> {
                return if (state.players.size == 2) {
                    // With 2 players, reversal returns play to the same person
                    newState.copy(
                        pendingDraw = 0,
                        phase = if (currentPlayer.isBot) GamePhase.PLAYING else GamePhase.PASS_AND_PLAY
                    )
                } else {
                    newState = newState.copy(reverseOnce = true, pendingDraw = 0)
                    advanceTurn(newState)
                }
            }
            Rank.EIGHT, Rank.JACK -> {
                // Always enter AWAITING_SECOND — player can cancel if no valid second card
                return newState.copy(
                    phase = GamePhase.AWAITING_SECOND,
                    selectedCard = card,
                    isChained = false,
                    pendingDraw = 0
                )
            }
            else -> {}
        }

        newState = newState.copy(pendingDraw = 0)
        return advanceTurn(newState)
    }

    // -----------------------------------------------------------------------
    // Play a pair (8/J + second card)
    // -----------------------------------------------------------------------

    fun playPair(state: GameState, second: Card): GameState {
        if (state.phase != GamePhase.AWAITING_SECOND) return state
        val first = state.selectedCard ?: return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        if (!Rules.validSecondCards(first, currentPlayer.hand).any { it.id == second.id }) return state

        val newHand = currentPlayer.hand.toMutableList()
        newHand.removeIf { it.id == second.id }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = state.players.toMutableList()
        newPlayers[state.currentPlayerIndex] = updatedPlayer

        val newDiscard = state.discardPile.toMutableList()
        newDiscard.add(second)

        var newState = state.copy(
            players = newPlayers,
            discardPile = newDiscard,
            topCard = second,
            selectedCard = null,
            isChained = false,
            pendingDraw = 0
        )

        // Check for bomb as second card
        if (second.isBomb) {
            return handleBombEnd(newState, state.currentPlayerIndex)
        }

        // Check win
        if (newHand.isEmpty()) {
            return handleNormalWin(newState, state.currentPlayerIndex)
        }

        // If second card is also 8/J, chain — player must play another second card
        if (Rules.requiresSecondCard(second)) {
            return newState.copy(
                phase = GamePhase.AWAITING_SECOND,
                selectedCard = second,
                isChained = true
            )
        }

        // Apply special effects of the second card
        when (second.rank) {
            Rank.FOUR -> {
                return if (state.players.size == 2) {
                    newState.copy(
                        phase = if (currentPlayer.isBot) GamePhase.PLAYING else GamePhase.PASS_AND_PLAY
                    )
                } else {
                    newState = newState.copy(reverseOnce = true)
                    advanceTurn(newState)
                }
            }
            Rank.TWO -> {
                newState = newState.copy(pendingDraw = state.pendingDraw + 2)
                return advanceTurn(newState)
            }
            else -> {}
        }

        return advanceTurn(newState)
    }

    // -----------------------------------------------------------------------
    // Cancel second card selection — draws 1 card and pauses
    // -----------------------------------------------------------------------

    fun cancelSecond(state: GameState): GameState {
        if (state.selectedCard == null) return state
        val currentPlayer = state.players[state.currentPlayerIndex]

        // If hand is already empty, win immediately
        if (currentPlayer.hand.isEmpty()) {
            return handleNormalWin(
                state.copy(selectedCard = null, isChained = false),
                state.currentPlayerIndex
            )
        }

        val handBefore = currentPlayer.hand.map { it.id }.toSet()
        var workingState = ensureDrawPile(state)
        val newHand = currentPlayer.hand.toMutableList()
        if (workingState.drawPile.isNotEmpty()) {
            newHand.add(workingState.drawPile.removeFirst())
        }
        val updatedPlayer = currentPlayer.copy(hand = newHand)
        val newPlayers = workingState.players.toMutableList()
        newPlayers[workingState.currentPlayerIndex] = updatedPlayer
        val drawnCards = newHand.filter { it.id !in handBefore }

        return workingState.copy(
            players = newPlayers,
            selectedCard = null,
            isChained = false,
            drawnCards = drawnCards,
            phase = GamePhase.DREW_CARD
        )
    }

    // -----------------------------------------------------------------------
    // Draw a card (player action) — pauses at DREW_CARD
    // -----------------------------------------------------------------------

    fun drawCard(state: GameState): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val currentPlayer = state.players[state.currentPlayerIndex]
        val drawCount = if (state.pendingDraw > 0) state.pendingDraw else 1
        val handBefore = currentPlayer.hand.map { it.id }.toSet()

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
        val drawnCards = newHand.filter { it.id !in handBefore }

        return workingState.copy(
            players = newPlayers,
            pendingDraw = 0,
            drawnCards = drawnCards,
            phase = GamePhase.DREW_CARD
        )
    }

    // -----------------------------------------------------------------------
    // End the drew-card pause and advance turn
    // -----------------------------------------------------------------------

    fun endDrawnTurn(state: GameState): GameState {
        return advanceTurn(state.copy(drawnCards = emptyList()))
    }

    // -----------------------------------------------------------------------
    // Compute next player index (exposed for ViewModel if needed)
    // -----------------------------------------------------------------------

    fun nextTurn(state: GameState): Int {
        val n = state.players.size
        val dir = if (state.reverseOnce) -state.direction else state.direction
        return ((state.currentPlayerIndex + dir) % n + n) % n
    }

    // -----------------------------------------------------------------------
    // Bomb ending
    // -----------------------------------------------------------------------

    private fun handleBombEnd(state: GameState, triggeringIndex: Int): GameState {
        val scores = state.players.map { Scoring.scoreHand(it.hand) }
        val bombWasLastCard = state.players[triggeringIndex].hand.isEmpty()

        val playerScores = if (bombWasLastCard) {
            val otherScores = scores.filterIndexed { i, _ -> i != triggeringIndex }
            val maxOther = if (otherScores.isNotEmpty()) otherScores.max() else 0
            state.players.mapIndexed { i, player ->
                PlayerScore(
                    player = player,
                    score = scores[i],
                    isWinner = i == triggeringIndex,
                    isLoser = i != triggeringIndex && scores[i] == maxOther
                )
            }
        } else {
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

        val winnerIdx = if (bombWasLastCard) triggeringIndex
        else playerScores.indexOfFirst { it.isWinner }

        return state.copy(
            phase = GamePhase.BOMB,
            endReason = EndReason.BOMB,
            scores = playerScores,
            winnerIndex = winnerIdx
        )
    }

    // -----------------------------------------------------------------------
    // Normal win — tiebreaker: tied highest scorers each draw until unique loser
    // -----------------------------------------------------------------------

    private fun handleNormalWin(state: GameState, winnerIndex: Int): GameState {
        val scores = state.players.map { Scoring.scoreHand(it.hand) }.toMutableList()
        var workingState = state

        val otherIndices = state.players.indices.filter { it != winnerIndex }
        val otherScores = otherIndices.associateWith { scores[it] }.toMutableMap()

        while (true) {
            if (otherScores.isEmpty()) break
            val maxScore = otherScores.values.max()
            val tied = otherScores.filter { it.value == maxScore }

            if (tied.size == 1) {
                val loserIdx = tied.keys.first()
                return workingState.copy(
                    phase = GamePhase.GAME_OVER,
                    endReason = EndReason.NORMAL,
                    scores = buildScores(state.players, scores, winnerIndex, loserIdx),
                    winnerIndex = winnerIndex
                )
            }

            // Tied — each tied player draws a card to break the tie
            var exhausted = false
            for (idx in tied.keys) {
                workingState = ensureDrawPile(workingState)
                if (workingState.drawPile.isEmpty()) { exhausted = true; break }
                val card = workingState.drawPile.removeFirst()
                val added = Scoring.scoreHand(listOf(card))
                scores[idx] += added
                otherScores[idx] = scores[idx]
            }

            if (exhausted) {
                val maxScore2 = otherScores.values.max()
                val loserIdx = otherScores.filter { it.value == maxScore2 }.keys.first()
                return workingState.copy(
                    phase = GamePhase.GAME_OVER,
                    endReason = EndReason.NORMAL,
                    scores = buildScores(state.players, scores, winnerIndex, loserIdx),
                    winnerIndex = winnerIndex
                )
            }
        }

        // Fallback (single other player)
        val loserIdx = otherIndices.firstOrNull() ?: winnerIndex
        return workingState.copy(
            phase = GamePhase.GAME_OVER,
            endReason = EndReason.NORMAL,
            scores = buildScores(state.players, scores, winnerIndex, loserIdx),
            winnerIndex = winnerIndex
        )
    }

    private fun buildScores(
        players: List<Player>,
        scores: List<Int>,
        winnerIndex: Int,
        loserIndex: Int
    ): List<PlayerScore> = players.mapIndexed { i, player ->
        PlayerScore(
            player = player,
            score = scores[i],
            isWinner = i == winnerIndex,
            isLoser = i == loserIndex
        )
    }

    // -----------------------------------------------------------------------
    // Deck recycling
    // -----------------------------------------------------------------------

    private fun recycleDeck(state: GameState): GameState {
        if (state.discardPile.size <= 1) return state
        val top = state.discardPile.last()
        val toShuffle = state.discardPile.dropLast(1).toMutableList()
        toShuffle.shuffle()
        val newDrawPile = (state.drawPile + toShuffle).toMutableList()
        return state.copy(drawPile = newDrawPile, discardPile = mutableListOf(top))
    }

    private fun ensureDrawPile(state: GameState): GameState {
        return if (state.drawPile.isEmpty()) recycleDeck(state) else state
    }
}
