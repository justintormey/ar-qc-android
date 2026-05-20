package com.arqcdemo.app.builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arqcdemo.app.builder.BuilderCounts
import com.arqcdemo.app.ui.components.FocusableButton
import com.arqcdemo.app.ui.theme.HudDim
import com.arqcdemo.app.ui.theme.Pass
import com.arqcdemo.app.ui.theme.Scrap

@Composable
fun BuilderCompleteScreen(
    counts: BuilderCounts,
    elapsedMs: Long,
    focusedIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    val m = (elapsedMs / 60_000L).toString()
    val s = ((elapsedMs % 60_000L) / 1000L).toString().padStart(2, '0')
    val attempts = counts.attempts

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SESSION COMPLETE",
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 3.sp,
            )
            Text(
                text = "Job 526526 — ${counts.pass} of 3 steps passed",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Stat(modifier = Modifier.weight(1f), label = "Passed",   value = counts.pass.toString(), color = Pass)
                Stat(modifier = Modifier.weight(1f), label = "Failures", value = counts.fail.toString(), color = Scrap)
                Stat(modifier = Modifier.weight(1f), label = "Attempts", value = attempts.toString(),   color = HudDim)
            }
            Text(
                text = "Total time   $m:$s",
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
            )
            FocusableButton(
                label = "Reset",
                focused = focusedIndex == 0,
                onClick = {},
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Stat(modifier: Modifier, label: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            color = HudDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(text = value, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}
