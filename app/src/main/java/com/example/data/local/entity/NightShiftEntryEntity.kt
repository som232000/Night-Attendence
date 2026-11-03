package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "night_shift_entries",
    indices = [
        Index(value = ["mistriId"]),
        Index(value = ["labourId"]),
        Index(value = ["workDate"])
    ]
)
data class NightShiftEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mistriId: Long,
    val mistriName: String,
    val labourId: Long,
    val labourName: String,
    val workPlace: String,
    val workDate: String, // YYYY-MM-DD format
    val shiftDetails: String,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
