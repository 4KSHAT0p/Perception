package com.example.myapplication.navigation

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController) {
    val screens = listOf(
        NavItem("Home", Icons.Filled.Home, "home"),
        NavItem("Browse", Icons.Filled.Search, "browse"),
        NavItem("Saved", Icons.Filled.Favorite, "saved"),
        NavItem("Settings", Icons.Filled.Settings, "settings")
    )

    NavigationBar {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        screens.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector, val route: String)
