package com.jaydiikay.bomb.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Query("SELECT * FROM game_results ORDER BY date DESC")
    fun getAllResults(): Flow<List<GameResult>>

    @Insert
    suspend fun insert(result: GameResult)
}
