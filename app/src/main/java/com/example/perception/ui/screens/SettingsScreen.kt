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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import java.util.*
import kotlin.math.roundToInt
import com.google.api.services.drive.model.File as DriveFile

// DataStore extension function
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "glb_upload_user"
)

// Download progress state
data class DownloadProgressState(
    val fileName: String = "",
    val progress: Float = 0f,
    val isDownloading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    // Constants
    val TAG = "SettingsScreen"
    val APP_NAME = "GLB Uploader"
    val GLB_MIME_TYPE = "model/gltf-binary"
    val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    val ROOT_FOLDER = "root"
    val DRIVE_SPACE = "drive"
    val GLB_FOLDER_NAME = "GLB Models"
    val EMAIL_KEY = "user_email"
    val FIELDS = "nextPageToken, files(id, name, size, modifiedTime)"

    // Context and scope
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // States
    var statusMessage by remember { mutableStateOf("Please sign in to continue") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var driveGlbFiles by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var showFileSelectionDialog by remember { mutableStateOf(false) }
    var downloadLocation by remember { mutableStateOf<Uri?>(null) }

    // Download progress tracking
    var downloadProgressState by remember { mutableStateOf(DownloadProgressState()) }
    var showDownloadProgressDialog by remember { mutableStateOf(false) }

    // Check login status on first load
    LaunchedEffect(Unit) {
        val email = getUserEmail(context)
        if (email != null) {
            statusMessage = "Signed in as: $email"
            isLoggedIn = true
        }
    }

    // Directory picker launcher
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            downloadLocation = it
            showFileSelectionDialog = true
        }
    }

    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                statusMessage = "Storage permission granted"
            } else {
                statusMessage = "Storage permission denied"
                Toast.makeText(context, "Storage permission is required for downloading files", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Google sign-in
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle sign-in result if needed
    }

    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sign in button
            Button(
                onClick = {
                    coroutineScope.launch {
                        performGoogleLogin(context) { success, message ->
                            if (success) {
                                isLoggedIn = true
                                statusMessage = message
                            } else {
                                statusMessage = message
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = !isLoggedIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Sign In with Google")
            }

            // Download GLB file
            Button(
                onClick = {
                    coroutineScope.launch {
                        val email = getUserEmail(context)
                        if (email != null) {
                            statusMessage = "Fetching available GLB files..."
                            listGlbFiles(
                                context,
                                email,
                                APP_NAME,
                                GLB_MIME_TYPE,
                                DRIVE_SPACE,
                                FIELDS
                            ) { success, message, files ->
                                if (success && files.isNotEmpty()) {
                                    driveGlbFiles = files
                                    directoryPicker.launch(null)
                                } else {
                                    statusMessage = "No GLB files found in your Drive"
                                }
                            }
                        } else {
                            statusMessage = "Please sign in first"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = isLoggedIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Download GLB from Drive")
            }

            // Sign out button
            Button(
                onClick = {
                    coroutineScope.launch {
                        performLogout(context)
                        isLoggedIn = false
                        statusMessage = "Signed out"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = isLoggedIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Sign Out")
            }
        }

        // Dark Mode Toggle - positioned just above where the profile button would be
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp),  // Positioned above where a bottom navigation item would be
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = darkMode,
                onCheckedChange = { onThemeChange(it) },
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }

    // File selection dialog
    if (showFileSelectionDialog && driveGlbFiles.isNotEmpty() && downloadLocation != null) {
        AlertDialog(
            onDismissRequest = { showFileSelectionDialog = false },
            title = { Text("Select GLB File to Download") },
            text = {
                Column {
                    driveGlbFiles.forEach { file ->
                        Button(
                            onClick = {
                                showFileSelectionDialog = false
                                showDownloadProgressDialog = true
                                downloadProgressState = DownloadProgressState(
                                    fileName = file.name,
                                    isDownloading = true
                                )

                                coroutineScope.launch {
                                    val email = getUserEmail(context)
                                    if (email != null) {
                                        downloadGlbFile(
                                            context,
                                            email,
                                            file.id,
                                            file.name,
                                            downloadLocation!!,
                                            GLB_MIME_TYPE,
                                            APP_NAME,
                                            storagePermissionLauncher,
                                            onProgress = { progress ->
                                                downloadProgressState = downloadProgressState.copy(
                                                    progress = progress
                                                )
                                            }
                                        ) { success, message ->
                                            statusMessage = message
                                            downloadProgressState = downloadProgressState.copy(
                                                isDownloading = false,
                                                isComplete = success,
                                                error = if (!success) message else null,
                                                progress = if (success) 1f else downloadProgressState.progress
                                            )

                                            // Close progress dialog after a delay if successful
                                            if (success) {
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    showDownloadProgressDialog = false
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(file.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { showFileSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Download Progress Dialog
    if (showDownloadProgressDialog) {
        Dialog(onDismissRequest = {
            // Don't dismiss while download is in progress
            if (!downloadProgressState.isDownloading) {
                showDownloadProgressDialog = false
                downloadProgressState = DownloadProgressState()
            }
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (downloadProgressState.isComplete) "Download Complete" else "Downloading from Drive",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = downloadProgressState.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { downloadProgressState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (downloadProgressState.isComplete)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(downloadProgressState.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (downloadProgressState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = downloadProgressState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (!downloadProgressState.isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showDownloadProgressDialog = false
                                downloadProgressState = DownloadProgressState()
                            }
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
// Helper functions

// Check for storage permission
private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Request storage permission
private fun requestStoragePermission(context: Context, launcher: ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            launcher.launch(intent)
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            launcher.launch(intent)
        }
    } else {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            100
        )
    }
}

// Save data to DataStore
private suspend fun saveData(context: Context, key: String, value: String) {
    context.userPreferencesDataStore.edit { preferences ->
        preferences[stringPreferencesKey(key)] = value
    }
}

// Get data from DataStore
private suspend fun getData(context: Context, key: String): String? {
    return context.userPreferencesDataStore.data
        .map { preferences -> preferences[stringPreferencesKey(key)] }
        .firstOrNull()
}

// Get user email from DataStore
private suspend fun getUserEmail(context: Context): String? {
    return getData(context, "user_email")
}

// Google login
private suspend fun performGoogleLogin(
    context: Context,
    callback: (Boolean, String) -> Unit
) {
    val rawNonce = UUID.randomUUID().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId("761360596769-k95jrn3qala76ufssaec47qc23vpjnc1.apps.googleusercontent.com")
        .setAutoSelectEnabled(true)
        .setNonce(hashedNonce)
        .build()

    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        handleSignInResult(context, result, callback)
    } catch (e: GetCredentialException) {
        callback(false, "Login failed: ${e.message}")
        Log.e("SettingsScreen", "Error getting credential", e)
    }
}

// Handle sign-in result
private suspend fun handleSignInResult(
    context: Context,
    result: GetCredentialResponse,
    callback: (Boolean, String) -> Unit
) {
    when (val credential = result.credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val name = googleIdTokenCredential.displayName ?: "User"

                    saveData(context, "user_email", email)
                    callback(true, "Signed in as: $email")
                } catch (e: Exception) {
                    callback(false, "Failed to parse credentials")
                    Log.e("SettingsScreen", "Error parsing Google ID token", e)
                }
            }
        }
        is PublicKeyCredential -> {
            callback(false, "Unsupported credential type")
        }
        else -> {
            callback(false, "Unsupported credential type")
        }
    }
}

// Logout
private suspend fun performLogout(context: Context) {
    val credentialManager = CredentialManager.create(context)
    context.userPreferencesDataStore.edit { it.clear() }
    credentialManager.clearCredentialState(ClearCredentialStateRequest())
}

// Get GLB files from Drive
private suspend fun getGlbFilesFromDrive(
    driveService: Drive,
    glbMimeType: String,
    driveSpace: String,
    fields: String
): List<DriveFile> {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<DriveFile>()
        var pageToken: String? = null
        do {
            val fileList = driveService.files().list()
                .setQ("mimeType='$glbMimeType' and trashed=false")
                .setSpaces(driveSpace)
                .setFields(fields)
                .setPageToken(pageToken)
                .execute()
            result.addAll(fileList.files)
            pageToken = fileList.nextPageToken
        } while (pageToken != null)
        return@withContext result
    }
}

// List GLB files
private suspend fun listGlbFiles(
    context: Context,
    email: String,
    appName: String,
    glbMimeType: String,
    driveSpace: String,
    fields: String,
    callback: (Boolean, String, List<DriveFile>) -> Unit
) {
    withContext(Dispatchers.IO) {
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

            // List GLB files
            val files = getGlbFilesFromDrive(driveService, glbMimeType, driveSpace, fields)
            withContext(Dispatchers.Main) {
                if (files.isNotEmpty()) {
                    val fileList = files.joinToString("\n") { "• ${it.name}" }
                    callback(true, "Found ${files.size} GLB files:\n$fileList", files)
                } else {
                    callback(true, "No GLB files found in your Drive", emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error listing GLB files", e)
            withContext(Dispatchers.Main) {
                callback(false, "Failed to list files: ${e.message}", emptyList())
            }
        }
    }
}

// Custom OutputStream for tracking download progress
// Custom OutputStream for tracking download progress
class ProgressDownloadOutputStream(
    private val outputStream: OutputStream,
    private val fileSize: Long,
    private val onProgress: (Float) -> Unit
) : OutputStream() {
    private var bytesWritten: Long = 0
    private val handler = Handler(Looper.getMainLooper())

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
        val progress = bytesWritten.toFloat() / fileSize
        // Use handler to post to main thread instead of withContext
        handler.post {
            onProgress(if (progress > 1f) 1f else progress)
        }
    }
}

private suspend fun downloadGlbFile(
    context: Context,
    email: String,
    fileId: String,
    fileName: String,
    downloadLocation: Uri,
    glbMimeType: String,
    appName: String,
    storagePermissionLauncher: ActivityResultLauncher<Intent>,
    onProgress: (Float) -> Unit,
    callback: (Boolean, String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                callback(false, "Downloading $fileName...")
            }

            // Check for storage permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    withContext(Dispatchers.Main) {
                        requestStoragePermission(context, storagePermissionLauncher)
                        callback(false, "Storage permission required")
                    }
                    return@withContext
                }
            } else {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    withContext(Dispatchers.Main) {
                        ActivityCompat.requestPermissions(
                            context as Activity,
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ),
                            100
                        )
                        callback(false, "Storage permission required")
                    }
                    return@withContext
                }
            }

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

            // Get file size first for progress tracking
            val fileMetadata = driveService.files().get(fileId)
                .setFields("size")
                .execute()
            val fileSize = fileMetadata.getSize() ?: 0

            // Create output file
            val documentFile = DocumentFile.fromTreeUri(context, downloadLocation)
            val outputFile = documentFile?.createFile(glbMimeType, fileName)

            if (outputFile != null) {
                // Use a custom OutputStream wrapper to track progress
                try {
                    val originalStream = context.contentResolver.openOutputStream(outputFile.uri)
                    if (originalStream != null) {
                        // Use the updated ProgressDownloadOutputStream that uses Handler
                        val progressStream = ProgressDownloadOutputStream(originalStream, fileSize, onProgress)

                        // Download file from Drive with progress tracking
                        val request = driveService.files().get(fileId)
                        request.executeMediaAndDownloadTo(progressStream)

                        withContext(Dispatchers.Main) {
                            callback(true, "Successfully downloaded $fileName")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            callback(false, "Failed to open output stream")
                        }
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Download failed: ${e.message}")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(false, "Failed to create output file")
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error downloading GLB file", e)
            withContext(Dispatchers.Main) {
                callback(false, "Download failed: ${e.message}")
            }
        }
    }
}