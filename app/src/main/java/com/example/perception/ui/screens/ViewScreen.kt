package com.example.perception.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import kotlin.random.Random
import com.example.perception.ui.navigation.ARScreen
import kotlinx.serialization.json.internal.encodeByWriter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun genRandomColor(): Color {
    val rand = Random(System.currentTimeMillis())
    val red = rand.nextInt(100, 200)
    val green = rand.nextInt(100, 200)
    val blue = rand.nextInt(100, 200)

    return Color(red, green, blue)
}

@Composable
fun ModelItem(modelName: String, modelPath: String, navController: NavController) {
    val backgroundColor = genRandomColor()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                // Navigate directly to AR screen with this model
                val encodedModel = URLEncoder.encode(modelPath, "UTF-8")
                val route = ARScreen.route.replace("{model}", encodedModel)
                navController.navigate(route)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = modelName.uppercase(),
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


@Composable
fun ViewScreen(navController: NavController) {
    val availableModels = listOf(
        "models/car.glb",
        "models/corner_sofa.glb",
        "models/two_seat_sofa.glb",
        "models/sofa (1).glb"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "Select a Model",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(availableModels) { model ->
                ModelItem(
                    modelName = model.substringAfterLast("/").substringBeforeLast("."),
                    modelPath = model,
                    navController = navController
                )
            }
        }
    }
}