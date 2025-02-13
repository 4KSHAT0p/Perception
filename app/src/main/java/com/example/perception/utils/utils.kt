package com.example.perception.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.filament.View
import com.google.android.filament.Renderer
import com.google.android.filament.Texture

import java.nio.ByteBuffer


object utils {
    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        modelInstance: MutableList<ModelInstance>,
        materialLoader: MaterialLoader,
        anchor: Anchor,
        model: String
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelInstance.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(model, 1)
                }
            }.removeAt(modelInstance.lastIndex),
            scaleToUnits = 0.2f
        ).apply { isEditable = true }
        val boundingBox = CubeNode(
            engine = engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White)
        ).apply { isVisible = false }
        modelNode.addChildNode(boundingBox)
        anchorNode.addChildNode(modelNode)
        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                boundingBox.isVisible = editingTransforms.isNotEmpty()
            }
        }
        return anchorNode
    }

    fun capture(filamentView: View, renderer: Renderer, context: Context) {
        val width = filamentView.viewport.width
        val height = filamentView.viewport.height

        // Create a bitmap to store the screenshot
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create a buffer to store the pixels
        val buffer = ByteBuffer.allocateDirect(width * height * 4)

        try {
            // Render the frame into the buffer
            val viewport = filamentView.viewport
            renderer.readPixels(renderer,0,width,height,buffer)

            // Copy the buffer to the bitmap
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)

            // Save the bitmap
            Handler(Looper.getMainLooper()).post {
                saveBitmapToGallery(bitmap, context)
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to capture view: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, context: Context) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "Filament_Scene_$timestamp.jpg"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (Q) and above uses MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    outputStream = context.contentResolver.openOutputStream(it)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                    Toast.makeText(context, "Screenshot saved to gallery", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Below Android 10
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, filename)
                outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream as FileOutputStream)

                // Make the image appear in the gallery
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
                Toast.makeText(context, "Screenshot saved to gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
        } finally {
            outputStream?.close()
        }
    }

}