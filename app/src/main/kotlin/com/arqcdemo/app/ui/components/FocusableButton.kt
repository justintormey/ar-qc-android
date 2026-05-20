package com.arqcdemo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arqcdemo.app.ui.theme.Accent
import com.arqcdemo.app.ui.theme.HudDim

/**
 * Compose button that glows cyan when its index matches the active
 * focused-index. The Argo's scroll-wheel moves focus between buttons in a
 * scene; clicking activates the focused one.
 *
 * @param label    The text to render.
 * @param focused  Whether this button currently holds the focus.
 * @param onClick  Activation callback (fires on wheel-click when focused, or
 *                 on regular touch).
 */
@Composable
fun FocusableButton(
    label: String,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        if (focused) Accent else HudDim.copy(alpha = 0.25f),
        label = "border",
    )
    val bgColor by animateColorAsState(
        if (focused) Accent.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.55f),
        label = "bg",
    )
    val textColor by animateColorAsState(
        if (focused) Accent else Color.White,
        label = "text",
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (focused) 6.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Accent,
                spotColor = Accent,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, borderColor), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}
