package com.arqcdemo.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arqcdemo.app.ui.theme.Accent

/** Cyan sweep line that travels top → bottom and repeats. */
@Composable
fun ScanLine(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan-sweep")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        val y = h * progress
        val lineHeight = 3.dp.toPx()
        val hazeHeight = h * 0.30f

        // Fade in/out at the top and bottom of the sweep
        val alpha = when {
            progress < 0.10f -> progress / 0.10f
            progress > 0.90f -> (1f - progress) / 0.10f
            else -> 1f
        }

        // Trailing haze
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Accent.copy(alpha = 0.22f * alpha), Color.Transparent),
                startY = (y - hazeHeight).coerceAtLeast(0f),
                endY = y,
            ),
            topLeft = Offset(0f, (y - hazeHeight).coerceAtLeast(0f)),
            size = Size(w, (hazeHeight).coerceAtMost(y)),
        )

        // The bar itself
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Accent.copy(alpha = alpha),
                    Accent.copy(alpha = alpha),
                    Color.Transparent,
                ),
                startX = 0f,
                endX = w,
            ),
            topLeft = Offset(0f, y - lineHeight / 2f),
            size = Size(w, lineHeight),
        )
    }
}
