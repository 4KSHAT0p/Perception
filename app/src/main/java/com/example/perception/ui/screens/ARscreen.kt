package com.example.perception.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import com.example.perception.utils.utils
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ARScreen(
    navController: NavController,
    modelList: List<String>,
    onBackPressed: () -> Unit = { navController.popBackStack() }
) {
    // Core AR components
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine = engine)
    val materialLoader = rememberMaterialLoader(engine = engine)
    val cameraNode = rememberARCameraNode(engine = engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine = engine)
    val collisionSystem = rememberCollisionSystem(view = view)

    // State variables
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val arViewRef = remember { mutableStateOf<View?>(null) }
    val selectedModel = remember { mutableStateOf(modelList.firstOrNull() ?: "") }
    val isModelListVisible = remember { mutableStateOf(false) }
    val selectedNode = remember { mutableStateOf<Node?>(null) }
    val scaleFactor = remember { mutableFloatStateOf(1.0f) }
    val planeRenderer = remember { mutableStateOf(true) }
    val trackingFailureReason = remember { mutableStateOf<TrackingFailureReason?>(null) }
    val frame = remember { mutableStateOf<Frame?>(null) }
    var captureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Clean up resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            childNodes.clear()
        }
    }

    BackHandler(onBack = onBackPressed)

    // Resize function
    val resizeSelectedNode = { increase: Boolean ->
        selectedNode.value?.let { node ->
            val scaleStep = 0.5f
            val minScale = 0.5f

            val newScale = if (increase) {
                scaleFactor.floatValue + scaleStep
            } else {
                (scaleFactor.floatValue - scaleStep).coerceAtLeast(minScale)
            }

            node.scale = Scale(newScale, newScale, newScale)
            scaleFactor.floatValue = newScale
        } ?: run {
            Toast.makeText(context, "Tap on a model to select it first", Toast.LENGTH_SHORT).show()
        }
    }

    // Screenshot handling
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    captureBitmap?.let { bitmap ->
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

    val captureScreen = {
        val currentView = arViewRef.value
        val activity = context as? android.app.Activity

        if (currentView != null && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val surfaceView = findARSurfaceView(currentView)
            val bitmap = createBitmap(currentView.width, currentView.height)
            val rect = Rect(0, 0, currentView.width, currentView.height)

            surfaceView?.let {
                PixelCopy.request(
                    it.holder.surface, rect, bitmap, { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            captureBitmap = bitmap
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                documentLauncher.launch("AR_Scene_${System.currentTimeMillis()}.jpg")
                            } else {
                                scope.launch {
                                    saveImageToGallery(context, bitmap)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "AR Scene captured!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to capture AR scene",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, Handler(Looper.getMainLooper())
                )
            }
        } else {
            Toast.makeText(
                context,
                "Screen capture not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {},
                    onDragCancel = {},
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        when {
                            dragAmount < -10 -> isModelListVisible.value = true
                            dragAmount > 10 -> isModelListVisible.value = false
                        }
                    }
                )
            }
    ) {
        // AR Scene
        AndroidView(
            factory = { ctx ->
                ComposeView(ctx).apply {
                    arViewRef.value = this
                    setContent {
                        ARScene(
                            modifier = Modifier.fillMaxSize(),
                            childNodes = childNodes,
                            engine = engine,
                            view = view,
                            modelLoader = modelLoader,
                            collisionSystem = collisionSystem,
                            planeRenderer = planeRenderer.value,
                            cameraNode = cameraNode,
                            materialLoader = materialLoader,
                            onTrackingFailureChanged = { trackingFailureReason.value = it },
                            onSessionUpdated = { _, updatedFrame -> frame.value = updatedFrame },
                            sessionConfiguration = { session, config ->
                                config.depthMode =
                                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                        Config.DepthMode.AUTOMATIC
                                    } else {
                                        Config.DepthMode.DISABLED
                                    }
                                config.lightEstimationMode =
                                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
                            },
                            onGestureListener = rememberOnGestureListener(
                                onSingleTapConfirmed = { e: MotionEvent, node: Node? ->
                                    if (node != null) {
                                        // Select existing model
                                        selectedNode.value = node
                                        scaleFactor.floatValue = node.scale.x
                                        Toast.makeText(
                                            context,
                                            "Model selected",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (selectedNode.value == null) {
                                        // Place new model
                                        val hitTestResult = frame.value?.hitTest(e.x, e.y)
                                        hitTestResult?.firstOrNull {
                                            it.isValid(depthPoint = false, point = false)
                                        }?.createAnchorOrNull()?.let { anchor ->
                                            try {
                                                val loadedModelInstances =
                                                    utils.loadModelInstancesSmart(
                                                        context = context,
                                                        modelLoader = modelLoader,
                                                        model = selectedModel.value
                                                    ).toMutableList()

                                                val anchorNode = utils.createAnchorNode(
                                                    engine = engine,
                                                    modelLoader = modelLoader,
                                                    materialLoader = materialLoader,
                                                    modelInstance = loadedModelInstances,
                                                    anchor = anchor,
                                                    model = selectedModel.value,
                                                    context = context
                                                )
                                                childNodes += anchorNode
                                                selectedNode.value = anchorNode
                                                scaleFactor.floatValue = anchorNode.scale.x
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "ARScreen",
                                                    "Error loading model: ${e.message}",
                                                    e
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Failed to load model",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        // Deselect
                                        selectedNode.value = null
                                    }
                                }
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Model movement control
        AnimatedVisibility(
            visible = selectedNode.value != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .padding(bottom = 150.dp)
                    .width(250.dp)
                    .height(150.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            selectedNode.value?.let { node ->
                                val moveFactor = 0.003f
                                val currentPos = node.worldPosition
                                node.worldPosition = Position(
                                    currentPos.x + (dragAmount.x * moveFactor),
                                    currentPos.y,
                                    currentPos.z - (dragAmount.y * moveFactor)
                                )
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0x66000000))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Drag here to move model",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        // Size down button

                        Button(
                            onClick = { resizeSelectedNode(false) },
                            modifier = Modifier
                                .size(50.dp),  // Increased from 50.dp to 60.dp
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)  // Remove default padding
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Size",
                                modifier = Modifier.size(24.dp),  // Increased from 24.dp to 40.dp
                                tint = Color.Black
                            )
                        }
                        // Delete button (unchanged)
                        Button(
                            onClick = {
                                selectedNode.value?.let { node ->
                                    childNodes.remove(node)
                                    node.destroy()
                                    Toast.makeText(context, "Model deleted", Toast.LENGTH_SHORT).show()
                                    selectedNode.value = null
                                } ?: run {
                                    Toast.makeText(context, "No model selected", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x99FF5555)),
                            modifier = Modifier
                                .height(50.dp)
                        ) {
                            Text("Delete Model")
                        }

                        Button(
                            onClick = { resizeSelectedNode(true) },
                            modifier = Modifier
                                .size(50.dp),  // Increased from 50.dp to 60.dp
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)  // Remove default padding
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Size",
                                modifier = Modifier.size(24.dp),  // Increased from 24.dp to 40.dp
                                tint = Color.Black
                            )
                        }
                        // Size up button
                    }
                }

            }
        }

        // Swipe hint indicator
        if (!isModelListVisible.value) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { isModelListVisible.value = true }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Swipe up for models",
                    tint = Color.White
                )
            }
        }

        // Model Selection Bar
        AnimatedVisibility(
            visible = isModelListVisible.value,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xAA000000))
                ) {
                    IconButton(
                        onClick = { isModelListVisible.value = false },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Hide models",
                            tint = Color.White
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xAA000000))
                ) {
                    items(modelList) { model ->
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (model == selectedModel.value) Color.Blue else Color.Gray)
                                .clickable { selectedModel.value = model }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = model.substringAfterLast("/").substringBeforeLast("."),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Resize buttons

        // Screenshot button
        FloatingActionButton(
            onClick = { captureScreen() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(75.dp)
                .border(3.dp, Color.Gray, CircleShape),
            containerColor = Color.White,
            shape = CircleShape
        ) {}
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

                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, fileName)
                FileOutputStream(image).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                MediaScannerConnection.scanFile(
                    context, arrayOf(image.toString()), arrayOf("image/jpeg"), null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun findARSurfaceView(view: View): SurfaceView? {
    if (view is SurfaceView) return view

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            findARSurfaceView(view.getChildAt(i))?.let { return it }
        }
    }

    return null
}