package com.example.perception

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.perception.ui.navigation.ARScreen
import com.example.perception.ui.navigation.HomeScreen
import com.example.perception.ui.navigation.ViewScreen
import com.example.perception.ui.screens.ARScreen
import com.example.perception.ui.screens.HomeScreen
import com.example.perception.ui.screens.ViewScreen
import com.example.perception.ui.theme.PerceptionTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerceptionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController= rememberNavController()
                    NavHost(navController=navController, startDestination = HomeScreen, modifier = Modifier.padding(innerPadding))
                    {
                        composable<HomeScreen>
                        {
                            HomeScreen(navController)
                        }
                        composable<ARScreen>
                        {
                            ARScreen(navController,"models/car.glb")
                        }
                        composable<ViewScreen>
                        {
                            ViewScreen(navController)
                        }
                    }
                }
            }
        }
    }
}