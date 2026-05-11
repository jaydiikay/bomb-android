package com.jaydiikay.bomb.game

object Rules {
    fun canPlay(card: Card, topCard: Card, pendingDraw: Int): Boolean {
        if (pendingDraw > 0) return card.rank == Rank.TWO
        if (card.isBomb) return true
        return card.suit == topCard.suit || card.rank == topCard.rank
    }

    fun requiresSecondCard(card: Card) = card.rank == Rank.EIGHT || card.rank == Rank.JACK

    fun validSecondCards(first: Card, hand: List<Card>) =
        hand.filter { it.id != first.id && (it.suit == first.suit || it.rank == first.rank) }
}
