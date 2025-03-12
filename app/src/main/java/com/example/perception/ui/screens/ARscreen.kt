package com.example.perception.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.perception.R
import com.example.perception.utils.utils
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


@Composable
fun ARScreen(navController: NavController, model: String) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine = engine)
    val materialLoader = rememberMaterialLoader(engine = engine)
    val cameraNode = rememberARCameraNode(engine = engine)
    val childnodes = rememberNodes()
    val view = rememberView(engine = engine)
    val collisionsystem = rememberCollisionSystem(view = view)
    val planeRenderer = remember { mutableStateOf(true) }
    val modelInstance = remember { mutableListOf<ModelInstance>() }
    val trackingFailureReason = remember { mutableStateOf<TrackingFailureReason?>(null) }
    val frame = remember { mutableStateOf<Frame?>(null) }

    val context= LocalContext.current
    val scope= rememberCoroutineScope()
    val arViewRef = remember { mutableStateOf<View?>(null) }

    var captureBitmap: Bitmap? = null

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    // Use the stored bitmap from PixelCopy
                    val bitmap = captureBitmap
                    if (bitmap != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "AR Scene saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Update your capture function to use PixelCopy
    val captureScreen = {
        val currentView = arViewRef.value
        val activity = context as? android.app.Activity

        if (currentView != null && activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Find the actual AR surface view within the view hierarchy
                val surfaceView = findARSurfaceView(currentView)

                // Use PixelCopy for Android 8.0+
                val bitmap = Bitmap.createBitmap(
                    currentView.width,
                    currentView.height,
                    Bitmap.Config.ARGB_8888
                )

                val rect = Rect(0, 0, currentView.width, currentView.height)
                val handler = Handler(Looper.getMainLooper())



                if (surfaceView != null) {
                    PixelCopy.request(
                        surfaceView.holder.surface,
                        rect,
                        bitmap,
                        { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                // Store the bitmap
                                captureBitmap = bitmap

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    // Use document launcher for Android 14+
                                    documentLauncher.launch("AR_Scene_${System.currentTimeMillis()}.jpg")
                                } else {
                                    // Save directly for Android 8.0-13
                                    scope.launch {
                                        saveImageToGallery(context, bitmap)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "AR Scene captured!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Failed to capture AR scene", Toast.LENGTH_SHORT).show()
                            }
                        },
                        handler
                    )
                }
            } else {
                // Fallback for Android 7.1 and below
                Toast.makeText(context, "Screen capture not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        // This AndroidView captures a reference to the ComposeView containing the AR scene
        AndroidView(
            factory = { ctx ->
                ComposeView(ctx).apply {
                    arViewRef.value = this
                    setContent {
                        // Your AR scene content
                        ARScene(
                            modifier = Modifier.fillMaxSize(),
                            childNodes = childnodes,
                            engine = engine,
                            view = view,
                            modelLoader = modelLoader,
                            collisionSystem = collisionsystem,
                            planeRenderer = planeRenderer.value,
                            cameraNode = cameraNode,
                            materialLoader = materialLoader,
                            onTrackingFailureChanged = {
                                trackingFailureReason.value = it
                            },
                            onSessionUpdated = { _, updatedFrame ->
                                frame.value = updatedFrame
                            },
                            sessionConfiguration = { session, config ->
                                config.depthMode =
                                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                        true -> Config.DepthMode.AUTOMATIC
                                        else -> Config.DepthMode.DISABLED
                                    }
                                config.lightEstimationMode =
                                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
                            },
                            onGestureListener = rememberOnGestureListener(
                                onSingleTapConfirmed = { e: MotionEvent, node: Node? ->
                                    if (node == null) {
                                        val hitTestResult = frame.value?.hitTest(e.x, e.y)
                                        hitTestResult?.firstOrNull {
                                            it.isValid(depthPoint = false, point = false)
                                        }?.createAnchorOrNull()?.let {
                                            val nodemodel = utils.createAnchorNode(
                                                engine = engine,
                                                modelLoader = modelLoader,
                                                materialLoader = materialLoader,
                                                modelInstance = modelInstance,
                                                anchor = it,
                                                model = model
                                            )
                                            childnodes += nodemodel
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        FloatingActionButton(
            onClick = { captureScreen() },  // Call the capture function when clicked
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            contentColor = Color.White,
            containerColor = Color.Transparent
        ) {
            Image(
                painter = painterResource(id = R.drawable.camera),
                contentDescription = "Capture",
                modifier = Modifier.size(75.dp).padding(10.dp)
            )
        }
    }
}


private suspend fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            val fileName = "AR_Scene_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val image = File(imagesDir, fileName)
                FileOutputStream(image).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(image.toString()),
                    arrayOf("image/jpeg"),
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Function to find the SurfaceView in the view hierarchy
private fun findARSurfaceView(view: View): SurfaceView? {
    // If this view is a SurfaceView, return it
    if (view is SurfaceView) {
        return view
    }

    // If this view is a ViewGroup, search its children
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val result = findARSurfaceView(view.getChildAt(i))
            if (result != null) {
                return result
            }
        }
    }

    // Not found
    return null
}


