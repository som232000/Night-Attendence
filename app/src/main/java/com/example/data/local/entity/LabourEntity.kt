package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "labours",
    foreignKeys = [
        ForeignKey(
            entity = MistriEntity::class,
            parentColumns = ["id"],
            childColumns = ["mistriId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mistriId"])]
)
data class LabourEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mistriId: Long,
    val name: String,
    val mobileNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
