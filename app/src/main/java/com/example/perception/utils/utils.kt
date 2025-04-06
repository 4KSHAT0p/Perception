package com.example.perception.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import android.util.Log
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale

object utils {
    // Default initial scale for newly placed models
    private const val DEFAULT_INITIAL_SCALE = 2f

    /**
     * Load model instances with optimized loading
     */
    fun loadModelInstancesSmart(
        context: Context,
        modelLoader: ModelLoader,
        model: String
    ): List<ModelInstance> {
        val startTime = System.currentTimeMillis()

        val instances = try {
            if (model.startsWith("/data/")) {
                val file = File(model)
                if (!file.exists()) {
                    throw FileNotFoundException("Model file not found at path: $model")
                }
                // Use direct ByteBuffer for better performance
                val bytes = file.readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size)
                buffer.put(bytes)
                buffer.flip()
                modelLoader.createInstancedModel(buffer, 1)
            } else {
                modelLoader.createInstancedModel(model, 1) // asset path
            }
        } catch (e: Exception) {
            Log.e("ModelLoader", "Error loading model $model: ${e.message}")
            emptyList()
        }

        val loadTime = System.currentTimeMillis() - startTime
        Log.d("ModelLoader", "Model $model loaded in $loadTime ms")

        return instances
    }

    /**
     * Create a movable node with the model
     * This uses a regular Node instead of AnchorNode for better movement
     */
    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        modelInstance: MutableList<ModelInstance>,
        anchor: Anchor,
        model: String,
        context: Context,
        minScale: Float = DEFAULT_INITIAL_SCALE,
        initialScale: Float = DEFAULT_INITIAL_SCALE
    ): Node {
        // Create a regular Node that will serve as our movable parent
        val parentNode = Node(engine)

        // Set initial position from anchor
        parentNode.worldPosition = Position(
            anchor.pose.tx(),
            anchor.pose.ty(),
            anchor.pose.tz()
        )

        // Set rotation from anchor
        parentNode.worldQuaternion = Quaternion(
            anchor.pose.qx(),
            anchor.pose.qy(),
            anchor.pose.qz(),
            anchor.pose.qw()
        )

        // Handle case when model instances list is empty
        if (modelInstance.isEmpty()) {
            modelInstance.addAll(loadModelInstancesSmart(context, modelLoader, model))
        }

        // Only proceed if we have model instances to work with
        if (modelInstance.isNotEmpty()) {
            val instance = modelInstance.removeAt(modelInstance.lastIndex)

            // Create model node directly with the instance
            val modelNode = ModelNode(
                modelInstance = instance
            ).apply {
                // Set initial scale
                scale = Scale(initialScale, initialScale, initialScale)

                // Enable all transformation options
                isEditable=true
                isTouchable=true
                isPositionEditable = true
                isRotationEditable = true
                isScaleEditable = false
            }

            // Add model as child of our movable parent
            parentNode.addChildNode(modelNode)
        }

        // Detach the anchor since we won't need it anymore
        // This is crucial for allowing the node to move freely
        anchor.detach()

        return parentNode
    }
}