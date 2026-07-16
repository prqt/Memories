package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.VideoDurationIndicator
import com.example.R
import com.example.data.model.Settings
import com.example.ui.theme.getAccentColor
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: Settings,
    totalPhotosCount: Int,
    albumsCount: Int,
    privatePhotos: List<com.example.data.model.Photo>,
    privateAlbums: List<com.example.data.model.Album>,
    onUpdatePhoto: (com.example.data.model.Photo) -> Unit,
    onUpdateAlbum: (com.example.data.model.Album) -> Unit,
    onSelectPhoto: (com.example.data.model.Photo) -> Unit,
    onSelectAlbum: (com.example.data.model.Album) -> Unit,
    onDismiss: () -> Unit,
    onUpdateSettings: (theme: String?, accent: String?, layout: String?, anims: Boolean?, pin: String?, locked: Boolean?, timeline: Boolean?, iconTheme: String?) -> Unit
) {
    val activeAccentColor = getAccentColor(settings.accentColor)
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showVaultPinPrompt by remember { mutableStateOf(false) }

    if (showVaultPinPrompt && settings.vaultPin != null) {
        PinEntryDialog(
            correctPin = settings.vaultPin,
            onSuccess = {
                showVaultPinPrompt = false
                isVaultUnlocked = true
            },
            onDismiss = {
                showVaultPinPrompt = false
            },
            activeAccentColor = activeAccentColor
        )
    }

    if (isVaultUnlocked) {
        PrivateVaultView(
            privatePhotos = privatePhotos,
            privateAlbums = privateAlbums,
            accentColorName = settings.accentColor,
            onBack = { isVaultUnlocked = false },
            onMakePublic = onUpdatePhoto,
            onMakeAlbumPublic = onUpdateAlbum,
            onSelectPhoto = onSelectPhoto,
            onSelectAlbum = onSelectAlbum
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
            // iOS Custom Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeAccentColor
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // APPEARANCE GROUP CARD
                item {
                    SettingsGroupCard(title = "Appearance") {
                        // 1. Theme Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("System", "Light", "Dark").forEach { theme ->
                                val isActive = settings.themeMode == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isActive) activeAccentColor.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) activeAccentColor else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onUpdateSettings(theme, null, null, null, null, null, null, null) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = theme,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isActive) activeAccentColor else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Icon Theme Selector
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "App Icon Style",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Light", "Dark").forEach { iconTheme ->
                                    val isActive = settings.iconTheme == iconTheme
                                    val iconRes = if (iconTheme == "Light") {
                                        R.drawable.img_app_icon_light_1784194481931
                                    } else {
                                        R.drawable.img_app_icon_dark_1784194494997
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isActive) activeAccentColor.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isActive) activeAccentColor else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onUpdateSettings(null, null, null, null, null, null, null, iconTheme) }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        AsyncImage(
                                            model = iconRes,
                                            contentDescription = "$iconTheme Icon Preview",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$iconTheme Mode",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = if (isActive) activeAccentColor else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Translucent Static Accent Selection Grid (Dedicated Place)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val chunkedAccents = listOf(
                                    listOf("Blue", "Purple", "Pink", "Orange"),
                                    listOf("Mint", "Green", "Graphite")
                                )
                                chunkedAccents.forEach { rowAccents ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowAccents.forEach { colorName ->
                                            val colorValue = getAccentColor(colorName)
                                            val isSelected = settings.accentColor == colorName
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) colorValue.copy(alpha = 0.15f)
                                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.02f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) colorValue.copy(alpha = 0.4f) else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        onUpdateSettings(null, colorName, null, null, null, null, null, null)
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = colorName,
                                                    color = if (isSelected) colorValue else Color.Gray,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                                                )
                                            }
                                        }
                                        if (rowAccents.size < 4) {
                                            repeat(4 - rowAccents.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Gallery Layout Style
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Standard", "Masonry").forEach { layout ->
                                val isActive = settings.layoutMode == layout
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) activeAccentColor else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onUpdateSettings(null, null, layout, null, null, null, null, null) }
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (layout == "Standard") Icons.Outlined.GridView else Icons.Outlined.Dashboard,
                                        contentDescription = null,
                                        tint = if (isActive) activeAccentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (layout == "Standard") "Uniform Grid" else "Masonry",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isActive) activeAccentColor else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Timeline Grouping Toggle (Pinterest layout toggle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Group by Date",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Enable timeline grouping with headers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = settings.showTimeline,
                                onCheckedChange = { checked ->
                                    onUpdateSettings(null, null, null, null, null, null, checked, null)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = activeAccentColor,
                                    checkedTrackColor = activeAccentColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                // PRIVATE VAULT GROUP CARD
                item {
                    SettingsGroupCard(title = "Private Vault") {
                        var currentPinInput by remember { mutableStateOf("") }
                        var newPinInput by remember { mutableStateOf("") }
                        var pinError by remember { mutableStateOf("") }
                        var isTogglingVaultOff by remember { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Vault PIN Protection",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (settings.vaultPin != null) "Active" else "Inactive",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.vaultPin != null,
                                    onCheckedChange = { checked ->
                                        if (!checked) {
                                            isTogglingVaultOff = true
                                        } else {
                                            isTogglingVaultOff = false
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = activeAccentColor,
                                        checkedTrackColor = activeAccentColor.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            if (isTogglingVaultOff) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Verify PIN to Disable Vault",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedTextField(
                                        value = currentPinInput,
                                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) currentPinInput = it },
                                        visualTransformation = PasswordVisualTransformation(),
                                        label = { Text("Enter Current PIN") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { isTogglingVaultOff = false; currentPinInput = "" }) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = {
                                                if (currentPinInput == settings.vaultPin) {
                                                    onUpdateSettings(null, null, null, null, "", false, null, null)
                                                    currentPinInput = ""
                                                    isTogglingVaultOff = false
                                                    pinError = ""
                                                } else {
                                                    pinError = "Incorrect PIN code"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Confirm Disable")
                                        }
                                    }
                                }
                            }

                            if (settings.vaultPin == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Setup a 4-Digit Vault PIN to secure private journals and memories",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newPinInput,
                                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                                            visualTransformation = PasswordVisualTransformation(),
                                            placeholder = { Text("4-Digit PIN") },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = activeAccentColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Button(
                                            onClick = {
                                                if (newPinInput.length == 4) {
                                                    onUpdateSettings(null, null, null, null, newPinInput, false, null, null)
                                                    newPinInput = ""
                                                    pinError = ""
                                                } else {
                                                    pinError = "PIN must be exactly 4 digits"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Set PIN")
                                        }
                                    }
                                }
                            } else if (!isTogglingVaultOff) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "To change your PIN, verify current PIN first:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )

                                    OutlinedTextField(
                                        value = currentPinInput,
                                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) currentPinInput = it },
                                        visualTransformation = PasswordVisualTransformation(),
                                        label = { Text("Current PIN") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeAccentColor)
                                    )

                                    OutlinedTextField(
                                        value = newPinInput,
                                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                                        visualTransformation = PasswordVisualTransformation(),
                                        label = { Text("New 4-Digit PIN") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeAccentColor)
                                    )

                                    Button(
                                        onClick = {
                                            if (currentPinInput != settings.vaultPin) {
                                                pinError = "Current PIN is incorrect"
                                            } else if (newPinInput.length != 4) {
                                                pinError = "New PIN must be 4 digits"
                                            } else {
                                                onUpdateSettings(null, null, null, null, newPinInput, false, null, null)
                                                currentPinInput = ""
                                                newPinInput = ""
                                                pinError = "PIN updated successfully!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Change PIN")
                                    }
                                }
                            }

                            if (pinError.isNotEmpty()) {
                                Text(
                                    text = pinError,
                                    color = if (pinError.contains("successfully")) activeAccentColor else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (settings.vaultPin != null && !isTogglingVaultOff) {
                                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                Button(
                                    onClick = { showVaultPinPrompt = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock & Enter Private Vault", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STORAGE OVERVIEW CARD
                item {
                    SettingsGroupCard(title = "Storage Info") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StorageRowItem(
                                label = "Photos & Videos Count",
                                value = "$totalPhotosCount items",
                                icon = Icons.Outlined.Photo,
                                tint = activeAccentColor
                            )
                            StorageRowItem(
                                label = "Total Journal Books",
                                value = "$albumsCount journals",
                                icon = Icons.Outlined.Book,
                                tint = activeAccentColor
                            )
                            StorageRowItem(
                                label = "Local Database Size",
                                value = "42.5 KB",
                                icon = Icons.Outlined.Storage,
                                tint = activeAccentColor
                            )
                        }
                    }
                }

                // ABOUT INFO CARD
                item {
                    SettingsGroupCard(title = "About") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Memories",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Version 1.0.0",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Private, Local & Beautiful.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeAccentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun PrivateVaultView(
    privatePhotos: List<com.example.data.model.Photo>,
    privateAlbums: List<com.example.data.model.Album>,
    accentColorName: String,
    onBack: () -> Unit,
    onMakePublic: (com.example.data.model.Photo) -> Unit,
    onMakeAlbumPublic: (com.example.data.model.Album) -> Unit,
    onSelectPhoto: (com.example.data.model.Photo) -> Unit,
    onSelectAlbum: (com.example.data.model.Album) -> Unit
) {
    val activeAccentColor = getAccentColor(accentColorName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = activeAccentColor
                )
            }
            Text(
                text = "Private Vault",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Intro Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your Protected Memories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "These items require your 4-digit PIN to access and are completely hidden from your public photo streams.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // Private Journals Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PRIVATE JOURNALS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (privateAlbums.isEmpty()) {
                        Text(
                            text = "No private journals created yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            privateAlbums.forEach { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { onSelectAlbum(album) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = album.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = album.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = album.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onMakeAlbumPublic(album) }) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Unlock Journal",
                                            tint = activeAccentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Private Memories Grid Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PRIVATE MEMORIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (privatePhotos.isEmpty()) {
                        Text(
                            text = "No private photos or videos inside vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            privatePhotos.chunked(3).forEach { rowPhotos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPhotos.forEach { photo ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onSelectPhoto(photo) }
                                        ) {
                                            AsyncImage(
                                                model = androidx.compose.ui.platform.LocalContext.current.resources.getIdentifier(
                                                    photo.uri, "drawable", androidx.compose.ui.platform.LocalContext.current.packageName
                                                ).let { if (it != 0) it else android.net.Uri.parse(photo.uri) },
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            if (photo.isVideo) {
                                                VideoDurationIndicator(
                                                    videoUriStr = photo.uri,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                )
                                            }

                                            // Unlock icon button
                                            IconButton(
                                                onClick = { onMakePublic(photo) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LockOpen,
                                                    contentDescription = "Unlock Photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (rowPhotos.size < 3) {
                                        repeat(3 - rowPhotos.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), // Lesser curved edges matching Apple theme
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun StorageRowItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
