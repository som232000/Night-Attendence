package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NightShiftEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NightShiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NightShiftEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: NightShiftEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: NightShiftEntryEntity)

    @Query("DELETE FROM night_shift_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT * FROM night_shift_entries ORDER BY workDate DESC, id DESC")
    fun getAllEntries(): Flow<List<NightShiftEntryEntity>>

    @Query("SELECT * FROM night_shift_entries WHERE mistriId = :mistriId ORDER BY workDate DESC, id DESC")
    fun getEntriesByMistri(mistriId: Long): Flow<List<NightShiftEntryEntity>>

    @Query("SELECT COUNT(*) FROM night_shift_entries")
    fun getTotalEntryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM night_shift_entries WHERE mistriId = :mistriId")
    fun getEntryCountForMistri(mistriId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM night_shift_entries WHERE workDate = :date")
    fun getEntryCountForDate(date: String): Flow<Int>

    @Query("SELECT DISTINCT workPlace FROM night_shift_entries ORDER BY workPlace ASC")
    fun getAllWorkPlaces(): Flow<List<String>>

    @Query("SELECT DISTINCT substr(workDate, 1, 7) FROM night_shift_entries ORDER BY substr(workDate, 1, 7) DESC")
    fun getAvailableMonths(): Flow<List<String>>
}
