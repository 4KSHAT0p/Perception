package com.example.myapplication.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.*
import com.example.myapplication.ui.theme.HomeScreen

@Composable
fun NavigationGraph(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen { navController.navigate("ar") } }
        composable("browse") { BrowseScreen() }
        composable("saved") { SavedScreen() }
        composable("settings") { SettingsScreen() }
        composable("ar") { ARScreen() }
    }
}
