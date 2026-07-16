package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val LightBg = Color(0xFFF6F6F4)         // Warm Off White
val LightSurface = Color(0xFFFFFFFF)    // Pure White
val LightTextPrimary = Color(0xFF1C1C1E)
val LightTextSecondary = Color(0xFF6D6D72)
val LightDivider = Color(0xFFE5E5EA)

// Dark Theme Colors
val DarkBg = Color(0xFF0B0B0D)          // Deep Black Slate
val DarkSurface = Color(0xFF18181B)     // Charcoal Card
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA1A1AA)
val DarkDivider = Color(0xFF2C2C2E)

// Apple-inspired Accent Colors
val AccentAppleBlue = Color(0xFF007AFF)
val AccentPurple = Color(0xFFAF52DE)
val AccentPink = Color(0xFFFF2D55)
val AccentOrange = Color(0xFFFF9500)
val AccentMint = Color(0xFF00C7BE)
val AccentGreen = Color(0xFF34C759)
val AccentGraphite = Color(0xFF8E8E93)

// Helper to get accent color by name
fun getAccentColor(name: String): Color {
    return when (name) {
        "Purple" -> AccentPurple
        "Pink" -> AccentPink
        "Orange" -> AccentOrange
        "Mint" -> AccentMint
        "Green" -> AccentGreen
        "Graphite" -> AccentGraphite
        else -> AccentAppleBlue // Default "Blue"
    }
}
