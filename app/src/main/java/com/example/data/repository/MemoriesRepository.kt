package com.example.data.repository

import com.example.data.dao.AlbumDao
import com.example.data.dao.PhotoDao
import com.example.data.dao.SettingsDao
import com.example.data.model.Album
import com.example.data.model.Photo
import com.example.data.model.PhotoAlbumCrossRef
import com.example.data.model.Settings
import kotlinx.coroutines.flow.Flow

class MemoriesRepository(
    private val photoDao: PhotoDao,
    private val albumDao: AlbumDao,
    private val settingsDao: SettingsDao
) {
    val allPhotos: Flow<List<Photo>> = photoDao.getAllPhotos()
    val favoritePhotos: Flow<List<Photo>> = photoDao.getFavoritePhotos()
    val coreMemories: Flow<List<Photo>> = photoDao.getCoreMemories()
    val allAlbums: Flow<List<Album>> = albumDao.getAllAlbums()
    val settingsFlow: Flow<Settings?> = settingsDao.getSettingsFlow()

    suspend fun getSettings(): Settings? = settingsDao.getSettings()

    suspend fun saveSettings(settings: Settings) {
        settingsDao.saveSettings(settings)
    }

    suspend fun insertPhoto(photo: Photo): Long = photoDao.insertPhoto(photo)

    suspend fun updatePhoto(photo: Photo) {
        photoDao.updatePhoto(photo)
    }

    suspend fun deletePhoto(photo: Photo) {
        photoDao.deleteCrossRefsForPhoto(photo.id)
        photoDao.deletePhoto(photo)
    }

    suspend fun insertAlbum(album: Album): Long = albumDao.insertAlbum(album)

    suspend fun updateAlbum(album: Album) {
        albumDao.updateAlbum(album)
    }

    suspend fun deleteAlbum(album: Album) {
        albumDao.deleteAlbum(album)
    }

    suspend fun addPhotoToAlbum(photoId: Int, albumId: Int) {
        photoDao.insertPhotoAlbumCrossRef(PhotoAlbumCrossRef(photoId, albumId))
    }

    suspend fun removePhotoFromAlbum(photoId: Int, albumId: Int) {
        photoDao.deletePhotoAlbumCrossRef(photoId, albumId)
    }

    fun getPhotosForAlbum(albumId: Int): Flow<List<Photo>> {
        return photoDao.getPhotosForAlbum(albumId)
    }

    suspend fun getPhotoCountWithUri(uri: String): Int {
        return photoDao.getPhotoCountWithUri(uri)
    }
}
