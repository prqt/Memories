package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val emoji: String = "📁",
    val accentColor: String = "Blue",
    val isPinned: Boolean = false,
    val coverPhotoUri: String? = null,
    val isPrivate: Boolean = false
) : Serializable
