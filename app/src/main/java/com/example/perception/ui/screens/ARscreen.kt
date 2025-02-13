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
import java.nio.file.WatchEvent


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



    Box(modifier = Modifier.fillMaxSize())
    {
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
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
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
        FloatingActionButton(
            onClick = {},
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
