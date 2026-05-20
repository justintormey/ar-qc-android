package com.arqcdemo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arqcdemo.app.settings.Prefs
import com.arqcdemo.app.ui.DemoApp
import com.arqcdemo.app.ui.theme.ArQcDemoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DemoViewModel by viewModels()
    private val cameraPermissionGranted = mutableStateOf(false)

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted.value = granted
        Log.i(TAG, "camera permission granted=$granted")
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

        // Load the persisted room PIN (if any) and start the bus. The Argo has
        // no easy keyboard so we bake in a default PIN on first launch; the
        // colleague's controller uses the same default. The user can later
        // change it via the SetupScreen (which is only reached if the PIN is
        // ever cleared, e.g. via `adb shell pm clear ...`).
        lifecycleScope.launch {
            val prefs = Prefs(applicationContext)
            var pin = prefs.roomPin.first()
            if (pin.isBlank()) {
                prefs.setRoomPin(DEFAULT_ROOM_PIN)
                pin = DEFAULT_ROOM_PIN
            }
            viewModel.start(this@MainActivity.application, pin)
        }

        setContent {
            ArQcDemoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    DemoApp(
                        viewModel = viewModel,
                        cameraGranted = cameraPermissionGranted.value,
                        onPinChange = { newPin ->
                            lifecycleScope.launch {
                                Prefs(applicationContext).setRoomPin(newPin)
                                viewModel.changeRoomPin(applicationContext, newPin)
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "ARQC"
        /** Baked-in default room PIN used on first launch. */
        const val DEFAULT_ROOM_PIN = "471471"
    }
}
