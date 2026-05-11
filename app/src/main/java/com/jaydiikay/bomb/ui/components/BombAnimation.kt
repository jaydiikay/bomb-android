package com.jaydiikay.bomb.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

@Composable
fun BombAnimation(onAnimationEnd: () -> Unit) {
    var triggered by remember { mutableStateOf(false) }

    // Ring expansion animation
    val ringRadius1 by animateFloatAsState(
        targetValue = if (triggered) 600f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "ring1"
    )
    val ringRadius2 by animateFloatAsState(
        targetValue = if (triggered) 450f else 0f,
        animationSpec = tween(durationMillis = 2000, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "ring2"
    )
    val ringRadius3 by animateFloatAsState(
        targetValue = if (triggered) 300f else 0f,
        animationSpec = tween(durationMillis = 2000, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "ring3"
    )

    // Alpha fading for rings
    val ringAlpha1 by animateFloatAsState(
        targetValue = if (triggered) 0f else 0.9f,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "alpha1"
    )
    val ringAlpha2 by animateFloatAsState(
        targetValue = if (triggered) 0f else 0.7f,
        animationSpec = tween(durationMillis = 2000, delayMillis = 200, easing = LinearEasing),
        label = "alpha2"
    )

    // Shake offset
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 100
                -10f at 0
                10f at 25
                -10f at 50
                10f at 75
                0f at 100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shakeX"
    )

    // Pulsing text
    val textScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textScale"
    )

    LaunchedEffect(Unit) {
        triggered = true
        delay(2500)
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .drawBehind {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Outer ring
                drawCircle(
                    color = Color(0xFFFF6D00).copy(alpha = ringAlpha1),
                    radius = ringRadius1,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                )
                // Middle ring
                drawCircle(
                    color = Color(0xFFFFB300).copy(alpha = ringAlpha2),
                    radius = ringRadius2,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
                )
                // Inner ring / filled flash
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = (ringAlpha1 * 0.5f).coerceIn(0f, 1f)),
                    radius = ringRadius3,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset(x = shakeX.dp)
        ) {
            Text(
                text = "💣",
                fontSize = (80 * textScale).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BOMB!",
                fontSize = (48 * textScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF6D00),
                textAlign = TextAlign.Center,
                letterSpacing = 6.sp
            )
            Text(
                text = "💣",
                fontSize = (80 * textScale).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
