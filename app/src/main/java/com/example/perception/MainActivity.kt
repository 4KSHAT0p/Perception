package com.example.perception

import AppTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
//import com.example.perception.ui.navigation.ARScreen
//import com.example.perception.ui.navigation.HomeScreen
//import com.example.perception.ui.navigation.ViewScreen
//import com.example.perception.ui.screens.ARScreen
//import com.example.perception.ui.screens.HomeScreen

//package com.example.myapplication

//import android.os.Bundle
//import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.compose.ui.Modifier
import com.example.perception.ui.navigation.BottomNavBar
import com.example.perception.ui.navigation.NavigationGraph


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
    var darkMode by remember { mutableStateOf(false) }  // Manage Dark Mode state
    val navController = rememberNavController()

    AppTheme(darkTheme = darkMode) {  // Apply theme dynamically
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        Scaffold(
            bottomBar = { BottomNavBar( modifier = Modifier,navController,isVisible = currentRoute != "ar/{model}") }
        ) { paddingValues ->
            NavigationGraph(navController, paddingValues, darkMode, onThemeChange = { darkMode = it })
        }
    }
}
