package com.arqcdemo.app.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Wires CameraX's Preview + ImageAnalysis to a PreviewView and an
 * Analyzer. World-facing camera by default (matches AR glasses
 * convention; the Argo's main 48MP camera enumerates as BACK).
 */
class CameraXController(
    private val context: Context,
) {

    private val analyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(analyzerExecutor, analyzer)

                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                provider.unbindAll()
                try {
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (t: Throwable) {
                    Log.w(TAG, "back-camera bind failed, trying front", t)
                    val frontSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                    provider.bindToLifecycle(lifecycleOwner, frontSelector, preview, imageAnalysis)
                }
                Log.i(TAG, "camera bound")
            } catch (t: Throwable) {
                Log.e(TAG, "camera bind failure", t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    fun shutdown() {
        unbind()
        analyzerExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ARQC.Cam"
    }
}
