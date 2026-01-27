package com.example.mydreamtrip.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,

    val title: String,
    val location: String,
    val ratingText: String,
    val author: String,

    val localImageUri: String?,

    val createdAt: Long
)
