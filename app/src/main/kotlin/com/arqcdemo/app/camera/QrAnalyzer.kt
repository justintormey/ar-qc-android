package com.arqcdemo.app.camera

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Runs Google ML Kit Barcode Scanning on every camera frame and emits
 * a single-letter callback ('A' | 'B' | 'C') when one of the demo's
 * QR codes is detected. Debounces 500ms so we don't spam the
 * ViewModel while the operator holds the part still.
 *
 * Only QR codes are scanned (the only format we ship), keeping the
 * analyzer cheap.
 */
class QrAnalyzer(
    private val onDetected: (Char) -> Unit,
    private val debounceMs: Long = 500L,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile private var lastEmitMs = 0L
    @Volatile private var lastValue: Char? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                for (b in barcodes) {
                    val raw = b.rawValue?.trim()?.uppercase() ?: continue
                    if (raw.length == 1 && raw[0] in "ABC") {
                        val now = System.currentTimeMillis()
                        if (lastValue != raw[0] || (now - lastEmitMs) > debounceMs) {
                            lastEmitMs = now
                            lastValue = raw[0]
                            Log.i(TAG, "QR detected: $raw")
                            onDetected(raw[0])
                            break
                        }
                    }
                }
            }
            .addOnFailureListener { Log.w(TAG, "ML Kit barcode failure", it) }
            .addOnCompleteListener { image.close() }
    }

    fun release() {
        scanner.close()
    }

    companion object {
        private const val TAG = "ARQC.QR"
    }
}
