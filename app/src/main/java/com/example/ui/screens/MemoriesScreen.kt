package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Album
import com.example.data.model.Photo
import com.example.ui.theme.getAccentColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoriesScreen(
    photos: List<Photo>,
    albums: List<Album>,
    allPhotos: List<Photo> = emptyList(),
    accentColorName: String,
    settings: com.example.data.model.Settings,
    onStartRelive: (List<Photo>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onCreateAlbum: (title: String, desc: String, emoji: String, accent: String, isPrivate: Boolean) -> Unit,
    onDeleteAlbum: (Album) -> Unit,
    onTogglePinAlbum: (Album) -> Unit,
    onUpdateAlbum: (Album) -> Unit = {},
    onAddPhotosToAlbum: (List<Int>, Int) -> Unit = { _, _ -> }
) {
    val activeAccentColor = getAccentColor(accentColorName)
    val context = LocalContext.current

    val sharedPrefs = remember { context.getSharedPreferences("memories_prefs", android.content.Context.MODE_PRIVATE) }
    var hiddenMemoriesSet by remember {
        mutableStateOf(sharedPrefs.getStringSet("hidden_memories", emptySet()) ?: emptySet())
    }
    val hideMemory: (String) -> Unit = { key ->
        val updated = hiddenMemoriesSet + key
        hiddenMemoriesSet = updated
        sharedPrefs.edit().putStringSet("hidden_memories", updated).apply()
        Toast.makeText(context, "Memory hidden from suggestions", Toast.LENGTH_SHORT).show()
    }
    var showHideMemoryDialog by remember { mutableStateOf(false) }
    var selectedMemoryToHide by remember { mutableStateOf<String?>(null) }

    var showCreateAlbumSheet by remember { mutableStateOf(false) }

    var selectedAlbumForOptions by remember { mutableStateOf<Album?>(null) }
    var showAlbumOptionsSheet by remember { mutableStateOf(false) }
    var showEditAlbumSheet by remember { mutableStateOf(false) }
    var showAddPhotosDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Selected narrative greeting subtitle
    val greetingSubtitle = remember {
        listOf(
            "Every memory tells a story.",
            "Some moments deserve another look.",
            "Let's revisit something beautiful."
        ).random()
    }

    // Today's Memory - select the first photo or group of photos up to 8
    val todaysMemoryPhotos = remember(photos) {
        if (photos.isEmpty()) emptyList()
        else {
            val sorted = photos.sortedByDescending { it.timestamp }
            sorted.take(8)
        }
    }
    val todaysMemoryCover = todaysMemoryPhotos.firstOrNull()

    val todaysMemoryTitle = remember(todaysMemoryCover) {
        if (todaysMemoryCover == null) ""
        else {
            val cal = Calendar.getInstance().apply { timeInMillis = todaysMemoryCover.timestamp }
            val year = cal.get(Calendar.YEAR)
            "A forgotten evening from $year."
        }
    }

    val todaysMemoryCaption = remember(todaysMemoryPhotos) {
        if (todaysMemoryPhotos.isEmpty()) ""
        else {
            val count = todaysMemoryPhotos.size
            val cover = todaysMemoryPhotos.first()
            val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(cover.timestamp))
            "$count photos • $dateStr"
        }
    }

    val isTodayMemoryHidden = remember(todaysMemoryTitle, hiddenMemoriesSet) {
        ("todays_memory_" + todaysMemoryTitle) in hiddenMemoriesSet
    }

    // Curate Memory Timeline (5-8 lightweight suggestions grouped by month)
    data class MemorySuggestion(
        val title: String,
        val caption: String,
        val coverPhoto: Photo,
        val photosList: List<Photo>
    )

    val memoryTimelineSuggestions = remember(photos) {
        if (photos.isEmpty()) emptyList()
        else {
            val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("MMMM", Locale.getDefault())
            
            // Group other photos by Month-Year
            val grouped = photos.groupBy { photo ->
                sdfMonthYear.format(Date(photo.timestamp))
            }

            grouped.entries
                .map { (monthYear, list) ->
                    val cover = list.first()
                    val cal = Calendar.getInstance().apply { timeInMillis = cover.timestamp }
                    val year = cal.get(Calendar.YEAR)
                    val month = sdfMonth.format(Date(cover.timestamp))

                    val title = when {
                        month.contains("June") || month.contains("July") || month.contains("August") -> "Summer looked good on you."
                        month.contains("December") || month.contains("January") || month.contains("February") -> "Cozy winter chapters."
                        month.contains("March") || month.contains("April") || month.contains("May") -> "Spring blooms from $year."
                        else -> "Autumn moments in $year."
                    }
                    val caption = "${list.size} photos • $monthYear"
                    MemorySuggestion(title, caption, cover, list)
                }
                .take(6) // Curate only 5-8 suggestions
        }
    }

    val visibleSuggestions = remember(memoryTimelineSuggestions, hiddenMemoriesSet) {
        memoryTimelineSuggestions.filter { suggestion ->
            val key = suggestion.title + "_" + suggestion.caption
            key !in hiddenMemoriesSet
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
        exit = fadeOut()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("memories_screen"),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp) // Generous vertical spacing between sections
        ) {
            // SECTION 1: GREETING
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 44.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "Memories",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = greetingSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // SECTION 2: TODAY'S MEMORY
            if (todaysMemoryCover != null && !isTodayMemoryHidden) {
                item {
                    val context = LocalContext.current
                    val coverResId = remember(todaysMemoryCover.uri) {
                        context.resources.getIdentifier(todaysMemoryCover.uri, "drawable", context.packageName)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(380.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .combinedClickable(
                                onClick = { onStartRelive(todaysMemoryPhotos, 0) },
                                onLongClick = {
                                    selectedMemoryToHide = "todays_memory_" + todaysMemoryTitle
                                    showHideMemoryDialog = true
                                }
                            )
                    ) {
                        // Image Fill
                        if (coverResId != 0) {
                            AsyncImage(
                                model = coverResId,
                                contentDescription = todaysMemoryTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = Uri.parse(todaysMemoryCover.uri),
                                contentDescription = todaysMemoryTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Very Low Opacity Bottom Dark Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                        )

                        // Info Content overlaid at bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Leftside Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = todaysMemoryTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = todaysMemoryCaption,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Rightside Elegant Glassmorphic Relive Button
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(
                                        width = 0.8.dp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { onStartRelive(todaysMemoryPhotos, 0) }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Relive",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: MEMORY TIMELINE
            if (visibleSuggestions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Suggestions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp) // Whitespace separating rows (no dividers)
                        ) {
                            visibleSuggestions.forEach { suggestion ->
                                val context = LocalContext.current
                                val resId = remember(suggestion.coverPhoto.uri) {
                                    context.resources.getIdentifier(suggestion.coverPhoto.uri, "drawable", context.packageName)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                        .combinedClickable(
                                            onClick = { onStartRelive(suggestion.photosList, 0) },
                                            onLongClick = {
                                                selectedMemoryToHide = suggestion.title + "_" + suggestion.caption
                                                showHideMemoryDialog = true
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Thumbnail (70-80dp, rounded)
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        if (resId != 0) {
                                            AsyncImage(
                                                model = resId,
                                                contentDescription = suggestion.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            AsyncImage(
                                                model = Uri.parse(suggestion.coverPhoto.uri),
                                                contentDescription = suggestion.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    // Right text details
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = suggestion.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = suggestion.caption,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: ALBUMS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Albums with Clickable "See All" and Creator "+" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            // Creator elegant "+" sign
                            IconButton(
                                onClick = { showCreateAlbumSheet = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create Album",
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = activeAccentColor,
                            modifier = Modifier.clickable { /* No-op, visual aesthetic anchor */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (albums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 24.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { showCreateAlbumSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No Albums Yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "Tap here or '+' to create a beautiful journal book.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        // Horizontal scrolling list
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(albums) { album ->
                                val context = LocalContext.current
                                val coverResId = remember(album.coverPhotoUri) {
                                    if (album.coverPhotoUri != null) {
                                        context.resources.getIdentifier(album.coverPhotoUri, "drawable", context.packageName)
                                    } else 0
                                }

                                // Album portrait card: 140 x 180dp
                                Box(
                                    modifier = Modifier
                                        .size(width = 140.dp, height = 180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { onAlbumClick(album) },
                                            onLongClick = {
                                                selectedAlbumForOptions = album
                                                showAlbumOptionsSheet = true
                                            }
                                        )
                                ) {
                                    // Photographic Cover Fill
                                    if (coverResId != 0) {
                                        AsyncImage(
                                            model = coverResId,
                                            contentDescription = album.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (album.coverPhotoUri != null) {
                                        AsyncImage(
                                            model = Uri.parse(album.coverPhotoUri),
                                            contentDescription = album.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Dynamic solid visual fallback with the album accent
                                        val albumAccent = getAccentColor(album.accentColor)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(albumAccent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = album.emoji, fontSize = 42.sp)
                                        }
                                    }

                                    // Soft Dark Gradient Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.45f)
                                                    )
                                                )
                                            )
                                    )

                                    // Meta labels bottom-aligned
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = album.description.ifEmpty { "Journal Book" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Creation
    if (showCreateAlbumSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateAlbumSheet = false },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            CreateAlbumSheetContent(
                onSave = { title, desc, emoji, accent, isPrivate ->
                    onCreateAlbum(title, desc, emoji, accent, isPrivate)
                    showCreateAlbumSheet = false
                },
                onCancel = { showCreateAlbumSheet = false },
                activeAccentColor = activeAccentColor,
                hasVaultPin = settings.vaultPin != null
            )
        }
    }

    // Hide Memory Confirmation Dialog
    if (showHideMemoryDialog && selectedMemoryToHide != null) {
        AlertDialog(
            onDismissRequest = {
                showHideMemoryDialog = false
                selectedMemoryToHide = null
            },
            title = { Text("Hide memory?") },
            text = { Text("Are you sure you want to hide this memory from your suggestions? You won't see it here again.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMemoryToHide?.let { key ->
                            hideMemory(key)
                        }
                        showHideMemoryDialog = false
                        selectedMemoryToHide = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hide", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHideMemoryDialog = false
                    selectedMemoryToHide = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Album Options Bottom Sheet (triggered via hold click / long-click)
    selectedAlbumForOptions?.let { album ->
        if (showAlbumOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAlbumOptionsSheet = false },
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
                        text = "${album.emoji} ${album.title}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAlbumOptionsSheet = false
                                showAddPhotosDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = activeAccentColor)
                        Text("Add Photos", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAlbumOptionsSheet = false
                                showEditAlbumSheet = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = activeAccentColor)
                        Text("Edit Album", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAlbumOptionsSheet = false
                                val updated = album.copy(isPrivate = !album.isPrivate)
                                onUpdateAlbum(updated)
                                Toast.makeText(context, if (album.isPrivate) "Moved to Public Gallery" else "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(if (album.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, tint = activeAccentColor)
                        Text(if (album.isPrivate) "Make Public" else "Move to Private Vault", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAlbumOptionsSheet = false
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out my album '${album.emoji} ${album.title}': ${album.description}. Shared from Memories App.")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Album")
                                context.startActivity(shareIntent)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = activeAccentColor)
                        Text("Share Album", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAlbumOptionsSheet = false
                                showDeleteConfirm = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Delete Album", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Edit Album Bottom Sheet
        if (showEditAlbumSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditAlbumSheet = false },
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
                        text = "Edit Journal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    var editTitle by remember { mutableStateOf(album.title) }
                    var editDesc by remember { mutableStateOf(album.description) }
                    var editEmoji by remember { mutableStateOf(album.emoji) }

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Journal Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            focusedLabelColor = activeAccentColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            focusedLabelColor = activeAccentColor
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Emoji horizontal selector
                    Text(
                        text = "Choose Icon",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val editShowcaseList = remember {
                        listOf("🌲", "🌅", "🏖️", "🏂", "🏕️", "☕", "🏡", "✨", "❤️", "💖", "🐶", "🐱", "🍕", "🚗", "⚽", "🎮")
                    }
                    val editEmojisList = remember {
                        listOf(
                            "🌲", "🌅", "🏖️", "🏂", "🏕️", "☕", "🏡", "✨",
                            "😀", "😃", "😊", "😇", "😍", "🥰", "😎", "🥳", "🤠", "🤩",
                            "❤️", "💖", "💝", "💕", "💞", "💘",
                            "🐶", "🐱", "🐰", "🦊", "🐻", "🐼", "🐨", "🦁", "🌳", "🌴",
                            "🍕", "🍔", "🍟", "🍿", "🍳", "🍩", "🍦", "🍓", "🍉",
                            "🚗", "🚲", "✈️", "🌋", "🏔️", "🏰", "⚽", "🏀", "🎮", "📸"
                        )
                    }
                    var showEditEmojisDialog by remember { mutableStateOf(false) }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(editShowcaseList) { emoji ->
                            val isActive = editEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) activeAccentColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { editEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showEditEmojisDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More Emojis",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (showEditEmojisDialog) {
                        AlertDialog(
                            onDismissRequest = { showEditEmojisDialog = false },
                            title = { Text("Select Icon") },
                            text = {
                                Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(editEmojisList.size) { index ->
                                            val emoji = editEmojisList[index]
                                            val isActive = editEmoji == emoji
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isActive) activeAccentColor.copy(alpha = 0.15f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        editEmoji = emoji
                                                        showEditEmojisDialog = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = emoji, fontSize = 24.sp)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showEditEmojisDialog = false }) {
                                    Text("Close", color = activeAccentColor)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditAlbumSheet = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank()) {
                                    onUpdateAlbum(album.copy(title = editTitle, description = editDesc, emoji = editEmoji))
                                    showEditAlbumSheet = false
                                } else {
                                    Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Delete Album Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Album?") },
                text = { Text("Are you sure you want to permanently delete '${album.title}'? This will not delete the actual photos.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDeleteAlbum(album)
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

        // Add Photos Multi-selector Dialog
        if (showAddPhotosDialog) {
            AddPhotosToAlbumDialog(
                allPhotos = allPhotos,
                existingPhotos = emptyList(), // we let them choose any photo to add
                onSave = { photoIds ->
                    onAddPhotosToAlbum(photoIds, album.id)
                    Toast.makeText(context, "Added ${photoIds.size} photos to ${album.title}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showAddPhotosDialog = false },
                activeAccentColor = activeAccentColor
            )
        }
    }
}

@Composable
fun CreateAlbumSheetContent(
    onSave: (String, String, String, String, Boolean) -> Unit,
    onCancel: () -> Unit,
    activeAccentColor: Color,
    hasVaultPin: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌲") }
    var selectedColorName by remember { mutableStateOf("Blue") } // Defaults to Blue
    var isPrivate by remember { mutableStateOf(false) }
    var showPinWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "New Journal Book",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeAccentColor)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeAccentColor)
        )

        // Emoji Picker
        Column {
            Text(text = "Choose Icon", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val showcaseEmojis = remember {
                listOf("🌲", "🌅", "🏖️", "🏂", "🏕️", "☕", "🏡", "✨", "❤️", "💖", "🐶", "🐱", "🍕", "🚗", "⚽", "🎮")
            }
            val allEmojis = remember {
                listOf(
                    // Nature / Default
                    "🌲", "🌅", "🏖️", "🏂", "🏕️", "☕", "🏡", "✨",
                    // Smileys
                    "😀", "😃", "😊", "😇", "😍", "🥰", "😎", "🥳", "🤠", "🤩", "😜", "😻", "👽", "🦄",
                    // Hearts / Love
                    "❤️", "💖", "💝", "💕", "💞", "💘", "💗", "💓", "💌", "💍",
                    // Nature & Animals
                    "🐶", "🐱", "🐰", "🦊", "🐻", "🐼", "🐨", "🦁", "🐮", "🐷", "🌳", "🌴", "🍀", "🌸", "🍁", "🍂", "❄️", "🔥",
                    // Food & Drinks
                    "🍕", "🍔", "🍟", "🍿", "🍳", "🍩", "🍦", "🍓", "🍉", "🍇", "🍺", "🥂", "🍷", "🥤",
                    // Travel / Sports / Hobbies
                    "🚗", "🚲", "✈️", "🚢", "🌋", "🏔️", "🏕️", "🏰", "🗼", "🎡", "⚽", "🏀", "🏈", "🎾", "🎮", "🎸", "🎨", "📸"
                )
            }
            var showAllEmojisDialog by remember { mutableStateOf(false) }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(showcaseEmojis) { emoji ->
                    val isActive = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) activeAccentColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showAllEmojisDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Emojis",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showAllEmojisDialog) {
                AlertDialog(
                    onDismissRequest = { showAllEmojisDialog = false },
                    title = { Text("Select Icon") },
                    text = {
                        Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(allEmojis.size) { index ->
                                    val emoji = allEmojis[index]
                                    val isActive = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActive) activeAccentColor.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedEmoji = emoji
                                                showAllEmojisDialog = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAllEmojisDialog = false }) {
                            Text("Close", color = activeAccentColor)
                        }
                    }
                )
            }
        }

        // Accent Selection
        Column {
            Text(text = "Theme Accent", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Blue", "Purple", "Pink", "Orange", "Mint", "Green", "Graphite").forEach { colorName ->
                    val colorValue = getAccentColor(colorName)
                    val isActive = selectedColorName == colorName
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colorValue)
                            .border(
                                width = 2.dp,
                                color = if (isActive) MaterialTheme.colorScheme.background else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorName = colorName },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(1.5.dp, colorValue, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Private Journal Switch
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Private Journal",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Requires 4-Digit PIN protection to open",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { checked ->
                        if (checked && !hasVaultPin) {
                            showPinWarning = true
                        } else {
                            isPrivate = checked
                            showPinWarning = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = activeAccentColor,
                        checkedTrackColor = activeAccentColor.copy(alpha = 0.3f)
                    )
                )
            }

            if (showPinWarning) {
                Text(
                    text = "Please set up a 4-Digit Vault PIN in Settings first before creating private journals.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Button(
                onClick = { if (title.isNotEmpty()) onSave(title, description, selectedEmoji, selectedColorName, isPrivate) },
                colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PinEntryDialog(
    correctPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    activeAccentColor: Color
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        confirmButton = {},
        dismissButton = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = activeAccentColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Enter Vault PIN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isError) "Incorrect PIN. Try again." else "Please enter your 4-digit passcode.",
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) activeAccentColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFilled) activeAccentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Numeric keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    )

                    rows.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            rowKeys.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(64.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            .clickable {
                                                isError = false
                                                if (key == "⌫") {
                                                    if (pinInput.isNotEmpty()) {
                                                        pinInput = pinInput.dropLast(1)
                                                    }
                                                } else {
                                                    if (pinInput.length < 4) {
                                                        pinInput += key
                                                        if (pinInput.length == 4) {
                                                            if (pinInput == correctPin) {
                                                                onSuccess()
                                                            } else {
                                                                isError = true
                                                                pinInput = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
