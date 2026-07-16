package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "photo_album_cross_ref",
    primaryKeys = ["photoId", "albumId"]
)
data class PhotoAlbumCrossRef(
    val photoId: Int,
    val albumId: Int
)
