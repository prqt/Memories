package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.getAccentColor
import com.example.ui.viewmodel.MemoriesViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MemoriesViewModel by viewModels {
        val app = application as MemoriesApplication
        ViewModelFactory(app.repository)
    }

    private val permissionsToRequest by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.syncDeviceMedia(this)
        }
    }

    private var hasRequestedPermission = false

    private fun hasPermissions(): Boolean {
        return permissionsToRequest.all {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasPermissions()) {
            viewModel.syncDeviceMedia(this)
        } else if (!hasRequestedPermission) {
            hasRequestedPermission = true
            try {
                requestPermissionLauncher.launch(permissionsToRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            val settingsState by viewModel.settings.collectAsStateWithLifecycle()
            val photosState by viewModel.photos.collectAsStateWithLifecycle()
            val albumsState by viewModel.albums.collectAsStateWithLifecycle()
            val privatePhotosState by viewModel.privatePhotos.collectAsStateWithLifecycle()
            val privateAlbumsState by viewModel.privateAlbums.collectAsStateWithLifecycle()

            val selectedPhoto by viewModel.selectedPhoto.collectAsStateWithLifecycle()
            val isPlayingRelive by viewModel.isPlayingRelive.collectAsStateWithLifecycle()
            val relivePhotos by viewModel.relivePhotos.collectAsStateWithLifecycle()
            val reliveStartIndex by viewModel.reliveStartIndex.collectAsStateWithLifecycle()
            val currentAlbumDetail by viewModel.currentAlbumDetail.collectAsStateWithLifecycle()
            val albumPhotos by viewModel.albumPhotos.collectAsStateWithLifecycle()

            // Setup custom accent color
            val activeAccentColor = getAccentColor(settingsState.accentColor)

            // Local navigation state
            var currentTab by remember { mutableStateOf("camera") } // camera, memories, calendar
            var showSettingsSheet by remember { mutableStateOf(false) }

            // Theme selector: follow DB settings, fall back to system
            val isDark = when (settingsState.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main screen content slot
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "MainTabsTransitions"
                        ) { tab ->
                            when (tab) {
                                "camera" -> CameraScreen(
                                    photos = photosState,
                                    layoutMode = settingsState.layoutMode,
                                    showTimeline = settingsState.showTimeline,
                                    accentColorName = settingsState.accentColor,
                                    onPhotoClick = { viewModel.selectPhoto(it) },
                                    onOpenSettings = { showSettingsSheet = true },
                                    onPhotoAdded = { viewModel.addPhoto(it) },
                                    onDeletePhotos = { viewModel.deletePhotos(it) },
                                    onTogglePhotosPrivate = { viewModel.togglePhotosPrivate(it) }
                                )
                                "memories" -> MemoriesScreen(
                                    photos = photosState,
                                    albums = albumsState,
                                    allPhotos = photosState,
                                    accentColorName = settingsState.accentColor,
                                    settings = settingsState,
                                    onStartRelive = { photos, idx -> viewModel.startRelive(photos, idx) },
                                    onAlbumClick = { album ->
                                        viewModel.selectAlbum(album)
                                    },
                                    onCreateAlbum = { title, desc, emoji, accent, isPrivate ->
                                        viewModel.createAlbum(title, desc, emoji, accent, isPrivate)
                                    },
                                    onDeleteAlbum = { viewModel.deleteAlbum(it) },
                                    onTogglePinAlbum = { viewModel.togglePinAlbum(it) },
                                    onUpdateAlbum = { viewModel.updateAlbum(it) },
                                    onAddPhotosToAlbum = { ids, aId -> viewModel.addPhotosToAlbum(ids, aId) }
                                )
                                "calendar" -> CalendarScreen(
                                    photos = photosState,
                                    accentColorName = settingsState.accentColor,
                                    onPhotoClick = { viewModel.selectPhoto(it) }
                                )
                            }
                        }

                        // Floating Glassmorphism Bottom Navigation Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("glass_navigation_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NavigationItem(
                                        label = "Camera",
                                        icon = if (currentTab == "camera") Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                        isActive = currentTab == "camera",
                                        activeColor = activeAccentColor,
                                        onClick = { currentTab = "camera" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    NavigationItem(
                                        label = "Memories",
                                        icon = if (currentTab == "memories") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        isActive = currentTab == "memories",
                                        activeColor = activeAccentColor,
                                        onClick = { currentTab = "memories" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    NavigationItem(
                                        label = "Calendar",
                                        icon = if (currentTab == "calendar") Icons.Filled.DateRange else Icons.Outlined.DateRange,
                                        isActive = currentTab == "calendar",
                                        activeColor = activeAccentColor,
                                        onClick = { currentTab = "calendar" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Dedicated Full Screen Settings Panel
                        AnimatedVisibility(
                            visible = showSettingsSheet,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            SettingsScreen(
                                settings = settingsState,
                                totalPhotosCount = photosState.size,
                                albumsCount = albumsState.size,
                                privatePhotos = privatePhotosState,
                                privateAlbums = privateAlbumsState,
                                onUpdatePhoto = { viewModel.togglePhotoPrivate(it) },
                                onUpdateAlbum = { viewModel.toggleAlbumPrivate(it) },
                                onSelectPhoto = { viewModel.selectPhoto(it) },
                                onSelectAlbum = { viewModel.selectAlbum(it) },
                                onDismiss = { showSettingsSheet = false },
                                onUpdateSettings = { theme, accent, layout, anims, pin, locked, timeline, iconTheme ->
                                    viewModel.updateSettings(
                                        themeMode = theme,
                                        accentColor = accent,
                                        layoutMode = layout,
                                        animationsEnabled = anims,
                                        vaultPin = pin,
                                        isVaultLocked = locked,
                                        showTimeline = timeline,
                                        iconTheme = iconTheme,
                                        onComplete = {
                                            if (iconTheme != null) {
                                                changeAppIcon(iconTheme)
                                            }
                                        }
                                    )
                                }
                            )
                        }

                        // Album Detail Overlay
                        currentAlbumDetail?.let { album ->
                            val viewerAllPhotos = if (album.isPrivate) privatePhotosState else photosState
                            AlbumDetailScreen(
                                album = album,
                                photos = albumPhotos,
                                allPhotos = viewerAllPhotos,
                                accentColorName = settingsState.accentColor,
                                onDismiss = { viewModel.selectAlbum(null) },
                                onPhotoClick = { viewModel.selectPhoto(it) },
                                onStartRelive = { list, idx -> viewModel.startRelive(list, idx) },
                                onUpdateAlbum = { viewModel.updateAlbum(it) },
                                onDeleteAlbum = { viewModel.deleteAlbum(it) },
                                onAddPhotosToAlbum = { ids, aId -> viewModel.addPhotosToAlbum(ids, aId) }
                            )
                        }

                        // Immersive fullscreen Photo Viewer Overlay
                        selectedPhoto?.let { photo ->
                            val viewerPhotos = if (photo.isPrivate) privatePhotosState else photosState
                            PhotoViewerScreen(
                                photos = viewerPhotos,
                                initialPhoto = photo,
                                accentColorName = settingsState.accentColor,
                                albums = albumsState + privateAlbumsState,
                                onDismiss = { viewModel.selectPhoto(null) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleCoreMemory = { viewModel.toggleCoreMemory(it) },
                                onDelete = { viewModel.deletePhoto(it) },
                                onStartRelive = { list, idx -> viewModel.startRelive(list, idx) },
                                onAddPhotoToAlbum = { photoId, albumId -> viewModel.addPhotoToAlbum(photoId, albumId) },
                                onTogglePhotoPrivate = { viewModel.togglePhotoPrivate(it) }
                            )
                        }

                        // Cinematic Relive Slideshow Overlay
                        if (isPlayingRelive) {
                            ReliveSlideshow(
                                photos = relivePhotos,
                                startIndex = reliveStartIndex,
                                onStopRelive = { viewModel.stopRelive() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeAppIcon(iconTheme: String) {
        val packageManager = packageManager
        val lightAlias = android.content.ComponentName(this, "com.example.MainActivityLight")
        val darkAlias = android.content.ComponentName(this, "com.example.MainActivityDark")
        val isDark = iconTheme == "Dark"

        try {
            if (isDark) {
                // Enable Dark mode launcher icon
                packageManager.setComponentEnabledSetting(
                    darkAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                // Disable Light mode launcher icon
                packageManager.setComponentEnabledSetting(
                    lightAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            } else {
                // Enable Light mode launcher icon
                packageManager.setComponentEnabledSetting(
                    lightAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                // Disable Dark mode launcher icon
                packageManager.setComponentEnabledSetting(
                    darkAlias,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.size(24.dp)
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}
