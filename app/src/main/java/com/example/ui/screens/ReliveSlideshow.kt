package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Photo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReliveSlideshow(
    photos: List<Photo>,
    startIndex: Int,
    onStopRelive: () -> Unit
) {
    if (photos.isEmpty()) {
        onStopRelive()
        return
    }

    val context = LocalContext.current
    var isStackAnimationCompleted by remember { mutableStateOf(false) }

    // Navigation back control
    BackHandler {
        onStopRelive()
    }

    if (!isStackAnimationCompleted) {
        // Physical Unfolding Polaroid Photo Stack Animation
        PhotoStackOpeningAnimation(
            initialPhoto = photos.getOrNull(startIndex) ?: photos[0],
            onAnimationComplete = { isStackAnimationCompleted = true }
        )
    } else {
        // Full Cinematic Slideshow
        CinematicSlideshowPlayer(
            photos = photos,
            startIndex = startIndex,
            onClose = onStopRelive
        )
    }
}

@Composable
fun PhotoStackOpeningAnimation(
    initialPhoto: Photo,
    onAnimationComplete: () -> Unit
) {
    val context = LocalContext.current

    // Animation drivers
    var triggerUnfold by remember { mutableStateOf(false) }

    val liftTransition = updateTransition(targetState = triggerUnfold, label = "LiftStack")

    // Left card rotation and offset
    val leftRotation by liftTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "LeftRotate"
    ) { unfolded ->
        if (unfolded) -12f else 0f
    }
    val leftOffset by liftTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "LeftOffset"
    ) { unfolded ->
        if (unfolded) (-60).dp else 0.dp
    }

    // Right card rotation and offset
    val rightRotation by liftTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "RightRotate"
    ) { unfolded ->
        if (unfolded) 12f else 0f
    }
    val rightOffset by liftTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "RightOffset"
    ) { unfolded ->
        if (unfolded) 60.dp else 0.dp
    }

    // Main central card lift height
    val centralScale by liftTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow) },
        label = "CentralScale"
    ) { unfolded ->
        if (unfolded) 1.08f else 0.9f
    }

    LaunchedEffect(Unit) {
        delay(300) // gentle wait before starting
        triggerUnfold = true
        delay(2200) // display stack before fading into slideshow
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_stack_animation"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow matching photo
        Box(modifier = Modifier.fillMaxSize()) {
            val resourceId = remember(initialPhoto.uri) {
                context.resources.getIdentifier(initialPhoto.uri, "drawable", context.packageName)
            }
            if (resourceId != 0) {
                AsyncImage(
                    model = resourceId,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f
                )
            }
        }

        // Polaroid Frame Pile
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // Stack Item 1 (Underneath Left)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .offset(x = leftOffset)
                    .rotate(leftRotation)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            )

            // Stack Item 2 (Underneath Right)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .offset(x = rightOffset)
                    .rotate(rightRotation)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            )

            // Primary Centered Front Image
            Surface(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .graphicsLayer(
                        scaleX = centralScale,
                        scaleY = centralScale
                    ),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                tonalElevation = 12.dp,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val resourceId = remember(initialPhoto.uri) {
                        context.resources.getIdentifier(initialPhoto.uri, "drawable", context.packageName)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
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
                                model = Uri.parse(initialPhoto.uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Opening Memory...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun CinematicSlideshowPlayer(
    photos: List<Photo>,
    startIndex: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(startIndex) }
    val currentPhoto = photos.getOrNull(currentIndex) ?: photos[0]

    // Panning & zooming animation state
    val zoomAnima = remember { Animatable(1f) }

    // Slideshow loop
    LaunchedEffect(currentIndex) {
        // Reset and trigger soft pan zoom (Ken Burns effect)
        zoomAnima.snapTo(1f)
        zoomAnima.animateTo(
            targetValue = 1.15f,
            animationSpec = tween(6000, easing = LinearEasing)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // each photo displays for 5 seconds
            currentIndex = (currentIndex + 1) % photos.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cinematic_slideshow")
    ) {
        // Ambient background blur
        AnimatedContent(
            targetState = currentPhoto,
            transitionSpec = {
                fadeIn(animationSpec = tween(1500)) togetherWith fadeOut(animationSpec = tween(1500))
            },
            label = "SlideshowAmbient"
        ) { photo ->
            val resourceId = remember(photo.uri) {
                context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (resourceId != 0) {
                    AsyncImage(
                        model = resourceId,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.45f
                    )
                }
            }
        }

        // Central image with Ken Burns slow zoom
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentPhoto,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1500)) togetherWith fadeOut(animationSpec = tween(1500))
                },
                label = "SlideshowMain"
            ) { photo ->
                val resourceId = remember(photo.uri) {
                    context.resources.getIdentifier(photo.uri, "drawable", context.packageName)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomAnima.value,
                            scaleY = zoomAnima.value
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (resourceId != 0) {
                        AsyncImage(
                            model = resourceId,
                            contentDescription = photo.caption,
                            modifier = Modifier.fillMaxWidth(0.95f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = Uri.parse(photo.uri),
                            contentDescription = photo.caption,
                            modifier = Modifier.fillMaxWidth(0.95f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Gradient overlay at top & bottom for elegant readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Close/Exit Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Slideshow",
                tint = Color.White
            )
        }

        // Bottom cinematic overlay (Date & Captions)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(currentPhoto.timestamp))

            Text(
                text = dateStr,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            if (currentPhoto.caption.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentPhoto.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            if (currentPhoto.location.isNotEmpty() && currentPhoto.location != "Unknown") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentPhoto.location,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
