package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mistris",
    indices = [Index(value = ["mobileNumber"], unique = true)]
)
data class MistriEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val mobileNumber: String,
    val password: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
