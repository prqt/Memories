package com.example.data.dao

import androidx.room.*
import com.example.data.model.Photo
import com.example.data.model.PhotoAlbumCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE isCoreMemory = 1 ORDER BY timestamp DESC")
    fun getCoreMemories(): Flow<List<Photo>>

    @Query("""
        SELECT photos.* FROM photos
        INNER JOIN photo_album_cross_ref ON photos.id = photo_album_cross_ref.photoId
        WHERE photo_album_cross_ref.albumId = :albumId
        ORDER BY photos.timestamp DESC
    """)
    fun getPhotosForAlbum(albumId: Int): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo): Long

    @Update
    suspend fun updatePhoto(photo: Photo)

    @Delete
    suspend fun deletePhoto(photo: Photo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoAlbumCrossRef(crossRef: PhotoAlbumCrossRef)

    @Query("DELETE FROM photo_album_cross_ref WHERE photoId = :photoId AND albumId = :albumId")
    suspend fun deletePhotoAlbumCrossRef(photoId: Int, albumId: Int)

    @Query("DELETE FROM photo_album_cross_ref WHERE photoId = :photoId")
    suspend fun deleteCrossRefsForPhoto(photoId: Int)

    @Query("SELECT COUNT(*) FROM photos WHERE uri = :uri")
    suspend fun getPhotoCountWithUri(uri: String): Int
}
