package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.VideoDurationIndicator
import com.example.data.model.Photo
import com.example.ui.theme.getAccentColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraScreen(
    photos: List<Photo>,
    layoutMode: String,
    showTimeline: Boolean,
    accentColorName: String,
    onPhotoClick: (Photo) -> Unit,
    onOpenSettings: () -> Unit,
    onPhotoAdded: (Photo) -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    onTogglePhotosPrivate: (List<Photo>) -> Unit
) {
    val context = LocalContext.current
    val activeAccentColor = getAccentColor(accentColorName)

    var isSelectionModeActive by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf(emptySet<Int>()) }

    val onToggleSelect: (Photo) -> Unit = { photo ->
        selectedPhotoIds = if (photo.id in selectedPhotoIds) {
            selectedPhotoIds - photo.id
        } else {
            selectedPhotoIds + photo.id
        }
        if (selectedPhotoIds.isEmpty()) {
            isSelectionModeActive = false
        }
    }

    val onStartSelection: (Photo) -> Unit = { photo ->
        selectedPhotoIds = setOf(photo.id)
        isSelectionModeActive = true
    }

    // Setup visual photo picker for importing real images/videos from emulator/device gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.contains("video", ignoreCase = true) == true
            val newPhoto = Photo(
                uri = uri.toString(),
                timestamp = System.currentTimeMillis(),
                caption = if (isVideo) "Imported Video" else "Imported Memory",
                location = "Local Gallery",
                cameraModel = "Android Device",
                lens = if (isVideo) "Video" else "Wide f/1.8",
                iso = 100,
                shutterSpeed = if (isVideo) "Video" else "1/120s",
                resolution = "1920 x 1080",
                fileSize = "2.4 MB",
                isLocal = true,
                isVideo = isVideo
            )
            onPhotoAdded(newPhoto)
        }
    }

    // Determine Hero photo (most recent)
    val heroPhoto = photos.firstOrNull()

    // Determine Greeting based on current local time
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good Morning,"
        in 12..16 -> "Good Afternoon,"
        in 17..21 -> "Good Evening,"
        else -> "Good Night,"
    }

    // Dynamic greeting subtitles
    val subtitle = remember(photos.size) {
        val list = listOf(
            "Ready to relive today?",
            "Every photo tells a story.",
            "Welcome back.",
            "Your moments are waiting.",
            "Let's revisit today."
        )
        list[photos.size % list.size]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // HERO SECTION
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(540.dp)
                ) {
                    // Hero Image Background (Blurs or falls back if empty)
                    val resourceId = remember(heroPhoto?.uri) {
                        if (heroPhoto != null) {
                            context.resources.getIdentifier(heroPhoto.uri, "drawable", context.packageName)
                        } else 0
                    }

                    if (heroPhoto != null) {
                        if (resourceId != 0) {
                            AsyncImage(
                                model = resourceId,
                                contentDescription = "Hero Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = Uri.parse(heroPhoto.uri),
                                contentDescription = "Hero Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        // Ambient Artwork background when empty
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(activeAccentColor.copy(alpha = 0.5f), Color.DarkGray)
                                    )
                                )
                        )
                    }

                    // Top/Bottom Dark Gradients to keep overlays extremely readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )

                    // Top-Right Glass Profile Portal button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onOpenSettings() },
                            color = Color.White.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Open Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Bottom-Left Greeting Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "prqt",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 34.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Timeline Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    // Standard Floating action to import real images & videos
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor.copy(alpha = 0.12f)),
                        elevation = null,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Render Timeline Photos Grouped by Date
            if (photos.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your memories live here.",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                if (showTimeline) {
                    // Group photos by clean date headers (Today, Yesterday, June 15, 2021)
                    val grouped = photos.groupBy { photo ->
                        val diff = System.currentTimeMillis() - photo.timestamp
                        val days = diff / (1000 * 60 * 60 * 24)
                        when {
                            days == 0L -> "Today"
                            days == 1L -> "Yesterday"
                            else -> {
                                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                sdf.format(Date(photo.timestamp))
                            }
                        }
                    }

                    grouped.forEach { (dateHeader, photosInGroup) ->
                        item {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }

                        item {
                            if (layoutMode == "Masonry") {
                                // Pinterest-style Masonry Grid
                                MasonryPhotoGrid(
                                    photos = photosInGroup,
                                    isSelectionModeActive = isSelectionModeActive,
                                    selectedPhotoIds = selectedPhotoIds,
                                    onToggleSelect = onToggleSelect,
                                    onStartSelection = onStartSelection,
                                    activeAccentColor = activeAccentColor,
                                    onPhotoClick = onPhotoClick
                                )
                            } else {
                                // Standard Uniform Columns Grid
                                StandardPhotoGrid(
                                    photos = photosInGroup,
                                    isSelectionModeActive = isSelectionModeActive,
                                    selectedPhotoIds = selectedPhotoIds,
                                    onToggleSelect = onToggleSelect,
                                    onStartSelection = onStartSelection,
                                    activeAccentColor = activeAccentColor,
                                    onPhotoClick = onPhotoClick
                                )
                            }
                        }
                    }
                } else {
                    // Continuous Single Pinterest-Style Grid
                    item {
                        if (layoutMode == "Masonry") {
                            MasonryPhotoGrid(
                                photos = photos,
                                isSelectionModeActive = isSelectionModeActive,
                                selectedPhotoIds = selectedPhotoIds,
                                onToggleSelect = onToggleSelect,
                                onStartSelection = onStartSelection,
                                activeAccentColor = activeAccentColor,
                                onPhotoClick = onPhotoClick
                            )
                        } else {
                            StandardPhotoGrid(
                                photos = photos,
                                isSelectionModeActive = isSelectionModeActive,
                                selectedPhotoIds = selectedPhotoIds,
                                onToggleSelect = onToggleSelect,
                                onStartSelection = onStartSelection,
                                activeAccentColor = activeAccentColor,
                                onPhotoClick = onPhotoClick
                            )
                        }
                    }
                }
            }
        }

        // Top floating Selection Bar
        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            var showActionsMenu by remember { mutableStateOf(false) }
            var showDeleteConfirmDialog by remember { mutableStateOf(false) }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Delete selected items?") },
                    text = { Text("Are you sure you want to delete these ${selectedPhotoIds.size} items? This cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                val selectedPhotos = photos.filter { it.id in selectedPhotoIds }
                                onDeletePhotos(selectedPhotos)
                                selectedPhotoIds = emptySet()
                                isSelectionModeActive = false
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedPhotoIds = emptySet()
                        isSelectionModeActive = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }

                    Text(
                        text = "${selectedPhotoIds.size} Selected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Selection Actions")
                        }

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = activeAccentColor) },
                                onClick = {
                                    showActionsMenu = false
                                    val selectedPhotos = photos.filter { it.id in selectedPhotoIds }
                                    if (selectedPhotos.isNotEmpty()) {
                                        val uris = selectedPhotos.map { Uri.parse(it.uri) }
                                        val shareIntent = Intent().apply {
                                            action = if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
                                            if (uris.size > 1) {
                                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                            } else {
                                                putExtra(Intent.EXTRA_STREAM, uris.first())
                                            }
                                            type = "image/*"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share selected memories"))
                                    }
                                    selectedPhotoIds = emptySet()
                                    isSelectionModeActive = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Move to Private Vault") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = activeAccentColor) },
                                onClick = {
                                    showActionsMenu = false
                                    val selectedPhotos = photos.filter { it.id in selectedPhotoIds }
                                    onTogglePhotosPrivate(selectedPhotos)
                                    selectedPhotoIds = emptySet()
                                    isSelectionModeActive = false
                                    Toast.makeText(context, "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showActionsMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandardPhotoGrid(
    photos: List<Photo>,
    isSelectionModeActive: Boolean,
    selectedPhotoIds: Set<Int>,
    onToggleSelect: (Photo) -> Unit,
    onStartSelection: (Photo) -> Unit,
    activeAccentColor: Color,
    onPhotoClick: (Photo) -> Unit
) {
    val context = LocalContext.current
    val rowSize = 3
    val chunked = photos.chunked(rowSize)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        chunked.forEach { rowPhotos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rowPhotos.forEach { photo ->
                    val isSelected = photo.id in selectedPhotoIds
                    val borderStroke = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, activeAccentColor)
                    } else null

                    val scale = if (isSelected) 0.93f else 1f
                    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = scale,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        )
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .graphicsLayer(
                                scaleX = animatedScale,
                                scaleY = animatedScale
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .combinedClickable(
                                onLongClick = { onStartSelection(photo) },
                                onClick = {
                                    if (isSelectionModeActive) {
                                        onToggleSelect(photo)
                                    } else {
                                        onPhotoClick(photo)
                                    }
                                }
                            )
                    ) {
                        val resourceId = remember(photo.uri) {
                            context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                        }
                        if (resourceId != 0) {
                            AsyncImage(
                                model = resourceId,
                                contentDescription = photo.caption,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = Uri.parse(photo.uri),
                                contentDescription = photo.caption,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (photo.isVideo) {
                            VideoDurationIndicator(
                                videoUriStr = photo.uri,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            )
                        }

                        // Checkbox/Selection indicator
                        if (isSelectionModeActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) activeAccentColor
                                        else Color.Black.copy(alpha = 0.4f)
                                    )
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // Fill remaining weights with blank spaces to keep grid items aligned
                val remainder = rowSize - rowPhotos.size
                if (remainder > 0) {
                    repeat(remainder) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MasonryPhotoGrid(
    photos: List<Photo>,
    isSelectionModeActive: Boolean,
    selectedPhotoIds: Set<Int>,
    onToggleSelect: (Photo) -> Unit,
    onStartSelection: (Photo) -> Unit,
    activeAccentColor: Color,
    onPhotoClick: (Photo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val col1 = photos.filterIndexed { index, _ -> index % 2 == 0 }
        val col2 = photos.filterIndexed { index, _ -> index % 2 != 0 }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            col1.forEach { photo ->
                MasonryItem(
                    photo = photo,
                    isSelectionModeActive = isSelectionModeActive,
                    selectedPhotoIds = selectedPhotoIds,
                    onToggleSelect = onToggleSelect,
                    onStartSelection = onStartSelection,
                    activeAccentColor = activeAccentColor,
                    onPhotoClick = onPhotoClick
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            col2.forEach { photo ->
                MasonryItem(
                    photo = photo,
                    isSelectionModeActive = isSelectionModeActive,
                    selectedPhotoIds = selectedPhotoIds,
                    onToggleSelect = onToggleSelect,
                    onStartSelection = onStartSelection,
                    activeAccentColor = activeAccentColor,
                    onPhotoClick = onPhotoClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasonryItem(
    photo: Photo,
    isSelectionModeActive: Boolean,
    selectedPhotoIds: Set<Int>,
    onToggleSelect: (Photo) -> Unit,
    onStartSelection: (Photo) -> Unit,
    activeAccentColor: Color,
    onPhotoClick: (Photo) -> Unit
) {
    val context = LocalContext.current

    val heightDp = remember(photo.id) {
        when (photo.id % 3) {
            0 -> 160.dp
            1 -> 240.dp
            else -> 200.dp
        }
    }

    val isSelected = photo.id in selectedPhotoIds
    val borderStroke = if (isSelected) {
        androidx.compose.foundation.BorderStroke(2.dp, activeAccentColor)
    } else null

    val scale = if (isSelected) 0.93f else 1f
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = scale,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale
            )
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(8.dp))
                else Modifier
            )
            .combinedClickable(
                onLongClick = { onStartSelection(photo) },
                onClick = {
                    if (isSelectionModeActive) {
                        onToggleSelect(photo)
                    } else {
                        onPhotoClick(photo)
                    }
                }
            )
    ) {
        val resourceId = remember(photo.uri) {
            context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
        }
        if (resourceId != 0) {
            AsyncImage(
                model = resourceId,
                contentDescription = photo.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = Uri.parse(photo.uri),
                contentDescription = photo.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (photo.isVideo) {
            VideoDurationIndicator(
                videoUriStr = photo.uri,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }

        // Checkbox/Selection indicator
        if (isSelectionModeActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) activeAccentColor
                        else Color.Black.copy(alpha = 0.4f)
                    )
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


