package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.VideoDurationIndicator
import com.example.data.model.Photo
import com.example.ui.theme.getAccentColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    photos: List<Photo>,
    accentColorName: String,
    onPhotoClick: (Photo) -> Unit
) {
    val context = LocalContext.current
    val activeAccentColor = getAccentColor(accentColorName)

    // Calendar state (Set default to July 2026, matching local date 2026-07-15)
    val calendar = remember { Calendar.getInstance().apply { set(2026, Calendar.JULY, 1) } }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    // Hold state for long press micro-thumbnail preview
    var previewPhoto by remember { mutableStateOf<Photo?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Selected date details
    var selectedDayPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var selectedDayLabel by remember { mutableStateOf("") }
    var selectedDay by remember(currentMonth, currentYear) { mutableStateOf<Int?>(null) }

    // Recalculate days in month
    val daysInMonth = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val monthName = remember(currentMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }
        sdf.format(cal.time)
    }

    // Median Reference M calculation (excluding 0-photo days)
    val medianReference = remember(photos) {
        if (photos.isEmpty()) return@remember 1.0

        val counts = photos.groupBy { photo ->
            val cal = Calendar.getInstance().apply { timeInMillis = photo.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }.map { it.value.size }

        if (counts.isEmpty()) return@remember 1.0

        val sorted = counts.sorted()
        val mid = sorted.size / 2
        if (sorted.size % 2 != 0) {
            sorted[mid].toDouble()
        } else {
            (sorted[mid - 1] + sorted[mid]).toDouble() / 2.0
        }
    }

    // Helper to calculate photo counts & first photo for a day
    fun getDayDetails(day: Int): Pair<Int, Photo?> {
        val dayPhotos = photos.filter { photo ->
            val cal = Calendar.getInstance().apply { timeInMillis = photo.timestamp }
            cal.get(Calendar.YEAR) == currentYear &&
                    cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == day
        }
        return Pair(dayPhotos.size, dayPhotos.firstOrNull())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendar Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Explore life's rhythm through density.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month Selector Card
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp)), // Lesser curved edges
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == Calendar.JANUARY) {
                            currentMonth = Calendar.DECEMBER
                            currentYear -= 1
                        } else {
                            currentMonth -= 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        if (currentMonth == Calendar.DECEMBER) {
                            currentMonth = Calendar.JANUARY
                            currentYear += 1
                        } else {
                            currentMonth += 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Days Grid Titles (Sun - Sat)
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.9f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of dates with liquid vertical fill
        val columns = 7
        val calStartOffset = remember(currentMonth, currentYear) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, currentYear)
            cal.set(Calendar.MONTH, currentMonth)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed Sun=0
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Render blank offset boxes before the 1st
            items(calStartOffset) {
                Box(modifier = Modifier.size(42.dp))
            }

            // Render calendar days
            items(daysInMonth) { index ->
                val day = index + 1
                val (count, firstPhoto) = getDayDetails(day)

                // Fill ratio formula: R = Min(Count / (3 * M), 1.0)
                val fillRatio = if (count > 0) {
                    kotlin.math.min(count.toDouble() / (3.0 * medianReference), 1.0).toFloat()
                } else 0f

                val todayCal = remember { Calendar.getInstance() }
                val isToday = todayCal.get(Calendar.YEAR) == currentYear &&
                        todayCal.get(Calendar.MONTH) == currentMonth &&
                        todayCal.get(Calendar.DAY_OF_MONTH) == day

                 DateLiquidCard(
                    day = day,
                    count = count,
                    firstPhoto = firstPhoto,
                    fillRatio = fillRatio,
                    activeAccentColor = activeAccentColor,
                    isToday = isToday,
                    isSelected = selectedDay == day,
                    onTap = {
                        selectedDay = day
                        val selectedCal = Calendar.getInstance().apply {
                            set(currentYear, currentMonth, day)
                        }
                        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                        selectedDayLabel = sdf.format(selectedCal.time)
                        selectedDayPhotos = photos.filter { photo ->
                            val pCal = Calendar.getInstance().apply { timeInMillis = photo.timestamp }
                            pCal.get(Calendar.YEAR) == currentYear &&
                                    pCal.get(Calendar.MONTH) == currentMonth &&
                                    pCal.get(Calendar.DAY_OF_MONTH) == day
                        }
                    },
                    onLongPress = { photo ->
                        if (photo != null) {
                            previewPhoto = photo
                            showPreviewDialog = true
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Day memories list section
        if (selectedDayPhotos.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(12.dp)) // Lesser curved edges
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedDayLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Captured ${selectedDayPhotos.size} moments on this day.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedDayPhotos.forEach { photo ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPhotoClick(photo) }
                        ) {
                            val resourceId = remember(photo.uri) {
                                context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                            }
                            if (resourceId != 0) {
                                AsyncImage(
                                    model = resourceId,
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
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Micro-thumbnail Long Press Hover/Hold Dialog
    if (showPreviewDialog && previewPhoto != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = {
                val cal = Calendar.getInstance().apply { timeInMillis = previewPhoto!!.timestamp }
                val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                Text(text = "Memory Preview • ${sdf.format(cal.time)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                val photo = previewPhoto!!
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val resourceId = remember(photo.uri) {
                        context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp)) // Lesser curved edges
                    ) {
                        if (resourceId != 0) {
                            AsyncImage(
                                model = resourceId,
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
                                    .padding(8.dp)
                            )
                        }
                    }
                    if (photo.caption.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = photo.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPreviewDialog = false
                        onPhotoClick(previewPhoto!!)
                    }
                ) {
                    Text("Open Immersive View", color = activeAccentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DateLiquidCard(
    day: Int,
    count: Int,
    firstPhoto: Photo?,
    fillRatio: Float, // 0.0 to 1.0
    activeAccentColor: Color,
    isToday: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: (Photo?) -> Unit
) {
    val fillHeightPercent = animateFloatAsState(
        targetValue = fillRatio,
        animationSpec = tween(1200, easing = EaseOutBack),
        label = "LiquidFillAnima"
    )

    // Blend color from soft accent background to deep brand highlight based on density ratio
    val interpolatedColor = remember(fillRatio, activeAccentColor) {
        val ratio = fillRatio.coerceIn(0f, 1f)
        Color(
            red = activeAccentColor.red * ratio + (1f - ratio) * 0.9f,
            green = activeAccentColor.green * ratio + (1f - ratio) * 0.9f,
            blue = activeAccentColor.blue * ratio + (1f - ratio) * 0.95f,
            alpha = if (ratio > 0f) 0.85f else 0.05f
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "todayBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    // Hover spring-based scaling animation
    val scale = if (isSelected) 1.08f else 1f
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HoverScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale
            )
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) activeAccentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)
            )
            .then(
                when {
                    isSelected -> {
                        Modifier.border(
                            width = 2.dp,
                            color = activeAccentColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    isToday -> {
                        Modifier.border(
                            width = 2.dp,
                            color = activeAccentColor.copy(alpha = borderAlpha),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    else -> {
                        Modifier.border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            )
            .pointerInput(firstPhoto) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress(firstPhoto) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical Liquid Container Draw
        if (count > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val height = size.height * fillHeightPercent.value
                        val y = size.height - height

                        // Bottom-up fill rectangle
                        drawRect(
                            color = interpolatedColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                            size = androidx.compose.ui.geometry.Size(size.width, height)
                        )
                    }
            )
        }

        // Day label text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || isSelected || count > 0) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) activeAccentColor else if (isToday) activeAccentColor else if (count > 0) Color.DarkGray else MaterialTheme.colorScheme.onBackground
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(activeAccentColor)
                )
            }
        }
    }
}
