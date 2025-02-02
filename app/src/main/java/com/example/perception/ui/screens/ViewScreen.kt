package com.example.perception.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.perception.ui.navigation.ARScreen
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewScreen(navController: NavController) {
    val lisal = listOf("Hello", "matchwood", "gander")
    Column {
        Box(modifier = Modifier.height(60.dp)) {
            Text(text = "Alphabets", fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center
        ) {
            lisal.forEach { alphabet ->
                AlItem(alphabet = alphabet)
                {
                    navController.navigate(ARScreen(alphabet))
                }
            }
        } //flexbox type

    }

}

@Composable

fun AlItem(alphabet: String, onClick: () -> Unit) {
    val color = remember(alphabet) {
        genrandomcolor()
    }
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable { onClick() })
    {
        Text(text = alphabet, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
    }
}

fun genrandomcolor(): Color {
    val rand = Random(System.currentTimeMillis())
    val red = rand.nextInt(150, 256)
    val green = rand.nextInt(200, 256)
    val blue = rand.nextInt(200, 256)

    val color = Color(red, green, blue)
    return color
}

