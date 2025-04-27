package com.example.perception

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Animation for progress
    val progressAnimation = remember { Animatable(0f) }

    LaunchedEffect(true) {
        // Animate the progress bar
        progressAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2500,
                easing = LinearEasing
            )
        )
        // Add a small delay at the end
        delay(500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo image
            Image(
                painter = painterResource(id = R.drawable.logoimage), // Make sure your image is in drawable
                contentDescription = "Perception Logo",
                modifier = Modifier
                    .width(180.dp)
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )

            // Add spacing between logo and tagline
            Spacer(modifier = Modifier.height(16.dp))

            // Tagline text
            Text(
                text = "Design Reality, Reimagined", // Choose your preferred tagline
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }

        // Bottom loading bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progressAnimation.value },
                modifier = Modifier
                    .width(250.dp)
                    .height(4.dp),
                color = Color.Red,
                trackColor = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}