package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MistriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistriDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMistri(mistri: MistriEntity): Long

    @Update
    suspend fun updateMistri(mistri: MistriEntity)

    @Delete
    suspend fun deleteMistri(mistri: MistriEntity)

    @Query("SELECT * FROM mistris WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getMistriByMobile(mobile: String): MistriEntity?

    @Query("SELECT * FROM mistris WHERE id = :id LIMIT 1")
    suspend fun getMistriById(id: Long): MistriEntity?

    @Query("SELECT * FROM mistris ORDER BY fullName ASC")
    fun getAllMistris(): Flow<List<MistriEntity>>

    @Query("UPDATE mistris SET isActive = :isActive WHERE id = :mistriId")
    suspend fun updateMistriStatus(mistriId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM mistris")
    fun getMistriCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mistris WHERE isActive = 1")
    fun getActiveMistriCount(): Flow<Int>
}
