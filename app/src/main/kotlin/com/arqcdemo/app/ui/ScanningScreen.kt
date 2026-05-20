package com.arqcdemo.app.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.arqcdemo.app.camera.CameraXController
import com.arqcdemo.app.camera.QrAnalyzer
import com.arqcdemo.app.ui.components.CornerBrackets
import com.arqcdemo.app.ui.components.ScanLine
import com.arqcdemo.app.ui.theme.HudDim

/**
 * Scanning state: live camera preview (CameraX) as the base layer with
 * corner brackets + cyan sweep line over the top. ML Kit QR analyzer
 * runs on every frame; emits 'A'/'B'/'C' → ViewModel via onQrDetected.
 */
@Composable
fun ScanningScreen(
    cameraGranted: Boolean,
    onQrDetected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analyzer = remember { QrAnalyzer(onDetected = onQrDetected) }
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
