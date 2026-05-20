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
 * Runs Google ML Kit Barcode Scanning on every camera frame and dispatches
 * decoded codes into the right callback.
 *
 * QC vocabulary: single-letter codes 'A', 'B', 'C'           → onQc(part)
 * Builder vocabulary: 2-char codes 'AP'/'AF'/'BP'/'BF'/'CP'/'CF'
 *                                                              → onBuilder(part, isPass)
 *
 * Debounced 500ms — re-seeing the same code within that window is suppressed
 * so we don't spam the ViewModel while the participant holds the part still.
 *
 * Only QR codes are scanned (the only format we ship). Each app passes only
 * the callback it cares about; the other is a no-op lambda.
 */
class QrAnalyzer(
    private val onQc: (Char) -> Unit = {},
    private val onBuilder: (part: Char, isPass: Boolean) -> Unit = { _, _ -> },
    private val debounceMs: Long = 500L,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile private var lastEmitMs = 0L
    @Volatile private var lastValue: String? = null

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
                    if (!matches(raw)) continue

                    val now = System.currentTimeMillis()
                    if (lastValue != raw || (now - lastEmitMs) > debounceMs) {
                        lastEmitMs = now
                        lastValue = raw
                        Log.i(TAG, "QR detected: $raw")
                        dispatch(raw)
                        break
                    }
                }
            }
            .addOnFailureListener { Log.w(TAG, "ML Kit barcode failure", it) }
            .addOnCompleteListener { image.close() }
    }

    private fun matches(raw: String): Boolean =
        (raw.length == 1 && raw[0] in "ABC") ||
        (raw.length == 2 && raw[0] in "ABC" && raw[1] in "PF")

    private fun dispatch(raw: String) {
        if (raw.length == 1) {
            onQc(raw[0])
        } else {
            val part = raw[0]
            val isPass = raw[1] == 'P'
            onBuilder(part, isPass)
        }
    }

    fun release() {
        scanner.close()
    }

    companion object {
        private const val TAG = "ARQC.QR"
    }
}
