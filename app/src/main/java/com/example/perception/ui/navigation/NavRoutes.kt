package com.example.perception.ui.navigation

import kotlinx.serialization.Serializable

// Sealed interface to group all navigation destinations
sealed interface NavigationDestination

@Serializable
data object HomeScreen : NavigationDestination {
    const val route = "home"
}

@Serializable
data class ARScreen(val model:String) : NavigationDestination {
    companion object {
        const val route = "ar/{model}"
    }
}

@Serializable
data object ViewScreen : NavigationDestination {
    const val route = "view"
}

