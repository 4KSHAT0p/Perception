package com.example.perception.ui.screens


import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.perception.R
import kotlinx.coroutines.delay
import com.google.gson.Gson

@Composable
fun NewHomeScreen(navController: NavController) {
    val images = listOf(
        R.drawable.home_bg,
        R.drawable.home_bg2,
        R.drawable.home_bg3
    )

    val pagerState = rememberPagerState(pageCount = { images.size })

    // Track if this screen is active
    var isActive by remember { mutableStateOf(true) }

    // Set isActive to false when leaving this composable
    DisposableEffect(Unit) {
        onDispose {
            isActive = false
        }
    }

    LaunchedEffect(pagerState.currentPage, isActive) {
        while (isActive) {
            delay(3000) // Wait for 3 seconds
            if (isActive) { // Check again after delay
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Pager
        HorizontalPager(state = pagerState) { page ->
            Image(
                painter = painterResource(id = images[page]),
                contentDescription = "Slide ${page + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dot Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            images.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .background(
                            color = if (pagerState.currentPage == index) Color.Black else Color.Gray,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        // Intro Text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Design your dream room..\n")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
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
                    withStyle(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Perception")
                    }
                    append(", you can visualize and arrange any object in your room before making it real.\n")
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append("\nIt's not just furniture — it's your imagination, redefined.")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }


        // Get Started Button
//        Button(
//            onClick = {},
//            modifier = Modifier.padding(top = 16.dp)
//        ) {
//            Text(text = "Get Started")
//        }
        Button(
            onClick = {
                val allModels = listOf(
                    "models/Lamborghini.glb",
                    "models/Sofa.glb",
                    "models/Pot.glb",
                    "models/CornerSofa.glb",
                    "models/TwoSeatSofa.glb",

                    )
                val encodedModelList = Uri.encode(Gson().toJson(allModels))
                navController.navigate("ar/$encodedModelList")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Get Started")
        }


    }
}