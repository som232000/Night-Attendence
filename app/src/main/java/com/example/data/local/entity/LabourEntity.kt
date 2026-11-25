package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labours",
    indices = [
        Index(value = ["mistriId"]),
        Index(value = ["cloudId"])
    ]
)
data class LabourEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val mistriId: Long,
    val name: String,
    val mobileNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
