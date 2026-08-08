package com.example.pdvmaquineta.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pdvmaquineta.data.local.database.entity.TerminalConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalConfigDao {
    @Query("SELECT * FROM terminal_config LIMIT 1")
    suspend fun find(): TerminalConfigEntity?

    @Query("SELECT * FROM terminal_config LIMIT 1")
    fun observe(): Flow<TerminalConfigEntity?>

    @Insert
    suspend fun insert(config: TerminalConfigEntity): Long

    @Update
    suspend fun update(config: TerminalConfigEntity)
}
