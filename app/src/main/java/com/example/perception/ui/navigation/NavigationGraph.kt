package com.example.perception.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.perception.ui.screens.*
import com.example.perception.ui.screens.SettingsScreen
import com.example.perception.ui.screens.NewHomeScreen
import java.net.URLDecoder

import com.google.gson.Gson


@Composable
fun NavigationGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context= LocalContext.current
    NavHost(
        navController,
        startDestination = "home"  // Ensure home is the default start destination
    ) {
        composable("home") {
            NewHomeScreen(navController)
        }
        composable("anchor") { ViewScreen(navController, context = context) }
        composable("stores") { }
        composable("profile") { SettingsScreen(darkMode, onThemeChange) }
//        composable(
//            route = ARScreen.route,  // Now this will be "ar/{model}"
//            arguments = listOf(
//                navArgument("model") {
//                    type = NavType.StringType
//                }
//            )
//        ) { backStackEntry ->
//            val modelPath = Uri.decode(
//                backStackEntry.arguments?.getString("model") ?: ""
//            )
//            ARScreen(navController, modelPath)
//        }
        composable(
            route = ARScreen.route,
            arguments = listOf(
                navArgument("model") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val modelJson = Uri.decode(backStackEntry.arguments?.getString("model") ?: "[]")

            val modelList = Gson().fromJson(modelJson, Array<String>::class.java).toList()

            ARScreen(navController, modelList)
        }

    }
}


