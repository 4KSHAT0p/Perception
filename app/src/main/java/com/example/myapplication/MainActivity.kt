package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.navigation.BottomNavBar
import com.example.myapplication.navigation.NavigationGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }  // Bottom Navigation Bar
    ) { paddingValues ->
        NavigationGraph(navController, paddingValues)  // Navigation Setup
    }
}
