package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LabourEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabour(labour: LabourEntity): Long

    @Update
    suspend fun updateLabour(labour: LabourEntity)

    @Delete
    suspend fun deleteLabour(labour: LabourEntity)

    @Query("SELECT * FROM labours WHERE mistriId = :mistriId ORDER BY name ASC")
    fun getLaboursByMistri(mistriId: Long): Flow<List<LabourEntity>>

    @Query("SELECT * FROM labours ORDER BY name ASC")
    fun getAllLabours(): Flow<List<LabourEntity>>

    @Query("SELECT * FROM labours WHERE mistriId = :mistriId AND name = :name LIMIT 1")
    suspend fun findLabourByName(mistriId: Long, name: String): LabourEntity?

    @Query("SELECT COUNT(*) FROM labours")
    fun getTotalLabourCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM labours WHERE mistriId = :mistriId")
    fun getLabourCountForMistri(mistriId: Long): Flow<Int>

    @Query("SELECT * FROM labours WHERE id = :id LIMIT 1")
    suspend fun getLabourById(id: Long): LabourEntity?
}
