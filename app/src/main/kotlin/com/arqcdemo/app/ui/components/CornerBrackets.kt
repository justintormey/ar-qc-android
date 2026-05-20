package com.arqcdemo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arqcdemo.app.ui.theme.Accent

/** Four cyan corner brackets, matching the HUD scanner look. */
@Composable
fun CornerBrackets(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val padding = 16.dp.toPx()
        val arm = 56.dp.toPx()
        val thickness = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val c = Accent

        // top-left
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(arm, thickness))
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
            size = androidx.compose.ui.geometry.Size(thickness, arm))
        // top-right
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(w - padding - arm, padding),
            size = androidx.compose.ui.geometry.Size(arm, thickness))
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(w - padding - thickness, padding),
            size = androidx.compose.ui.geometry.Size(thickness, arm))
        // bottom-left
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(padding, h - padding - thickness),
            size = androidx.compose.ui.geometry.Size(arm, thickness))
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(padding, h - padding - arm),
            size = androidx.compose.ui.geometry.Size(thickness, arm))
        // bottom-right
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(w - padding - arm, h - padding - thickness),
            size = androidx.compose.ui.geometry.Size(arm, thickness))
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(w - padding - thickness, h - padding - arm),
            size = androidx.compose.ui.geometry.Size(thickness, arm))
    }
}
