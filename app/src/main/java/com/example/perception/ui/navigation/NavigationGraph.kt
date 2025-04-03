package com.example.perception.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.perception.ui.screens.*
import com.example.perception.ui.screens.SettingsScreen
import com.example.perception.ui.screens.HomeScreen
import java.net.URLDecoder


@Composable
fun NavigationGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {

    NavHost(
        navController,
        startDestination = "home"  // Ensure home is the default start destination
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("anchor") { ViewScreen(navController) }
        composable("stores") { }
        composable("profile") { SettingsScreen(darkMode, onThemeChange) }
        composable(
            route = ARScreen.route,  // Now this will be "ar/{model}"
            arguments = listOf(
                navArgument("model") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val modelPath = URLDecoder.decode(
                backStackEntry.arguments?.getString("model") ?: "",
                "UTF-8"
            )
            ARScreen(navController, modelPath)
        }
    }
}


