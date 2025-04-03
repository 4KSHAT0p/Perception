package com.example.perception

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.perception.ui.screens.ARScreen
//import com.example.perception.ui.screens.HomeScreen
import com.example.perception.ui.screens.HomeScreen
import com.example.perception.ui.screens.ViewScreen
import com.example.perception.ui.theme.PerceptionTheme
import java.net.URLDecoder

//package com.example.myapplication

//import android.os.Bundle
//import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.perception.ui.navigation.BottomNavBar
import com.example.perception.ui.navigation.NavigationGraph
import com.example.perception.ui.screens.AppTheme  // Import custom theme


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
            bottomBar = { BottomNavBar(navController,isVisible = currentRoute != "ar/{model}", modifier = Modifier) }
        ) { paddingValues ->
            NavigationGraph(navController, paddingValues, darkMode, onThemeChange = { darkMode = it })
        }
    }
}
