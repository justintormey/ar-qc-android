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
 * verdict — PASS routes here for the next step, FAIL routes here for the
 * same step (the rework loop).
 *
 * Each step has its own step list keyed by the partLabel (A/B/C from the
 * scene). The procedure assembles four 3D-printed angle brackets across
 * three steps: A+B (velcro, T-shape), then C onto B (string-tied), then D
 * (velcro, opposing C's flange).
 */
private data class StepCard(val header: String, val title: String, val steps: List<String>)

private val STEP_CARDS = mapOf(
    'A' to StepCard(
        header = "STEP 1 of 3 — ATTACH A + B",
        title = "Form the T",
        steps = listOf(
            "Pick up brackets A and B and the velcro tabs attached to each.",
            "Press A and B together face-to-face using the velcro tabs.",
            "Align the end channel flanges so they face AWAY from each other.",
            "The two pieces should form a T-shape.",
            "Hold the assembly up to the lens and scan.",
        ),
    ),
    'B' to StepCard(
        header = "STEP 2 of 3 — ATTACH C TO B",
        title = "Tie C onto B",
        steps = listOf(
            "Pick up bracket C and the supplied string.",
            "Tie C to bracket B using the string — secure but not over-tightened.",
            "Position C so its end channel flange points AWAY from the center of the assembly.",
            "Hold the assembly up to the lens and scan.",
        ),
    ),
    'C' to StepCard(
        header = "STEP 3 of 3 — ATTACH D",
        title = "Mount D opposing C",
        steps = listOf(
            "Pick up bracket D and use the velcro tabs already attached.",
            "Press D onto the assembly with its velcro tabs.",
            "Position D so its channel flange points AWAY from C's channel flange (opposing sides).",
            "Hold the assembly up so BOTH the B-face and C-face QRs are visible.",
            "Scan — pass requires BP and CP both in frame.",
        ),
    ),
)

@Composable
fun BuilderInstructionsScreen(partLabel: String, focusedIndex: Int = 0, modifier: Modifier = Modifier) {
    val key = partLabel.lastOrNull()?.uppercaseChar() ?: 'A'
    val card = STEP_CARDS[key] ?: STEP_CARDS.getValue('A')

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
                text = card.header,
                color = HudDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = card.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            card.steps.forEachIndexed { i, step ->
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
