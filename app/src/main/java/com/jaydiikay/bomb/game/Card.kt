package com.jaydiikay.bomb.game

enum class Suit { HEARTS, DIAMONDS, CLUBS, SPADES }

enum class Rank(val display: String, val points: Int) {
    ACE("A", 1),
    TWO("2", 20),
    THREE("3", 3),
    FOUR("4", 20),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 45),
    QUEEN("Q", 2),
    KING("K", 4)
}

data class Card(val suit: Suit, val rank: Rank) {
    val id: String get() = "${rank.name}_${suit.name}"
    val isBomb: Boolean get() = rank == Rank.SEVEN && suit == Suit.HEARTS
    val points: Int get() = if (isBomb) 500 else rank.points
    val suitSymbol: String get() = when (suit) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }
    val isRed: Boolean get() = suit == Suit.HEARTS || suit == Suit.DIAMONDS
}
