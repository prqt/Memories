package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "System", // System, Light, Dark
    val accentColor: String = "Blue", // Blue, Purple, Pink, Orange, Mint, Green, Graphite
    val layoutMode: String = "Standard", // Standard, Masonry
    val animationsEnabled: Boolean = true,
    val vaultPin: String? = null,
    val isVaultLocked: Boolean = false,
    val showTimeline: Boolean = true,
    val iconTheme: String = "Light" // Light, Dark
)
