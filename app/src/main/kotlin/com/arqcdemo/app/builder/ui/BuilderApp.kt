package com.arqcdemo.app.builder.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.arqcdemo.app.builder.BuilderScene
import com.arqcdemo.app.builder.BuilderViewModel
import com.arqcdemo.app.camera.CameraXController
import com.arqcdemo.app.camera.QrAnalyzer
import com.arqcdemo.app.ui.SetupScreen
import com.arqcdemo.app.ui.components.ConnectionPill
import com.arqcdemo.app.ui.components.CornerBrackets
import com.arqcdemo.app.ui.components.ScanLine
import com.arqcdemo.app.ui.components.StatusChip
import com.arqcdemo.app.ui.theme.HudDim

@Composable
fun BuilderApp(
    viewModel: BuilderViewModel,
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
        when (val s = state.scene) {
            BuilderScene.Welcome -> BuilderWelcomeScreen(focusedIndex = state.focusedIndex)
            is BuilderScene.Instructions -> BuilderInstructionsScreen(
                partLabel = "Part ${s.part}",
                focusedIndex = state.focusedIndex,
            )
            BuilderScene.Scanning -> BuilderScanningScreen(
                cameraGranted = cameraGranted,
                onBuilderQrDetected = viewModel::onBuilderQrDetected,
            )
            is BuilderScene.VerdictPass -> BuilderVerdictPassScreen(
                part = s.part.toString(),
                focusedIndex = state.focusedIndex,
            )
            is BuilderScene.VerdictFail -> BuilderVerdictFailScreen(
                part = s.part.toString(),
                focusedIndex = state.focusedIndex,
            )
            BuilderScene.Complete -> BuilderCompleteScreen(
                counts = state.counts,
                elapsedMs = state.elapsedMs,
                focusedIndex = state.focusedIndex,
            )
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

/**
 * Scanning screen that wires CameraX + ML Kit's QrAnalyzer for the Builder
 * QR vocabulary (AP/AF/BP/BF/CP/CF). Identical visual to the QC scanning
 * screen — corner brackets + sweep line + live preview.
 */
@Composable
fun BuilderScanningScreen(
    cameraGranted: Boolean,
    onBuilderQrDetected: (part: Char, isPass: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analyzer = remember { QrAnalyzer(onBuilder = onBuilderQrDetected) }
    val controller = remember { CameraXController(context) }

    DisposableEffect(Unit) {
        onDispose {
            controller.unbind()
            analyzer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (cameraGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    controller.bind(lifecycleOwner, previewView, analyzer)
                },
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Camera permission not granted — grant it from Android Settings",
                    color = HudDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        CornerBrackets(modifier = Modifier.fillMaxSize())
        ScanLine(modifier = Modifier.fillMaxSize())
    }
}
