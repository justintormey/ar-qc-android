package com.arqcdemo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arqcdemo.app.transport.TransportState
import com.arqcdemo.app.ui.theme.Accent
import com.arqcdemo.app.ui.theme.HudDim
import com.arqcdemo.app.ui.theme.Pass
import com.arqcdemo.app.ui.theme.Rework
import com.arqcdemo.app.ui.theme.Scrap

@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            color = HudDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
fun ConnectionPill(state: TransportState, modifier: Modifier = Modifier) {
    val (dotColor, label) = when (state) {
        TransportState.Connected -> Pass to "connected"
        is TransportState.Peers -> Rework to state.text
        TransportState.Initiating -> Rework to "initiating"
        TransportState.PeerConnecting -> Rework to "negotiating"
        TransportState.SignalingOpen -> Rework to "waiting for headset"
        TransportState.Connecting -> Rework to "connecting"
        TransportState.Initializing -> Rework to "init"
        TransportState.PeerLeft -> Scrap to "peer left"
        is TransportState.Error -> Scrap to state.text
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, HudDim.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape),
            content = {},
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = label,
            color = HudDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
