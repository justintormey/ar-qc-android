package com.arqcdemo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.arqcdemo.app.ui.theme.Pass
import com.arqcdemo.app.ui.theme.Rework
import com.arqcdemo.app.ui.theme.Scrap

private data class VerdictSpec(
    val title: String,
    val lines: List<Pair<String, String>>,
    val verdictText: String,
    val symbol: String,
    val color: Color,
    val cue: String,
)

@Composable
fun VerdictScreen(part: String, focusedIndex: Int = 0, modifier: Modifier = Modifier) {
    val spec = when (part.uppercase()) {
        "A" -> VerdictSpec(
            title = "PART A — JOB 471471",
            lines = listOf(
                "Surface finish" to "smooth, no defects",
                "Vent openings" to "9 / 9 present",
                "Warp" to "0.04 mm (in tolerance)",
            ),
            verdictText = "PASS",
            symbol = "✓",
            color = Pass,
            cue = "Place the part in the PASS zone",
        )
        "B" -> VerdictSpec(
            title = "PART B — JOB 471471",
            lines = listOf(
                "Vent openings" to "0 / 6 detected",
                "Warp" to "1.8 mm at base",
                "Surface finish" to "acceptable",
                "Layer adhesion" to "within tolerance",
            ),
            verdictText = "REWORK",
            symbol = "⚠",
            color = Rework,
            cue = "Place the part in the REWORK zone",
        )
        else -> VerdictSpec(
            title = "PART C — JOB 471471",
            lines = listOf(
                "Surface finish" to "extruder blobs, stringing",
                "Layer adhesion" to "failed",
                "Vent openings" to "0 / 6 detected",
                "Defects" to "12+ flagged",
            ),
            verdictText = "SCRAP",
            symbol = "✕",
            color = Scrap,
            cue = "Place the part in the SCRAP zone",
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
            ) {
                Text(
                    text = spec.title,
                    color = HudDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                for ((k, v) in spec.lines) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = k, color = Color.White, fontSize = 16.sp)
                        Text(text = v, color = HudDim, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = spec.symbol, color = spec.color, fontSize = 32.sp)
                    Text(
                        text = "VERDICT: ${spec.verdictText}",
                        color = spec.color,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, spec.color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = spec.cue, color = spec.color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            // Wearer's scroll-wheel + click drives one of three actions.
            // Each FocusableButton glows cyan when focusedIndex points at it.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.arqcdemo.app.ui.components.FocusableButton(
                    label = "Accept",
                    focused = focusedIndex == 0,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                com.arqcdemo.app.ui.components.FocusableButton(
                    label = "Reject",
                    focused = focusedIndex == 1,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                com.arqcdemo.app.ui.components.FocusableButton(
                    label = "End session",
                    focused = focusedIndex == 2,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
