package com.jaydiikay.bomb.game

object Deck {
    fun create(): List<Card> = Suit.values().flatMap { suit ->
        Rank.values().map { rank -> Card(suit, rank) }
    }

    fun List<Card>.shuffled(): List<Card> = toMutableList().also { it.shuffle() }
}
