package com.example.perception.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.perception.R
import java.io.File
import java.io.FileOutputStream
import com.google.gson.Gson

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelItem(
    modelName: String,
    modelPath: String,
    thumbnailPath: String?,
    navController: NavController,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    selectionMode: Boolean
) {
    val context = LocalContext.current
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                spotColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                clip = false
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val thumbnailUri = thumbnailPath?.let {
                "file:///android_asset/$it"
            } ?: R.drawable.deflaut

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = modelName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}


@Composable
fun ViewScreen(navController: NavController, context: Context) {
    var userModels by remember { mutableStateOf(loadUserModels(context)) }
    var selectedModels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val defaultModels = listOf(
        "models/Lamborghini.glb",
        "models/Sofa.glb",
        "models/Pot.glb",
        "models/CornerSofa.glb",
    )
    val availableModels = defaultModels + userModels

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedPath = saveModelFile(context, it)
                if (savedPath != null) {
                    userModels = loadUserModels(context)
                }
            }
        }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to delete ${selectedModels.size} model${if (selectedModels.size > 1) "s" else ""}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteModels(context, selectedModels.toList())
                        userModels = loadUserModels(context)
                        selectedModels = emptySet()
                        selectionMode = false
                        showDeleteConfirmation = false
                        Toast.makeText(context, "Selected model(s) deleted", Toast.LENGTH_SHORT)
                            .show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBarContent(selectionMode, selectedModels.size, {
                selectionMode = false
                selectedModels = emptySet()
            }) {
                if (selectedModels.isNotEmpty()) showDeleteConfirmation = true
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .padding(bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(availableModels) { model ->
                    val isDefaultModel = model in defaultModels
                    val isSelected = model in selectedModels
                    val thumbnail = getThumbnailForModel(model)

                    ModelItem(
                        modelName = model.substringAfterLast("/").substringBeforeLast("."),
                        modelPath = model,
                        thumbnailPath = thumbnail,
                        navController = navController,
                        isSelected = isSelected,
                        onLongClick = {
                            if (isDefaultModel) {
                                Toast.makeText(
                                    context,
                                    "Default models cannot be deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                selectionMode = true
                                selectedModels = setOf(model)
                            }
                        },
                        onClick = {
                            if (selectionMode) {
                                if (isDefaultModel) {
                                    Toast.makeText(
                                        context,
                                        "Default models cannot be deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    selectedModels =
                                        if (isSelected) selectedModels - model else selectedModels + model
                                    if (selectedModels.isEmpty()) selectionMode = false
                                }
                            } else {
//                                val encodedModelPath = Uri.encode(model)
//                                navController.navigate("ar/$encodedModelPath")
                                val modelsToSend = listOf(model)
                                val json = Gson().toJson(modelsToSend)
                                val encodedJson = Uri.encode(json)
                                navController.navigate("ar/$encodedJson")

                            }
                        },
                        selectionMode = selectionMode
                    )
                }
            }
        }

        // Floating Upload Button
        FloatingActionButton(
            onClick = { launcher.launch("*/*") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 130.dp,
                    end = 40.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Model")
        }
    }

}

@Composable
fun TopAppBarContent(
    selectionMode: Boolean,
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = if (selectionMode) "$selectedCount Selected" else "Explore Items",
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        if (selectionMode) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("Cancel")
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun getThumbnailForModel(modelPath: String): String? {
    val context = LocalContext.current
    try {
        // Just check if the asset exists
        context.assets.open(modelPath.replace(".glb", ".png")).close()
        return modelPath.replace(".glb", ".png")  // Return the asset path
    } catch (e: Exception) {
        Log.d("Thumbnails", "Asset thumbnail not found: ${modelPath.replace(".glb", ".png")}")
        return null
    }
}

fun loadUserModels(context: Context): List<String> {
    val modelsDir = File(context.filesDir, "models")
    if (!modelsDir.exists()) modelsDir.mkdirs()
    return modelsDir.listFiles()?.filter { it.extension == "glb" }?.map { it.absolutePath }
        ?: emptyList()
}

fun deleteModels(context: Context, modelPaths: List<String>) {
    modelPaths.forEach { path ->
        val file = File(path)
        if (file.exists() && file.isFile) {
            val deleted = file.delete()
            Log.d("ViewScreen", "Model deleted: $path, success: $deleted")

            val thumb = File(path.replace(".glb", ".png"))
            if (thumb.exists()) thumb.delete()
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(nameIndex)
    }
}

fun saveModelFile(context: Context, uri: Uri): String? {
    val allowedExtensions = setOf("glb", "gltf", "obj") // ✅ HashSet for fast lookup
    val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    val originalFileName = getFileNameFromUri(context, uri) ?: return null
    val fileExtension = originalFileName.substringAfterLast('.', "").lowercase()

    if (fileExtension !in allowedExtensions) {
        Toast.makeText(
            context,
            "Unsupported format! Only .glb, .gltf, .obj allowed.",
            Toast.LENGTH_SHORT
        ).show()
        return null
    }

    val file = File(modelsDir, originalFileName)
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
        file.absolutePath
    } catch (e: Exception) {
        Log.e("ViewScreen", "Error: ${e.message}")
        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}

