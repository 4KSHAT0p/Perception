package com.example.perception.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.perception.ui.navigation.ViewScreen

@Composable

fun HomeScreen(navController: NavController)
{
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {navController.navigate(ViewScreen)}) {
            Text(text = "View")
        }
        Button(onClick = {}) {
            Text(text = "Quiz")
        }
    }
}