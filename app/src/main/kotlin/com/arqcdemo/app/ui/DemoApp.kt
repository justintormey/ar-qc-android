package com.arqcdemo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arqcdemo.app.DemoViewModel
import com.arqcdemo.app.Scene
import com.arqcdemo.app.ui.components.ConnectionPill
import com.arqcdemo.app.ui.components.StatusChip

@Composable
fun DemoApp(
    viewModel: DemoViewModel,
    cameraGranted: Boolean,
    onPinChange: (String) -> Unit,
) {
    val state by viewModel.ui.collectAsState()

    if (state.roomPin.isBlank()) {
        SetupScreen(onSave = onPinChange)
        return
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        when (state.scene) {
            Scene.Welcome -> WelcomeScreen()
            Scene.Scanning -> ScanningScreen(
                cameraGranted = cameraGranted,
                onQrDetected = viewModel::onQrDetected,
            )
            Scene.VerdictA -> VerdictScreen(part = "A")
            Scene.VerdictB -> VerdictScreen(part = "B")
            Scene.VerdictC -> VerdictScreen(part = "C")
            Scene.Complete -> CompleteScreen(counts = state.counts, elapsedMs = state.elapsedMs)
        }

        StatusChip(
            text = state.scene.statusText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        )
        ConnectionPill(
            state = state.transport,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        )
    }
}
