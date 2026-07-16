package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val timestamp: Long,
    val caption: String = "",
    val location: String = "Unknown",
    val cameraModel: String = "iPhone 15 Pro",
    val lens: String = "24mm f/1.78",
    val iso: Int = 50,
    val shutterSpeed: String = "1/120s",
    val resolution: String = "4000 x 3000",
    val fileSize: String = "3.2 MB",
    val isFavorite: Boolean = false,
    val isCoreMemory: Boolean = false,
    val isLocal: Boolean = false,
    val isVideo: Boolean = false,
    val isPrivate: Boolean = false
) : Serializable
