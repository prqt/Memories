package com.example.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// Global thread-safe cache for video durations to avoid retrieving them multiple times
private val durationCache = ConcurrentHashMap<String, String>()

@Composable
fun rememberVideoDuration(videoUriStr: String, context: Context): String {
    var duration by remember(videoUriStr) {
        mutableStateOf(durationCache[videoUriStr] ?: "0:00")
    }

    if (!durationCache.containsKey(videoUriStr)) {
        LaunchedEffect(videoUriStr) {
            val formatted = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    if (videoUriStr.startsWith("content://") || videoUriStr.startsWith("file://")) {
                        retriever.setDataSource(context, Uri.parse(videoUriStr))
                    } else if (videoUriStr.startsWith("http")) {
                        retriever.setDataSource(videoUriStr, HashMap<String, String>())
                    } else {
                        val resId = context.resources.getIdentifier(videoUriStr, "drawable", context.packageName)
                        if (resId != 0) {
                            val assetFileDescriptor = context.resources.openRawResourceFd(resId)
                            retriever.setDataSource(
                                assetFileDescriptor.fileDescriptor,
                                assetFileDescriptor.startOffset,
                                assetFileDescriptor.length
                            )
                        } else {
                            retriever.setDataSource(videoUriStr)
                        }
                    }
                    val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    retriever.release()
                    
                    val durationMs = timeString?.toLongOrNull() ?: 0L
                    val totalSeconds = durationMs / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    String.format("%d:%02d", minutes, seconds)
                } catch (e: Exception) {
                    e.printStackTrace()
                    "0:00"
                }
            }
            durationCache[videoUriStr] = formatted
            duration = formatted
        }
    }

    return duration
}

@Composable
fun VideoDurationIndicator(
    videoUriStr: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val duration = rememberVideoDuration(videoUriStr = videoUriStr, context = context)

    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f), // subtle glass container
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
            text = duration,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 11.sp
            )
        )
    }
}
