package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Photo
import com.example.data.model.Album
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.getAccentColor
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.viewinterop.AndroidView

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<Photo>,
    initialPhoto: Photo,
    accentColorName: String,
    albums: List<Album> = emptyList(),
    onDismiss: () -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    onToggleCoreMemory: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onStartRelive: (List<Photo>, Int) -> Unit,
    onAddPhotoToAlbum: (Int, Int) -> Unit = { _, _ -> },
    onTogglePhotoPrivate: (Photo) -> Unit = {}
) {
    val context = LocalContext.current
    val activeAccentColor = getAccentColor(accentColorName)

    // Find index of the initial photo in our list
    val initialIndex = remember {
        val idx = photos.indexOfFirst { it.id == initialPhoto.id }
        if (idx == -1) 0 else idx
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: initialPhoto

    var controlsVisible by remember { mutableStateOf(true) }
    var showMetadataSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddToAlbumSheet by remember { mutableStateOf(false) }

    // Intercept back button to dismiss the viewer
    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_viewer_screen")
    ) {
        // Main swipable pager for full photos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            val photo = photos.getOrNull(page)
            if (photo != null) {
                if (photo.isVideo) {
                    VideoPlayerView(
                        videoUriStr = photo.uri,
                        onTap = { controlsVisible = !controlsVisible }
                    )
                } else {
                    ZoomableImage(
                        photo = photo,
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }
            }
        }

        // Overlay controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top controls (Back, Info)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Add to Album Button
                        IconButton(
                            onClick = { showAddToAlbumSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Album",
                                tint = Color.White
                            )
                        }

                        // Vault Private Toggle Button
                        IconButton(
                            onClick = {
                                onTogglePhotoPrivate(currentPhoto)
                                val msg = if (currentPhoto.isPrivate) "Moved to Public Gallery" else "Moved to Private Vault"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                // If private, go back to dismiss or just update
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (currentPhoto.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Toggle Vault Privacy",
                                tint = if (currentPhoto.isPrivate) activeAccentColor else Color.White
                            )
                        }

                        // Google Lens Button
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setPackage("com.google.android.googlequicksearchbox")
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Google Lens is unavailable. Processing locally...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Camera,
                                contentDescription = "Google Lens",
                                tint = Color.White
                            )
                        }

                        // Info Metadata button
                        IconButton(
                            onClick = { showMetadataSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Metadata Details",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Bottom controls (Relive slideshow, favorite, core memory toggle, share, delete)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                ) {
                    // Date & Location Info overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                        val timeSdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val dateStr = sdf.format(Date(currentPhoto.timestamp))
                        val timeStr = timeSdf.format(Date(currentPhoto.timestamp))

                        Text(
                            text = "$dateStr • $timeStr",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (currentPhoto.location.isNotEmpty() && currentPhoto.location != "Unknown") {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentPhoto.location,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Glass Bottom Actions Row
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite toggle
                            IconButton(onClick = { onToggleFavorite(currentPhoto) }) {
                                Icon(
                                    imageVector = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (currentPhoto.isFavorite) Color.Red else Color.White
                                )
                            }

                            // Core Memory toggle
                            IconButton(onClick = { onToggleCoreMemory(currentPhoto) }) {
                                Icon(
                                    imageVector = if (currentPhoto.isCoreMemory) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Core Memory",
                                    tint = if (currentPhoto.isCoreMemory) activeAccentColor else Color.White
                                )
                            }

                            // Cinematic Relive Slideshow
                            Button(
                                onClick = { onStartRelive(photos, pagerState.currentPage) },
                                colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Relive", fontWeight = FontWeight.Bold)
                            }

                            // Share sheet launch
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Revisiting a beautiful memory: ${currentPhoto.caption} at ${currentPhoto.location}")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Memory")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }

                            // Delete photo
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Verification Dialog for Deletion
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Memory?") },
                text = { Text("Are you sure you want to permanently delete this memory? It will be removed from all albums completely.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(currentPhoto)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // EXIF Metadata Sheet
        if (showMetadataSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMetadataSheet = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Information",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    val timeSdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val dateFormatted = sdf.format(Date(currentPhoto.timestamp))
                    val timeFormatted = timeSdf.format(Date(currentPhoto.timestamp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MetadataRow(label = "Date", value = dateFormatted)
                        MetadataRow(label = "Time", value = timeFormatted)
                        MetadataRow(label = "Location", value = currentPhoto.location)
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        MetadataRow(label = "Camera Model", value = currentPhoto.cameraModel)
                        MetadataRow(label = "Lens Settings", value = currentPhoto.lens)
                        MetadataRow(label = "ISO Speed", value = "ISO ${currentPhoto.iso}")
                        MetadataRow(label = "Shutter Speed", value = currentPhoto.shutterSpeed)
                        MetadataRow(label = "Resolution", value = currentPhoto.resolution)
                        MetadataRow(label = "File Size", value = currentPhoto.fileSize)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Add to Album Sheet
        if (showAddToAlbumSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddToAlbumSheet = false },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Add to Album",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (albums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No albums created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(albums) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onAddPhotoToAlbum(currentPhoto.id, album.id)
                                            Toast.makeText(context, "Added to ${album.title}", Toast.LENGTH_SHORT).show()
                                            showAddToAlbumSheet = false
                                        }
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = album.emoji, fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (album.description.isNotEmpty()) {
                                            Text(
                                                text = album.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ZoomableImage(photo: Photo, onTap: () -> Unit) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetState by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scale) {
                if (scale > 1f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetState = androidx.compose.ui.geometry.Offset(
                                x = offsetState.x + pan.x,
                                y = offsetState.y + pan.y
                            )
                        } else {
                            offsetState = androidx.compose.ui.geometry.Offset(0f, 0f)
                        }
                    }
                } else {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                scale = 1.05f
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetState = androidx.compose.ui.geometry.Offset(0f, 0f)
                        } else {
                            scale = 3f
                        }
                    },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val resourceId = remember(photo.uri) {
            context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
        }

        if (resourceId != 0) {
            AsyncImage(
                model = resourceId,
                contentDescription = photo.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetState.x,
                        translationY = offsetState.y
                    ),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback for custom imported local photos outside standard drawable pack
            AsyncImage(
                model = Uri.parse(photo.uri),
                contentDescription = photo.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetState.x,
                        translationY = offsetState.y
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun VideoPlayerView(
    videoUriStr: String,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var videoViewRef by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) } // Default false: Playback does NOT begin immediately
    var hasStartedPlaying by remember { mutableStateOf(false) }
    
    // Playback state tracking
    var currentPositionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    
    // Timeline scrubbing state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(0f) }
    
    // Double-tap visual feedback bubble
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var doubleTapDirection by remember { mutableStateOf(true) } // true for right (+10s), false for left (-10s)
    
    // Volume & Brightness overlays
    var brightnessVal by remember {
        val activity = context as? Activity
        val attrs = activity?.window?.attributes
        val initB = attrs?.screenBrightness ?: 0.5f
        mutableStateOf(if (initB < 0f) 0.5f else initB)
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volumeVal by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    
    // Auto-update playback progress while playing
    LaunchedEffect(isPlaying, isScrubbing) {
        if (isPlaying && !isScrubbing) {
            while (true) {
                videoViewRef?.let {
                    currentPositionMs = it.currentPosition
                    durationMs = it.duration
                }
                delay(250)
            }
        }
    }

    // Function to format milliseconds (e.g. 84000 -> "1:24")
    fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Video View embedded via AndroidView
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp)),
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    val uri = if (videoUriStr.startsWith("http") || videoUriStr.startsWith("content")) {
                        Uri.parse(videoUriStr)
                    } else {
                        val resId = ctx.resources.getIdentifier(videoUriStr, "drawable", ctx.packageName)
                        if (resId != 0) {
                            Uri.parse("android.resource://${ctx.packageName}/$resId")
                        } else {
                            Uri.parse(videoUriStr)
                        }
                    }
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        durationMs = duration
                        // Display the first frame by seeking to 1ms initially without starting
                        seekTo(1)
                    }
                    videoViewRef = this
                }
            },
            update = { videoView ->
                // Handled via states
            }
        )

        // Smooth First Frame Poster Overlay (Fades out when playback starts)
        AnimatedVisibility(
            visible = !hasStartedPlaying,
            exit = fadeOut(animationSpec = tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(videoUriStr)
                    .videoFrameMillis(0)
                    .build(),
                contentDescription = "Video Poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Gestures Detector Overlay (Single Tap, Double Tap, Vertical Drag for Brightness/Volume, Horizontal Drag for Seek)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { offset ->
                            val width = size.width
                            val isRight = offset.x > width / 2
                            videoViewRef?.let { view ->
                                val current = view.currentPosition
                                val newPos = if (isRight) {
                                    (current + 10000).coerceAtMost(view.duration)
                                } else {
                                    (current - 10000).coerceAtLeast(0)
                                }
                                view.seekTo(newPos)
                                currentPositionMs = newPos
                                
                                // Show feedback bubble
                                doubleTapDirection = isRight
                                doubleTapFeedback = if (isRight) "+10s" else "-10s"
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var dragType = 0 // 0: undecided, 1: vertical, 2: horizontal
                    var isLeftHalf = false
                    var startVolume = 0f
                    var startBrightness = 0f
                    var startSeekMs = 0f
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragType = 0
                            isLeftHalf = offset.x < size.width / 2
                            startVolume = volumeVal
                            startBrightness = brightnessVal
                            videoViewRef?.let {
                                startSeekMs = it.currentPosition.toFloat()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            // Decide drag direction type if undecided
                            if (dragType == 0) {
                                dragType = if (abs(dragAmount.y) > abs(dragAmount.x)) 1 else 2
                                if (dragType == 2) {
                                    isScrubbing = true
                                    scrubPositionMs = startSeekMs
                                }
                            }
                            
                            if (dragType == 1) {
                                // Vertical drag
                                val delta = -dragAmount.y / 400f // Swipe up increases
                                if (isLeftHalf) {
                                    showBrightnessIndicator = true
                                    brightnessVal = (startBrightness + delta).coerceIn(0f, 1f)
                                    // Apply to window brightness
                                    (context as? Activity)?.let { activity ->
                                        val attrs = activity.window.attributes
                                        attrs.screenBrightness = brightnessVal
                                        activity.window.attributes = attrs
                                    }
                                } else {
                                    showVolumeIndicator = true
                                    volumeVal = (startVolume + delta).coerceIn(0f, 1f)
                                    // Apply to system audio
                                    val targetVol = (volumeVal * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                }
                            } else if (dragType == 2) {
                                // Horizontal seek drag
                                val deltaMs = (dragAmount.x / size.width.toFloat()) * durationMs
                                scrubPositionMs = (scrubPositionMs + deltaMs).coerceIn(0f, durationMs.toFloat())
                            }
                        },
                        onDragEnd = {
                            if (dragType == 2) {
                                isScrubbing = false
                                videoViewRef?.let { view ->
                                    view.seekTo(scrubPositionMs.toInt())
                                    currentPositionMs = scrubPositionMs.toInt()
                                }
                            }
                            // Auto-fade indicators
                            coroutineScope.launch {
                                delay(1000)
                                showVolumeIndicator = false
                                showBrightnessIndicator = false
                            }
                        }
                    )
                }
        )

        // Clear double-tap feedback bubble after duration
        doubleTapFeedback?.let { text ->
            LaunchedEffect(text) {
                delay(800)
                doubleTapFeedback = null
            }
        }

        // Play/Pause middle floating ripple action button (visible when controls or poster is visible)
        AnimatedVisibility(
            visible = !isPlaying || !hasStartedPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable {
                        videoViewRef?.let {
                            if (it.isPlaying) {
                                it.pause()
                                isPlaying = false
                            } else {
                                it.start()
                                isPlaying = true
                                hasStartedPlaying = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Double-tap visual feedback bubble animation
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (doubleTapDirection) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = doubleTapFeedback ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // System adjustments overlays (Brightness / Volume)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showBrightnessIndicator) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = "Brightness", tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightnessVal)
                                .align(Alignment.BottomStart)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (showVolumeIndicator) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (volumeVal == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volumeVal)
                                .align(Alignment.BottomStart)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Floating thumbnail preview scrubbing popup
        if (isScrubbing && durationMs > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .width(160.dp)
                        .height(100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(videoUriStr)
                                .videoFrameMillis(scrubPositionMs.toLong())
                                .build(),
                            contentDescription = "Preview frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatTime(scrubPositionMs.toInt()),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Glassmorphic Playback Bottom Controls Card
        AnimatedVisibility(
            visible = !isScrubbing, // Hide controls during swipe seeking to focus on the preview frame
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play / Pause button
                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                if (it.isPlaying) {
                                    it.pause()
                                    isPlaying = false
                                } else {
                                    it.start()
                                    isPlaying = true
                                    hasStartedPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Current Time text
                    Text(
                        text = formatTime(if (isScrubbing) scrubPositionMs.toInt() else currentPositionMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                    )

                    // Progress Slider
                    Slider(
                        value = if (isScrubbing) scrubPositionMs else currentPositionMs.toFloat(),
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        onValueChange = {
                            isScrubbing = true
                            scrubPositionMs = it
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            videoViewRef?.let {
                                it.seekTo(scrubPositionMs.toInt())
                                currentPositionMs = scrubPositionMs.toInt()
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Remaining/Total Time text
                    Text(
                        text = formatTime(durationMs),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}
