package com.example.perception.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Repository for managing model files and their state across the application.
 * Provides a shared model list between ViewScreen and other screens.
 */
object ModelRepository {
    // Default models that come with the app
    private val defaultModels = listOf(
        "models/Lamborghini.glb",
        "models/Sofa.glb",
        "models/Pot.glb",
        "models/TwoSeatSofa.glb",
        "models/Cabinet.glb",
        "models/Cabinet2.glb",
        "models/Chair.glb",
        "models/Chest.glb",
        "models/Horse.glb",
        "models/Vase.glb",
        "models/Statue.glb",
        "models/wooden_table.glb",
        "models/chair_table.glb"
    )

    // Combined list of all models (default + user models)
    private val _availableModels = mutableStateListOf<String>()
    val availableModels: SnapshotStateList<String> = _availableModels

    // Currently selected model
    private val _selectedModelPath = MutableStateFlow<String?>(null)
    val selectedModelPath: StateFlow<String?> = _selectedModelPath.asStateFlow()

    /**
     * Initializes the repository with default and user models
     */
    fun initialize(context: Context) {
        val userModels = loadUserModels(context)
        _availableModels.clear()
        _availableModels.addAll(defaultModels)
        _availableModels.addAll(userModels)

        // Select first model by default if none is selected
        if (_selectedModelPath.value == null && _availableModels.isNotEmpty()) {
            _selectedModelPath.value = _availableModels[0]
        }
    }

    /**
     * Load user models from the app's files directory
     */
    private fun loadUserModels(context: Context): List<String> {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        return modelsDir.listFiles()
            ?.filter { it.extension == "glb" }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    /**
     * Update the collection of user models
     */
    fun updateUserModels(context: Context) {
        val currentSelectedModel = _selectedModelPath.value
        val userModels = loadUserModels(context)

        _availableModels.clear()
        _availableModels.addAll(defaultModels)
        _availableModels.addAll(userModels)

        // If the previously selected model still exists, keep it at the beginning
        if (currentSelectedModel != null && (_availableModels.contains(currentSelectedModel))) {
            _availableModels.remove(currentSelectedModel)
            _availableModels.add(0, currentSelectedModel)
            _selectedModelPath.value = currentSelectedModel
        } else {
            // If the previously selected model was deleted, default to the first available model
            _selectedModelPath.value = _availableModels.firstOrNull()
        }
    }

    /**
     * Check if the model is a default model
     */
    fun isDefaultModel(modelPath: String): Boolean {
        return modelPath in defaultModels
    }

    /**
     * Select a model and move it to the beginning of the list
     */
    fun selectModel(model: String) {
        _selectedModelPath.value = model

        // Remove the model from its current position if it exists
        if (_availableModels.contains(model)) {
            _availableModels.remove(model)
        }

        // Add it to the beginning of the list
        _availableModels.add(0, model)
    }
}