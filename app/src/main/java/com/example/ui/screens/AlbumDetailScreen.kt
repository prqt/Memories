package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.VideoDurationIndicator
import com.example.data.model.Album
import com.example.data.model.Photo
import com.example.ui.theme.getAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    photos: List<Photo>,
    allPhotos: List<Photo>,
    accentColorName: String,
    onDismiss: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onStartRelive: (List<Photo>, Int) -> Unit,
    onUpdateAlbum: (Album) -> Unit,
    onDeleteAlbum: (Album) -> Unit,
    onAddPhotosToAlbum: (List<Int>, Int) -> Unit
) {
    val activeAccentColor = getAccentColor(accentColorName)
    val context = LocalContext.current

    BackHandler {
        onDismiss()
    }

    var showEditAlbumSheet by remember { mutableStateOf(false) }
    var showAddPhotosDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("album_detail_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Minimal Header Bar with 3-dot Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Relive Glassmorphic Capsule Button on Top Right
                    if (photos.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .clickable { onStartRelive(photos, 0) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Relive",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // 3-dot Menu
                    var showDropdownMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Album Options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Photos") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showDropdownMenu = false
                                    showAddPhotosDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Album") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showDropdownMenu = false
                                    showEditAlbumSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (album.isPrivate) "Make Public" else "Move to Private Vault") },
                                leadingIcon = { Icon(if (album.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    showDropdownMenu = false
                                    onUpdateAlbum(album.copy(isPrivate = !album.isPrivate))
                                    Toast.makeText(context, if (album.isPrivate) "Moved to Public Gallery" else "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Album") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showDropdownMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Check out my album '${album.emoji} ${album.title}': ${album.description}. Shared from Memories App.")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Album")
                                    context.startActivity(shareIntent)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete Album", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            // Scrollable Content Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                // Header details: Emoji badge, Title, Subtitle
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(activeAccentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = album.emoji, fontSize = 24.sp)
                    }

                    Column {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (album.description.isNotEmpty()) {
                            Text(
                                text = album.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${photos.size} memories",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = activeAccentColor
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No photos added to this journal yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Go to the Camera tab to select and add photos.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(photos) { _, photo ->
                            val resourceId = remember(photo.uri) {
                                context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPhotoClick(photo) }
                            ) {
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
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
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
                        val allEmojis = remember {
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
                                            items(allEmojis.size) { index ->
                                                val emoji = allEmojis[index]
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
                    existingPhotos = photos,
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
}

@Composable
fun AddPhotosToAlbumDialog(
    allPhotos: List<Photo>,
    existingPhotos: List<Photo>,
    onSave: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    activeAccentColor: Color
) {
    val existingIds = remember(existingPhotos) { existingPhotos.map { it.id }.toSet() }
    val availablePhotos = remember(allPhotos) { allPhotos.filter { it.id !in existingIds } }
    val selectedPhotoIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Photos", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            onSave(selectedPhotoIds.toList())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                    ) {
                        Text("Add (${selectedPhotoIds.size})", color = Color.White)
                    }
                }
            }
        },
        text = {
            if (availablePhotos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All available photos are already in this album.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(availablePhotos.size) { index ->
                        val photo = availablePhotos[index]
                        val isSelected = photo.id in selectedPhotoIds
                        val context = LocalContext.current
                        val resId = remember(photo.uri) {
                            context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) {
                                        selectedPhotoIds.remove(photo.id)
                                    } else {
                                        selectedPhotoIds.add(photo.id)
                                    }
                                }
                        ) {
                            if (resId != 0) {
                                AsyncImage(
                                    model = resId,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = Uri.parse(photo.uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (photo.isVideo) {
                                VideoDurationIndicator(
                                    videoUriStr = photo.uri,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) activeAccentColor
                                        else Color.Black.copy(alpha = 0.4f)
                                    )
                                    .border(1.dp, Color.White, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
