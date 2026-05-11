package com.jaydiikay.bomb.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val players: String,      // JSON string of player names
    val scores: String,       // JSON string of {name: score}
    val winner: String,
    val loser: String,
    val endReason: String     // "normal" or "bomb"
)
