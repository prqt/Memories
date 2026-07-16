package com.example.ui.viewmodel

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Album
import com.example.data.model.Photo
import com.example.data.model.Settings
import com.example.data.repository.MemoriesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemoriesViewModel(private val repository: MemoriesRepository) : ViewModel() {

    val photos: StateFlow<List<Photo>> = repository.allPhotos
        .map { list -> list.filter { !it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePhotos: StateFlow<List<Photo>> = repository.favoritePhotos
        .map { list -> list.filter { !it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coreMemories: StateFlow<List<Photo>> = repository.coreMemories
        .map { list -> list.filter { !it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repository.allAlbums
        .map { list -> list.filter { !it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privatePhotos: StateFlow<List<Photo>> = repository.allPhotos
        .map { list -> list.filter { it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateAlbums: StateFlow<List<Album>> = repository.allAlbums
        .map { list -> list.filter { it.isPrivate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<Settings> = repository.settingsFlow
        .map { it ?: Settings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    // UI state states
    private val _selectedPhoto = MutableStateFlow<Photo?>(null)
    val selectedPhoto = _selectedPhoto.asStateFlow()

    private val _currentAlbumDetail = MutableStateFlow<Album?>(null)
    val currentAlbumDetail = _currentAlbumDetail.asStateFlow()

    private val _albumPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val albumPhotos = _albumPhotos.asStateFlow()

    private val _isPlayingRelive = MutableStateFlow(false)
    val isPlayingRelive = _isPlayingRelive.asStateFlow()

    private val _relivePhotos = MutableStateFlow<List<Photo>>(emptyList())
    val relivePhotos = _relivePhotos.asStateFlow()

    private val _reliveStartIndex = MutableStateFlow(0)
    val reliveStartIndex = _reliveStartIndex.asStateFlow()

    fun selectPhoto(photo: Photo?) {
        _selectedPhoto.value = photo
    }

    fun selectAlbum(album: Album?) {
        _currentAlbumDetail.value = album
        if (album != null) {
            viewModelScope.launch {
                repository.getPhotosForAlbum(album.id).collect {
                    _albumPhotos.value = it
                }
            }
        } else {
            _albumPhotos.value = emptyList()
        }
    }

    fun toggleFavorite(photo: Photo) {
        viewModelScope.launch {
            val updated = photo.copy(isFavorite = !photo.isFavorite)
            repository.updatePhoto(updated)
            // Keep selected photo updated in viewer
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun toggleCoreMemory(photo: Photo) {
        viewModelScope.launch {
            val updated = photo.copy(isCoreMemory = !photo.isCoreMemory)
            repository.updatePhoto(updated)
            // Keep selected photo updated in viewer
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = null
            }
        }
    }

    fun deletePhotos(photosList: List<Photo>) {
        viewModelScope.launch {
            photosList.forEach { photo ->
                repository.deletePhoto(photo)
            }
            if (photosList.any { it.id == _selectedPhoto.value?.id }) {
                _selectedPhoto.value = null
            }
        }
    }

    fun togglePhotosPrivate(photosList: List<Photo>) {
        viewModelScope.launch {
            photosList.forEach { photo ->
                repository.updatePhoto(photo.copy(isPrivate = !photo.isPrivate))
            }
            val currentSelected = _selectedPhoto.value
            if (currentSelected != null && photosList.any { it.id == currentSelected.id }) {
                _selectedPhoto.value = currentSelected.copy(isPrivate = !currentSelected.isPrivate)
            }
        }
    }

    fun addPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.insertPhoto(photo)
        }
    }

    fun createAlbum(title: String, description: String, emoji: String, accentColor: String, isPrivate: Boolean = false) {
        viewModelScope.launch {
            repository.insertAlbum(
                Album(
                    title = title,
                    description = description,
                    emoji = emoji,
                    accentColor = accentColor,
                    isPinned = false,
                    isPrivate = isPrivate
                )
            )
        }
    }

    fun toggleAlbumPrivate(album: Album) {
        viewModelScope.launch {
            val updated = album.copy(isPrivate = !album.isPrivate)
            repository.updateAlbum(updated)
            // Update all photos in this album to match the album's privacy status
            repository.getPhotosForAlbum(album.id).firstOrNull()?.forEach { photo ->
                repository.updatePhoto(photo.copy(isPrivate = updated.isPrivate))
            }
        }
    }

    fun togglePhotoPrivate(photo: Photo) {
        viewModelScope.launch {
            val updated = photo.copy(isPrivate = !photo.isPrivate)
            repository.updatePhoto(updated)
            // Keep selected photo updated in viewer
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = updated
            }
        }
    }

    fun deleteAlbum(album: Album) {
        viewModelScope.launch {
            repository.deleteAlbum(album)
            if (_currentAlbumDetail.value?.id == album.id) {
                _currentAlbumDetail.value = null
            }
        }
    }

    fun updateAlbum(album: Album) {
        viewModelScope.launch {
            repository.updateAlbum(album)
            if (_currentAlbumDetail.value?.id == album.id) {
                _currentAlbumDetail.value = album
            }
        }
    }

    fun addPhotoToAlbum(photoId: Int, albumId: Int) {
        viewModelScope.launch {
            repository.addPhotoToAlbum(photoId, albumId)
        }
    }

    fun addPhotosToAlbum(photoIds: List<Int>, albumId: Int) {
        viewModelScope.launch {
            photoIds.forEach { photoId ->
                repository.addPhotoToAlbum(photoId, albumId)
            }
        }
    }

    fun removePhotoFromAlbum(photoId: Int, albumId: Int) {
        viewModelScope.launch {
            repository.removePhotoFromAlbum(photoId, albumId)
        }
    }

    fun togglePinAlbum(album: Album) {
        viewModelScope.launch {
            repository.updateAlbum(album.copy(isPinned = !album.isPinned))
        }
    }

    fun updateSettings(
        themeMode: String? = null,
        accentColor: String? = null,
        layoutMode: String? = null,
        animationsEnabled: Boolean? = null,
        vaultPin: String? = null,
        isVaultLocked: Boolean? = null,
        showTimeline: Boolean? = null,
        iconTheme: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: Settings()
            val updated = current.copy(
                themeMode = themeMode ?: current.themeMode,
                accentColor = accentColor ?: current.accentColor,
                layoutMode = layoutMode ?: current.layoutMode,
                animationsEnabled = animationsEnabled ?: current.animationsEnabled,
                vaultPin = vaultPin ?: current.vaultPin,
                isVaultLocked = isVaultLocked ?: current.isVaultLocked,
                showTimeline = showTimeline ?: current.showTimeline,
                iconTheme = iconTheme ?: current.iconTheme
            )
            repository.saveSettings(updated)
            onComplete()
        }
    }

    fun startRelive(photosList: List<Photo>, startIndex: Int = 0) {
        _relivePhotos.value = photosList
        _reliveStartIndex.value = startIndex
        _isPlayingRelive.value = true
    }

    fun stopRelive() {
        _isPlayingRelive.value = false
        _relivePhotos.value = emptyList()
    }

    fun syncDeviceMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                
                // 1. Scan and Import Images
                val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val imageProjection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                
                try {
                    resolver.query(imageUri, imageProjection, null, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val path = cursor.getString(dataCol) ?: ""
                            val bucket = cursor.getString(bucketCol) ?: ""
                            val contentUri = ContentUris.withAppendedId(imageUri, id).toString()

                            // Check if already present in DB
                            if (repository.getPhotoCountWithUri(contentUri) > 0) {
                                continue
                            }

                            var timestamp = cursor.getLong(dateTakenCol)
                            if (timestamp <= 0) {
                                timestamp = cursor.getLong(dateAddedCol) * 1000
                            }
                            if (timestamp <= 0) {
                                timestamp = System.currentTimeMillis()
                            }

                            val size = cursor.getLong(sizeCol)
                            val width = cursor.getInt(widthCol)
                            val height = cursor.getInt(heightCol)
                            val name = cursor.getString(nameCol) ?: "Image"

                            val sizeStr = if (size > 1024 * 1024) {
                                String.format(Locale.getDefault(), "%.1f MB", size.toFloat() / (1024 * 1024))
                            } else {
                                String.format(Locale.getDefault(), "%d KB", size / 1024)
                            }

                            // Check if WhatsApp
                            val isWhatsApp = path.contains("WhatsApp", ignoreCase = true) || bucket.contains("WhatsApp", ignoreCase = true)
                            val caption = if (isWhatsApp) "WhatsApp Image" else "Camera Image"
                            val location = if (isWhatsApp) "WhatsApp" else "Local Gallery"

                            repository.insertPhoto(
                                Photo(
                                    uri = contentUri,
                                    timestamp = timestamp,
                                    caption = caption,
                                    location = location,
                                    cameraModel = if (isWhatsApp) "WhatsApp" else "Device Camera",
                                    lens = if (isWhatsApp) "WhatsApp" else "Unknown",
                                    iso = 0,
                                    shutterSpeed = "",
                                    resolution = "${width} x ${height}",
                                    fileSize = sizeStr,
                                    isFavorite = false,
                                    isCoreMemory = false,
                                    isLocal = true
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Scan and Import Videos
                val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATE_TAKEN,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )

                try {
                    resolver.query(videoUri, videoProjection, null, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                        val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                        val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val path = cursor.getString(dataCol) ?: ""
                            val bucket = cursor.getString(bucketCol) ?: ""
                            val contentUri = ContentUris.withAppendedId(videoUri, id).toString()

                            // Check if already present in DB
                            if (repository.getPhotoCountWithUri(contentUri) > 0) {
                                continue
                            }

                            var timestamp = cursor.getLong(dateTakenCol)
                            if (timestamp <= 0) {
                                timestamp = cursor.getLong(dateAddedCol) * 1000
                            }
                            if (timestamp <= 0) {
                                timestamp = System.currentTimeMillis()
                            }

                            val size = cursor.getLong(sizeCol)
                            val width = cursor.getInt(widthCol)
                            val height = cursor.getInt(heightCol)
                            val name = cursor.getString(nameCol) ?: "Video"

                            val sizeStr = if (size > 1024 * 1024) {
                                String.format(Locale.getDefault(), "%.1f MB", size.toFloat() / (1024 * 1024))
                            } else {
                                String.format(Locale.getDefault(), "%d KB", size / 1024)
                            }

                            // Check if WhatsApp
                            val isWhatsApp = path.contains("WhatsApp", ignoreCase = true) || bucket.contains("WhatsApp", ignoreCase = true)
                            val caption = if (isWhatsApp) "WhatsApp Video" else "Local Video"
                            val location = if (isWhatsApp) "WhatsApp" else "Local Gallery"

                            repository.insertPhoto(
                                Photo(
                                    uri = contentUri,
                                    timestamp = timestamp,
                                    caption = caption,
                                    location = location,
                                    cameraModel = if (isWhatsApp) "WhatsApp Video" else "Device Camera",
                                    lens = "Video",
                                    iso = 0,
                                    shutterSpeed = "Video",
                                    resolution = "${width} x ${height}",
                                    fileSize = sizeStr,
                                    isFavorite = false,
                                    isCoreMemory = false,
                                    isLocal = true,
                                    isVideo = true
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ViewModelFactory(private val repository: MemoriesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
