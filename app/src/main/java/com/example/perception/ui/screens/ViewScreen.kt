package com.example.perception.ui.screens
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.perception.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.math.roundToInt

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

// Google Drive file holder class for uploads
class GoogleDriveFileHolder {
    var id: String = ""
    var name: String = ""
}

// Progress tracking class
data class UploadProgressState(
    val fileName: String = "",
    val progress: Float = 0f,
    val isUploading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Composable
fun ViewScreen(navController: NavController, context: Context) {
    // Constants
    val APP_NAME = "GLB Uploader"
    val GLB_MIME_TYPE = "model/gltf-binary"
    val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    val ROOT_FOLDER = "root"
    val DRIVE_SPACE = "drive"
    val GLB_FOLDER_NAME = "GLB Models"

    var userModels by remember { mutableStateOf(loadUserModels(context)) }
    var selectedModels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showStatusToast by remember { mutableStateOf(false) }

    // Upload progress state
    var uploadProgressState by remember { mutableStateOf(UploadProgressState()) }
    var showUploadProgressDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val defaultModels = listOf(
        "models/Lamborghini.glb",
        "models/Sofa.glb",
        "models/Pot.glb",
        "models/CornerSofa.glb",
    )
    val availableModels = defaultModels + userModels

    LaunchedEffect(showStatusToast) {
        if (showStatusToast) {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            showStatusToast = false
        }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedPath = saveModelFile(context, it)
                if (savedPath != null) {
                    userModels = loadUserModels(context)
                }
            }
        }

    // Google Drive authorization launcher
    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val authorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data!!)
            statusMessage = "Drive permissions granted"
            showStatusToast = true

            // Now upload the file after permission is granted
            showUploadProgressDialog = true
            uploadSelectedModelsToDrive(
                context,
                selectedModels.toList(),
                coroutineScope,
                onProgress = { fileName, progress, isComplete ->
                    uploadProgressState = uploadProgressState.copy(
                        fileName = fileName,
                        progress = progress,
                        isUploading = !isComplete,
                        isComplete = isComplete
                    )
                },
                onError = { errorMessage ->
                    uploadProgressState = uploadProgressState.copy(
                        error = errorMessage,
                        isUploading = false
                    )
                },
                onComplete = { message ->
                    statusMessage = message
                    showStatusToast = true
                    // Close the dialog after a short delay
                    coroutineScope.launch {
                        delay(2000)
                        showUploadProgressDialog = false
                        uploadProgressState = UploadProgressState()
                    }
                }
            )
        } else {
            statusMessage = "Failed to grant Drive permissions"
            showStatusToast = true
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

    // Upload Progress Dialog
    if (showUploadProgressDialog) {
        Dialog(onDismissRequest = {
            // Don't dismiss while upload is in progress
            if (!uploadProgressState.isUploading) {
                showUploadProgressDialog = false
                uploadProgressState = UploadProgressState()
            }
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uploadProgressState.isComplete) "Upload Complete" else "Uploading to Drive",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uploadProgressState.fileName.isNotEmpty()) {
                        Text(
                            text = uploadProgressState.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LinearProgressIndicator(
                        progress = { uploadProgressState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (uploadProgressState.isComplete)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(uploadProgressState.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (uploadProgressState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uploadProgressState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (!uploadProgressState.isUploading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showUploadProgressDialog = false
                                uploadProgressState = UploadProgressState()
                            }
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBarContent(
                selectionMode,
                selectedModels.size,
                {
                    selectionMode = false
                    selectedModels = emptySet()
                },
                {
                    if (selectedModels.isNotEmpty()) showDeleteConfirmation = true
                },
                onUploadToDrive = {
                    // In the upload to drive button handler
                    if (selectedModels.isNotEmpty() && !selectedModels.any { it in defaultModels }) {
                        // Check user email and request drive permissions if needed
                        coroutineScope.launch {
                            val email = getUserEmailFromDataStore(context)
                            if (email != null) {
                                // Reset the progress state
                                uploadProgressState = UploadProgressState(isUploading = true)

                                requestDrivePermissions(
                                    context,
                                    authorizationLauncher,
                                    { message ->
                                        statusMessage = message
                                        showStatusToast = true
                                    },
                                    {
                                        // This block will be called when permissions are already granted
                                        showUploadProgressDialog = true
                                        uploadSelectedModelsToDrive(
                                            context,
                                            selectedModels.toList(),
                                            coroutineScope,
                                            onProgress = { fileName, progress, isComplete ->
                                                uploadProgressState = uploadProgressState.copy(
                                                    fileName = fileName,
                                                    progress = progress,
                                                    isUploading = !isComplete,
                                                    isComplete = isComplete
                                                )
                                            },
                                            onError = { errorMessage ->
                                                uploadProgressState = uploadProgressState.copy(
                                                    error = errorMessage,
                                                    isUploading = false
                                                )
                                            },
                                            onComplete = { message ->
                                                statusMessage = message
                                                showStatusToast = true
                                                // Close the dialog after a short delay
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    showUploadProgressDialog = false
                                                    uploadProgressState = UploadProgressState()
                                                }
                                            }
                                        )
                                    }
                                )
                            } else {
                                statusMessage = "Please sign in on the Settings screen first"
                                showStatusToast = true
                            }
                        }

                    } else if (selectedModels.any { it in defaultModels }) {
                        statusMessage = "Cannot upload default models to Drive"
                        showStatusToast = true
                    } else {
                        statusMessage = "Please select at least one model"
                        showStatusToast = true
                    }
                },
                showUploadButton = selectedModels.isNotEmpty() && !selectedModels.any { it in defaultModels }
            )

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
    onDelete: () -> Unit,
    onUploadToDrive: () -> Unit,
    showUploadButton: Boolean = false
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

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showUploadButton) {
                    IconButton(onClick = onUploadToDrive) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Upload to Drive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
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

// Request Drive permissions
fun requestDrivePermissions(
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    callback: (String) -> Unit,
    onPermissionsAlreadyGranted: () -> Unit
) {
    val requestedScopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    val authorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(requestedScopes)
        .build()

    Identity.getAuthorizationClient(context)
        .authorize(authorizationRequest)
        .addOnSuccessListener { result ->
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                pendingIntent?.intentSender?.let { intentSender ->
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                    launcher.launch(intentSenderRequest)
                }
            } else {
                callback("Drive permissions already granted")
                onPermissionsAlreadyGranted()  // Call this when permissions are already granted
            }
        }
        .addOnFailureListener { e ->
            callback("Failed to request permissions: ${e.message}")
        }
}

// Get user email from DataStore
suspend fun getUserEmailFromDataStore(context: Context): String? {
    return context.userPreferencesDataStore.data
        .map { preferences -> preferences[androidx.datastore.preferences.core.stringPreferencesKey("user_email")] }
        .firstOrNull()
}

// Upload selected models to Drive with progress tracking
// Upload selected models to Drive with progress tracking
// Upload selected models to Drive with progress tracking
fun uploadSelectedModelsToDrive(
    context: Context,
    selectedModels: List<String>,
    coroutineScope: CoroutineScope,
    onProgress: (fileName: String, progress: Float, isComplete: Boolean) -> Unit,
    onError: (String) -> Unit,
    onComplete: (String) -> Unit
) {
    if (selectedModels.isEmpty()) {
        onError("No models selected for upload")
        return
    }

    coroutineScope.launch {
        val email = getUserEmailFromDataStore(context)
        if (email != null) {
            try {
                var overallSuccess = true
                var totalFiles = selectedModels.size
                var completedFiles = 0

                for (modelPath in selectedModels) {
                    val file = File(modelPath)
                    if (file.exists() && file.isFile) {
                        onProgress(file.name, completedFiles.toFloat() / totalFiles, false)

                        // Define a progress handler that doesn't use withContext
                        val progressHandler = object {
                            fun updateProgress(progress: Float) {
                                val fileWeight = 1.0f / totalFiles
                                val overallProgress = completedFiles.toFloat() / totalFiles + (progress * fileWeight)
                                coroutineScope.launch(Dispatchers.Main) {
                                    onProgress(file.name, overallProgress, false)
                                }
                            }
                        }

                        // Call the suspend function with a normal method reference
                        val uploadResult = uploadFileToDrive(
                            context,
                            email,
                            file,
                            "GLB Uploader",
                            "model/gltf-binary",
                            "GLB Models",
                            "application/vnd.google-apps.folder",
                            "root",
                            "drive",
                            progressHandler::updateProgress
                        )

                        completedFiles++

                        if (!uploadResult) {
                            overallSuccess = false
                        }
                    }
                }

                // Final callback after all uploads complete
                withContext(Dispatchers.Main) {
                    onProgress("", 1.0f, true)

                    if (overallSuccess) {
                        onComplete("Successfully uploaded ${selectedModels.size} model(s) to Drive")
                    } else {
                        onComplete("Some files failed to upload. Please try again.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Upload failed: ${e.message}")
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                onError("Please sign in on the Settings screen first")
            }
        }
    }
}

// Custom OutputStream for tracking progress
class ProgressOutputStream(
    private val outputStream: OutputStream,
    private val expectedSize: Long,
    private val onProgress: (Float) -> Unit
) : OutputStream() {
    private var bytesWritten: Long = 0

    override fun write(b: Int) {
        outputStream.write(b)
        bytesWritten++
        updateProgress()
    }

    override fun write(b: ByteArray) {
        outputStream.write(b)
        bytesWritten += b.size
        updateProgress()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        outputStream.write(b, off, len)
        bytesWritten += len
        updateProgress()
    }

    override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }

    private fun updateProgress() {
        val progress = bytesWritten.toFloat() / expectedSize
        onProgress(if (progress > 1f) 1f else progress)
    }
}

// Upload file to Drive with progress tracking
// Upload file to Drive with progress tracking
private suspend fun uploadFileToDrive(
    context: Context,
    email: String,
    file: File,
    appName: String,
    glbMimeType: String,
    folderName: String,
    folderMimeType: String,
    rootFolder: String,
    driveSpace: String,
    onProgress: (Float) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Set up Google Drive credential
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = Account(email, "com.google")

            // Build Drive service
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName(appName).build()

            // Create GLB models folder
            val folder = createOrGetFolder(driveService, folderName, folderMimeType, rootFolder, driveSpace)

            if (folder != null) {
                val parents = folder.id.let { listOf(it) }
                val metadata = DriveFile()
                    .setParents(parents)
                    .setMimeType(glbMimeType)
                    .setName(file.name)

                // Create a media content
                val fileContent = FileContent(glbMimeType, file)

                // Create a request that supports progress tracking
                val request = driveService.files().create(metadata, fileContent)

                // Configure the media upload to enable tracking
                request.mediaHttpUploader.isDirectUploadEnabled = false

                // Use a simple class to track progress without using coroutines
                val handler = android.os.Handler(android.os.Looper.getMainLooper())

                request.mediaHttpUploader.setProgressListener { uploader ->
                    val progress = uploader.progress.toFloat()  // Convert Double to Float
                    // Post to main thread using Handler instead of withContext
                    handler.post {
                        onProgress(progress)
                    }
                }

                // Set fields and execute
                request.fields = "id, name, size, modifiedTime"
                request.execute()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ViewScreen", "Error uploading GLB file", e)
            false
        }
    }
}

// Create or get folder in Drive
private suspend fun createOrGetFolder(
    driveService: Drive,
    folderName: String,
    folderMimeType: String,
    rootFolder: String,
    driveSpace: String
): GoogleDriveFileHolder? {
    return withContext(Dispatchers.IO) {
        try {
            // Check if folder already exists
            val result = driveService.files().list()
                .setQ("mimeType='$folderMimeType' and name='$folderName' and trashed=false")
                .setSpaces(driveSpace)
                .setFields("files(id, name)")
                .execute()
            if (result.files.isNotEmpty()) {
                // Folder exists, return it
                val folder = GoogleDriveFileHolder()
                folder.id = result.files[0].id
                folder.name = result.files[0].name
                return@withContext folder
            }
            // Create new folder
            val metadata = DriveFile()
                .setParents(listOf(rootFolder))
                .setMimeType(folderMimeType)
                .setName(folderName)
            val folder = driveService.files().create(metadata).execute()
            val fileHolder = GoogleDriveFileHolder()
            fileHolder.id = folder.id
            fileHolder.name = folder.name
            return@withContext fileHolder
        } catch (e: Exception) {
            Log.e("ViewScreen", "Error creating/getting folder", e)
            return@withContext null
        }
    }
}