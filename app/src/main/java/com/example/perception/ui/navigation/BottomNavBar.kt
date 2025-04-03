package com.example.perception.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController, isVisible: Boolean = true, modifier: Modifier) {

    val screens = listOf(
        NavItem("Home", Icons.Default.Home, "home"),
        NavItem("Anchor", Icons.Default.Build, "anchor"),
        NavItem("Stores", Icons.Default.ShoppingCart, "stores"),
        NavItem("Profile", Icons.Default.Person, "profile")
    )

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier.fillMaxWidth(),
        enter = slideInVertically(
            initialOffsetY = { height -> height },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { height -> height },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        NavigationBar() {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route

            screens.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            // Optional: Add visual feedback
                            tint = if (currentRoute == item.route)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        // Improved navigation with stack management
                        navController.navigate(item.route) {
                            // Pop up to root destination to avoid building large back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Reuse existing instance instead of creating multiple
                            launchSingleTop = true
                            // Restore state when re-selecting the same item
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,    // Change icon color when selected
                        indicatorColor = Color.Black       // Background behind selected item
                    )
                )
            }
        }
    }
}

// Enhanced NavItem with optional badge or additional metadata
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)