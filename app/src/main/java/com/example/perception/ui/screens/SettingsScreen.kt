package com.example.perception.ui.screens

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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.*

// DataStore extension function
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "glb_upload_user"
)

// Google Drive file holder class for uploads
class GoogleDriveFileHolder {
    var id: String = ""
    var name: String = ""
}

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
    val STORAGE_PERMISSION_CODE = 100
    val FIELDS = "nextPageToken, files(id, name, size, modifiedTime)"

    // Context and scope
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // States
    var statusMessage by remember { mutableStateOf("Please sign in to continue") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var selectedGlbFile by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var driveGlbFiles by remember { mutableStateOf<List<com.google.api.services.drive.model.File>>(emptyList()) }
    var showFileSelectionDialog by remember { mutableStateOf(false) }
    var downloadLocation by remember { mutableStateOf<Uri?>(null) }

    // Check login status on first load
    LaunchedEffect(Unit) {
        val email = getUserEmail(context)
        if (email != null) {
            statusMessage = "Signed in as: $email"
            isLoggedIn = true
        }
    }

    // File picker launcher
    val glbFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFleNameFromUri(context, it)
            if (fileName.endsWith(".glb", ignoreCase = true)) {
                selectedGlbFile = it
                selectedFileName = fileName
                statusMessage = "Selected: $fileName"
            } else {
                statusMessage = "Please select a valid GLB file (.glb)"
            }
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

    // Google Drive authorization launcher
    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val authorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data!!)
            statusMessage = "Drive permissions granted"
        } else {
            statusMessage = "Failed to grant Drive permissions"
        }
    }

    // Google sign-in
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle sign-in result if needed
    }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GLB File Uploader",
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

        // Request Drive permissions
        Button(
            onClick = {
                requestDrivePermissions(context, authorizationLauncher) { message ->
                    statusMessage = message
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
            Text("Request Drive Permissions")
        }

        // Select GLB file
        Button(
            onClick = { glbFilePicker.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            enabled = isLoggedIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text("Select GLB File")
        }

        // Upload GLB file
        Button(
            onClick = {
                if (selectedGlbFile == null) {
                    statusMessage = "Please select a GLB file first"
                    return@Button
                }

                coroutineScope.launch {
                    val email = getUserEmail(context)
                    if (email != null) {
                        selectedGlbFile?.let { uri ->
                            statusMessage = "Uploading $selectedFileName..."
                            uploadGlbFile(
                                context,
                                email,
                                uri,
                                selectedFileName,
                                GLB_MIME_TYPE,
                                GLB_FOLDER_NAME,
                                APP_NAME,
                                FOLDER_MIME_TYPE,
                                ROOT_FOLDER,
                                DRIVE_SPACE
                            ) { success, message ->
                                statusMessage = message
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
            Text("Upload GLB File")
        }

        // List GLB files
        Button(
            onClick = {
                coroutineScope.launch {
                    val email = getUserEmail(context)
                    if (email != null) {
                        statusMessage = "Fetching GLB files..."
                        listGlbFiles(
                            context,
                            email,
                            APP_NAME,
                            GLB_MIME_TYPE,
                            DRIVE_SPACE,
                            FIELDS
                        ) { success, message, files ->
                            statusMessage = message
                            if (success) {
                                driveGlbFiles = files
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
            Text("List GLB Files")
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
                    selectedGlbFile = null
                    selectedFileName = ""
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

        Spacer(modifier = Modifier.height(16.dp))

        // Theme switch section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
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
                                            storagePermissionLauncher
                                        ) { success, message ->
                                            statusMessage = message
                                        }
                                    }
                                }
                                showFileSelectionDialog = false
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
}

// Helper functions

// Get file name from URI
private fun getFleNameFromUri(context: Context, uri: Uri): String {
    var fileName = "unknown_file.glb"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}

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

// Request Drive permissions
private fun requestDrivePermissions(
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    callback: (String) -> Unit
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
            }
        }
        .addOnFailureListener { e ->
            callback("Failed to request permissions: ${e.message}")
        }
}

// Create temporary file from URI
private fun createTempFileFromUri(
    context: Context,
    uri: Uri,
    fileName: String
): File? {
    try {
        val tempDir = File(context.cacheDir, "glb_uploads")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val tempFile = File(tempDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Error creating temp file", e)
        return null
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
            val metadata = com.google.api.services.drive.model.File()
                .setParents(listOf(rootFolder))
                .setMimeType(folderMimeType)
                .setName(folderName)
            val folder = driveService.files().create(metadata).execute()
            val fileHolder = GoogleDriveFileHolder()
            fileHolder.id = folder.id
            fileHolder.name = folder.name
            return@withContext fileHolder
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error creating/getting folder", e)
            return@withContext null
        }
    }
}

// Upload file to Drive
private suspend fun uploadFileToDrive(
    driveService: Drive,
    file: File,
    mimeType: String,
    folderId: String?,
    rootFolder: String
): GoogleDriveFileHolder {
    return withContext(Dispatchers.IO) {
        val parents = folderId?.let { listOf(it) } ?: listOf(rootFolder)
        val metadata = com.google.api.services.drive.model.File()
            .setParents(parents)
            .setMimeType(mimeType)
            .setName(file.name)
        val fileContent = com.google.api.client.http.FileContent(mimeType, file)
        val uploadedFile = driveService.files().create(metadata, fileContent)
            .setFields("id, name, size, modifiedTime")
            .execute()
        val result = GoogleDriveFileHolder()
        result.id = uploadedFile.id
        result.name = uploadedFile.name
        return@withContext result
    }
}

// Upload GLB file
private suspend fun uploadGlbFile(
    context: Context,
    email: String,
    uri: Uri,
    fileName: String,
    glbMimeType: String,
    glbFolderName: String,
    appName: String,
    folderMimeType: String,
    rootFolder: String,
    driveSpace: String,
    callback: (Boolean, String) -> Unit
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

            // Create GLB models folder
            val folderJob = async {
                createOrGetFolder(driveService, glbFolderName, folderMimeType, rootFolder, driveSpace)
            }
            val folder = folderJob.await()

            // Create temp file from URI
            val tempFile = createTempFileFromUri(context, uri, fileName)
            if (tempFile != null) {
                // Upload file to Drive
                val uploadResult = withContext(Dispatchers.IO) {
                    uploadFileToDrive(driveService, tempFile, glbMimeType, folder?.id, rootFolder)
                }
                // Clean up temp file
                tempFile.delete()
                withContext(Dispatchers.Main) {
                    callback(true, "Successfully uploaded: ${uploadResult.name}")
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(false, "Failed to process file")
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error uploading GLB file", e)
            withContext(Dispatchers.Main) {
                callback(false, "Upload failed: ${e.message}")
            }
        }
    }
}

// Get GLB files from Drive
private suspend fun getGlbFilesFromDrive(
    driveService: Drive,
    glbMimeType: String,
    driveSpace: String,
    fields: String
): List<com.google.api.services.drive.model.File> {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<com.google.api.services.drive.model.File>()
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
    callback: (Boolean, String, List<com.google.api.services.drive.model.File>) -> Unit
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

private suspend fun downloadGlbFile(
    context: Context,
    email: String,
    fileId: String,
    fileName: String,
    downloadLocation: Uri,
    glbMimeType: String,
    appName: String,
    storagePermissionLauncher: ActivityResultLauncher<Intent>,
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

            // Create output file
            val documentFile = DocumentFile.fromTreeUri(context, downloadLocation)
            val outputFile = documentFile?.createFile(glbMimeType, fileName)

            if (outputFile != null) {
                context.contentResolver.openOutputStream(outputFile.uri)?.use { outputStream ->
                    // Download file from Drive
                    driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream)
                }

                withContext(Dispatchers.Main) {
                    callback(true, "Successfully downloaded $fileName")
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