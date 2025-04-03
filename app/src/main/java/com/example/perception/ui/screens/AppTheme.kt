package com.example.perception.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFB0B0B0),  // Light gray (inside switch)
            secondary = Color(0xFFE0E0E0), // Softer white
            background = Color(0xFF121212), // True dark mode black
            onPrimary = Color.White        // Text/icons on primary
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFF5F5F5),      // Pure white for light theme
            secondary = Color(0xFF2E2E2E),  // Dark gray (for accents)
            background = Color(0xFFF5F5F5), // Slightly off-white for eye comfort
            onPrimary = Color.Black     // Text/icons on primary
        )
    }


    MaterialTheme(colorScheme = colorScheme, content = content)
}

