package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { /* Navigate to AR scanning screen */ },
            modifier = Modifier.size(200.dp)
        ) {
            Text(text = "Start AR Scan")
        }
    }
}
