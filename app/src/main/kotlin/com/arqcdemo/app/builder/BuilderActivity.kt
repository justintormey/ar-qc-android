package com.arqcdemo.app.builder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arqcdemo.app.builder.ui.BuilderApp
import com.arqcdemo.app.input.WheelInput
import com.arqcdemo.app.settings.Prefs
import com.arqcdemo.app.ui.theme.ArQcDemoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Second launcher Activity in the same APK. Mirrors MainActivity but hosts
 * the BuilderViewModel + BuilderApp Compose tree.
 *
 * Uses a different DataStore key for its room PIN (see Prefs.BUILDER_PIN_KEY)
 * so QC and Builder can have independent default PINs.
 */
class BuilderActivity : ComponentActivity() {

    private val viewModel: BuilderViewModel by viewModels()
    private val cameraPermissionGranted = mutableStateOf(false)

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted.value = granted
        Log.i(TAG, "camera permission granted=$granted")
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (WheelInput.dispatch(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraPermissionGranted.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!cameraPermissionGranted.value) {
            requestCamera.launch(Manifest.permission.CAMERA)
        }

        lifecycleScope.launch {
            val prefs = Prefs(applicationContext)
            var pin = prefs.builderRoomPin.first()
            if (pin.isBlank()) {
                prefs.setBuilderRoomPin(DEFAULT_BUILDER_PIN)
                pin = DEFAULT_BUILDER_PIN
            }
            viewModel.start(this@BuilderActivity.application, pin)
        }

        setContent {
            ArQcDemoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    BuilderApp(
                        viewModel = viewModel,
                        cameraGranted = cameraPermissionGranted.value,
                        onPinChange = { newPin ->
                            lifecycleScope.launch {
                                Prefs(applicationContext).setBuilderRoomPin(newPin)
                                viewModel.changeRoomPin(applicationContext, newPin)
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "ARQC.Builder"
        /** Baked-in default room PIN for Builder. Different from QC's 471471. */
        const val DEFAULT_BUILDER_PIN = "526526"
    }
}
