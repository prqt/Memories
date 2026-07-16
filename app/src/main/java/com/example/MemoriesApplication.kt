package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.database.AppDatabase
import com.example.data.model.Album
import com.example.data.model.Photo
import com.example.data.model.Settings
import com.example.data.repository.MemoriesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MemoriesApplication : Application(), ImageLoaderFactory {
    lateinit var database: AppDatabase
    lateinit var repository: MemoriesRepository

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = MemoriesRepository(database.photoDao(), database.albumDao(), database.settingsDao())

        // Seed database if empty
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettings()
            if (settings == null) {
                repository.saveSettings(Settings())
            }

            // Check if photos are empty
            val existingPhotos = repository.allPhotos.firstOrNull() ?: emptyList()
            if (existingPhotos.isEmpty()) {
                seedInitialData()
            }
        }
    }

    private suspend fun seedInitialData() {
        // Seed Photos (referencing our generated image resource names)
        val photo1 = Photo(
            uri = "img_memory_sunset_1784099119859",
            timestamp = 1623789000000L, // June 15, 2021
            caption = "A quiet, golden evening on the coast",
            location = "Carmel-by-the-Sea, CA",
            cameraModel = "Sony Alpha 7R V",
            lens = "35mm f/1.4 GM",
            iso = 100,
            shutterSpeed = "1/250s",
            resolution = "9504 x 6336",
            fileSize = "14.2 MB",
            isFavorite = true,
            isCoreMemory = true
        )
        val photo2 = Photo(
            uri = "img_memory_mountains_1784099129903",
            timestamp = 1692864900000L, // August 24, 2023
            caption = "The morning light over the crystal peaks",
            location = "Banff, Canada",
            cameraModel = "Fujifilm X-T5",
            lens = "18-55mm f/2.8",
            iso = 160,
            shutterSpeed = "1/500s",
            resolution = "7728 x 5152",
            fileSize = "11.1 MB",
            isFavorite = false,
            isCoreMemory = true
        )
        val photo3 = Photo(
            uri = "img_memory_cozy_1784099142479",
            timestamp = 1763067600000L, // November 12, 2025
            caption = "Warm fireplace on a freezing autumn evening",
            location = "Aspen, CO",
            cameraModel = "Leica Q3",
            lens = "28mm f/1.7 Summilux",
            iso = 400,
            shutterSpeed = "1/60s",
            resolution = "9520 x 6344",
            fileSize = "18.4 MB",
            isFavorite = true,
            isCoreMemory = false
        )
        val photo4 = Photo(
            uri = "img_memory_forest_1784099153130",
            timestamp = 1783977600000L, // July 14, 2026 (Yesterday)
            caption = "Lost in the whispers of the giant redwoods",
            location = "Muir Woods, CA",
            cameraModel = "Canon EOS R5",
            lens = "24-70mm f/2.8L",
            iso = 200,
            shutterSpeed = "1/125s",
            resolution = "8192 x 5464",
            fileSize = "12.8 MB",
            isFavorite = false,
            isCoreMemory = false
        )
        val videoPhoto = Photo(
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            timestamp = 1718481600000L, // June 15, 2024
            caption = "Cinematic golden twilight fireplace",
            location = "Yosemite, CA",
            cameraModel = "Sony FX3 Cinema Camera",
            lens = "50mm T1.2 Cine",
            iso = 320,
            shutterSpeed = "1/48s",
            resolution = "3840 x 2160",
            fileSize = "8.4 MB",
            isFavorite = true,
            isCoreMemory = false,
            isVideo = true
        )

        val id1 = repository.insertPhoto(photo1).toInt()
        val id2 = repository.insertPhoto(photo2).toInt()
        val id3 = repository.insertPhoto(photo3).toInt()
        val id4 = repository.insertPhoto(photo4).toInt()
        val id5 = repository.insertPhoto(videoPhoto).toInt()

        // Seed Albums
        val album1 = Album(
            title = "Quiet Escapes",
            description = "Serene places and moments where time slowed down.",
            emoji = "🌲",
            accentColor = "Mint",
            isPinned = true,
            coverPhotoUri = "img_memory_forest_1784099153130"
        )
        val album2 = Album(
            title = "Golden Hour",
            description = "Capturing the magic when the sun kisses the earth.",
            emoji = "🌅",
            accentColor = "Orange",
            isPinned = false,
            coverPhotoUri = "img_memory_sunset_1784099119859"
        )

        val albumId1 = repository.insertAlbum(album1).toInt()
        val albumId2 = repository.insertAlbum(album2).toInt()

        // Link photos to albums
        repository.addPhotoToAlbum(id4, albumId1) // Forest inside Quiet Escapes
        repository.addPhotoToAlbum(id2, albumId1) // Mountains inside Quiet Escapes
        repository.addPhotoToAlbum(id1, albumId2) // Sunset inside Golden Hour
        repository.addPhotoToAlbum(id5, albumId2) // Cinematic video inside Golden Hour
    }
}
