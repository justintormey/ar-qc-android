package com.arqcdemo.app.builder.ui

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
import com.arqcdemo.app.ui.components.FocusableButton
import com.arqcdemo.app.ui.theme.HudDim
import com.arqcdemo.app.ui.theme.Pass

private data class PassSpec(val stepLabel: String, val checks: List<Pair<String, String>>, val cue: String)

private val PASS_SPECS = mapOf(
    'A' to PassSpec(
        stepLabel = "STEP 1 — A + B",
        checks = listOf(
            "Velcro" to "both tabs engaged",
            "Flanges" to "aligned, facing outward",
            "Shape" to "T-form verified",
        ),
        cue = "Step 1 complete — assembly to spec.",
    ),
    'B' to PassSpec(
        stepLabel = "STEP 2 — C ONTO B",
        checks = listOf(
            "String tie" to "secure",
            "Position" to "C joined to B",
            "Flange" to "away from center",
        ),
        cue = "Step 2 complete — assembly to spec.",
    ),
    'C' to PassSpec(
        stepLabel = "STEP 3 — D",
        checks = listOf(
            "Velcro" to "tabs engaged",
            "Flange" to "opposite C's flange",
            "QR check" to "BP + CP both visible",
        ),
        cue = "Step 3 complete — full assembly to spec.",
    ),
)

@Composable
fun BuilderVerdictPassScreen(part: String, focusedIndex: Int = 0, modifier: Modifier = Modifier) {
    val key = part.lastOrNull()?.uppercaseChar() ?: 'A'
    val spec = PASS_SPECS[key] ?: PASS_SPECS.getValue('A')
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
                    text = "${spec.stepLabel} — JOB 526526",
                    color = HudDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                spec.checks.forEach { (k, v) ->
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
                    Text(text = "✓", color = Pass, fontSize = 32.sp)
                    Text(
                        text = "VERDICT: PASS",
                        color = Pass,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Pass, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = spec.cue,
                    color = Pass,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FocusableButton(
                    label = if (key == 'C') "Finish" else "Next step",
                    focused = focusedIndex == 0,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                FocusableButton(
                    label = "End session",
                    focused = focusedIndex == 1,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
