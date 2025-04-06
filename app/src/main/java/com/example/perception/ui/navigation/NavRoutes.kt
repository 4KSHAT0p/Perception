package com.example.perception.ui.navigation
import kotlinx.serialization.Serializable
sealed interface NavigationDestination

@Serializable
data class ARScreen(val model:String) : NavigationDestination {
    companion object {
        const val route = "ar/{model}"
    }
}
