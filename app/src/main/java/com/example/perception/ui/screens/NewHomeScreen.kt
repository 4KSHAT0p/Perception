package com.example.perception.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.perception.R
import com.example.perception.utils.ModelRepository
import kotlinx.coroutines.delay
import com.google.gson.Gson
//
@Composable
fun NewHomeScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize the model repository
    LaunchedEffect(Unit) {
        ModelRepository.initialize(context)
    }

    val images = listOf(
        R.drawable.home_bg,
        R.drawable.home_bg2,
        R.drawable.home_bg3
    )
    // Initialize with initialPage = 0 and pageCount from the list size
    val pagerState = rememberPagerState(initialPage = 0) { images.size }
    // Auto-scroll effect
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(3000) // Wait for 3 seconds
            // Safe calculation of next page with modulo
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Pager with fixed height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Image(
                    painter = painterResource(id = images[page]),
                    contentDescription = "Slide ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dot Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(images.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) Color.Black else Color.Gray,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        // Intro Text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Design your dream room..\n")
                    }
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary)) {
                        append("with just a tap.")
                    }
                },
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = buildAnnotatedString {
                    append("With ")
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                        append("Perception")
                    }
                    append(", you can visualize and arrange any object in your room before making it real.\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append("\nIt's not just furniture — it's your imagination, redefined.")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        // Get Started Button
        Button(
            onClick = {
                // Get models from the repository
                val modelList = ModelRepository.availableModels.toList()
                val encodedModelList = Uri.encode(Gson().toJson(modelList))
                navController.navigate("ar/$encodedModelList")
            },
            modifier = Modifier
                .padding(vertical = 16.dp)
                .height(48.dp)
                .widthIn(min = 200.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.labelLarge
            )
            Image(
                painter = painterResource(id = R.drawable.perception_logo), // Replace with actual name
                contentDescription = "Perception Logo",
                modifier = Modifier
                    .height(60.dp)
                    .padding(8.dp)
            )
        }
    }
}