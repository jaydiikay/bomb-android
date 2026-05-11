package com.jaydiikay.bomb.game

object Scoring {
    fun scoreHand(hand: List<Card>) = hand.sumOf { it.points }
}
