package com.arqcdemo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.arqcdemo.app.ui.theme.HudDim

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "QC STATION 3",
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
            )
            Text(
                text = "Job 4471 — 16-ga bracket inspection",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "Three parts in front of you.", color = Color.White, fontSize = 18.sp)
            Text(text = "Three verdicts: pass, rework, scrap.", color = Color.White, fontSize = 18.sp)
            Text(
                text = "The glasses do the analysis. You make the call.",
                color = HudDim,
                fontSize = 16.sp,
            )
            Text(
                text = "AWAITING MODERATOR",
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
