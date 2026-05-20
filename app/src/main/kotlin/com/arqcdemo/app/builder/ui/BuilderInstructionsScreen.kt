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
import com.arqcdemo.app.ui.components.FocusableButton
import com.arqcdemo.app.ui.theme.HudDim

/**
 * Assembly instructions card. Shown after Welcome (begin) and after every
 * verdict — PASS routes here for the next part, FAIL routes here for the
 * same part (the rework loop).
 */
private val STEPS = listOf(
    "Pick up the sub-piece and 2 bolts.",
    "Align the sub-piece with the bolt pattern on the base.",
    "Orient it so the spec face points outward (the recessed corner = up).",
    "Install both bolts and finger-tighten.",
    "Hold the finished assembly up to the lens.",
)

@Composable
fun BuilderInstructionsScreen(partLabel: String, focusedIndex: Int = 0, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "ASSEMBLY — ${partLabel.uppercase()}",
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = "Steps",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            STEPS.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${i + 1}.",
                        color = HudDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                    )
                    Text(text = step, color = Color.White, fontSize = 16.sp)
                }
            }
            FocusableButton(
                label = "SCAN when ready",
                focused = focusedIndex == 0,
                onClick = {},
                modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}
