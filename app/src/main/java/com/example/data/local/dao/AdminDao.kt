package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AdminEntity

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins WHERE username = :username LIMIT 1")
    suspend fun getAdmin(username: String): AdminEntity?

    @Query("SELECT * FROM admins LIMIT 1")
    suspend fun getPrimaryAdmin(): AdminEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAdmin(admin: AdminEntity)

    @Update
    suspend fun updateAdmin(admin: AdminEntity)

    @Query("DELETE FROM admins")
    suspend fun deleteAllAdmins()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAdmin(admin: AdminEntity)
}
