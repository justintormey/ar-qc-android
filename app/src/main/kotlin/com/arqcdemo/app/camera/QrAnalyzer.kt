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
 * QC vocabulary:      single-letter 'A'/'B'/'C'           → onQc(part)
 * Builder vocabulary: 2-char 'AP'/'AF'/'BP'/'BF'/'CP'/'CF' → onBuilderFrame(codes)
 *
 * Builder uses a per-frame callback (a Set of all visible 2-char codes) so the
 * ViewModel can apply compound rules — e.g., step 3 requires BP AND CP visible
 * in the same frame. QC stays single-code since only one QC sticker is in view
 * at a time.
 *
 * Builder frames are throttled: we only fire when the visible code-set changes
 * OR when [debounceMs] has elapsed since the last emit. QC suppresses
 * re-detection of the same code within the debounce window.
 *
 * Each app passes only the callback it cares about; the others default to no-ops.
 */
class QrAnalyzer(
    private val onQc: (Char) -> Unit = {},
    private val onBuilderFrame: (codes: Set<String>) -> Unit = {},
    private val debounceMs: Long = 500L,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile private var lastQcEmitMs = 0L
    @Volatile private var lastQc: Char? = null

    @Volatile private var lastBuilderEmitMs = 0L
    @Volatile private var lastBuilderSet: Set<String>? = null

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
                val all = barcodes.mapNotNull { it.rawValue?.trim()?.uppercase() }
                val qcCodes = all.filter { it.length == 1 && it[0] in "ABC" }
                val builderCodes = all.filter { it.length == 2 && it[0] in "ABC" && it[1] in "PF" }.toSet()

                val now = System.currentTimeMillis()

                // QC: fire on first matching single-letter code, suppress repeats within debounce.
                qcCodes.firstOrNull()?.let { raw ->
                    val ch = raw[0]
                    if (lastQc != ch || (now - lastQcEmitMs) > debounceMs) {
                        lastQcEmitMs = now
                        lastQc = ch
                        Log.i(TAG, "QC code: $raw")
                        onQc(ch)
                    }
                }

                // Builder: fire when the visible Set changes or debounce elapsed.
                if (builderCodes.isNotEmpty()) {
                    val changed = builderCodes != lastBuilderSet
                    if (changed || (now - lastBuilderEmitMs) > debounceMs) {
                        lastBuilderEmitMs = now
                        lastBuilderSet = builderCodes
                        Log.i(TAG, "Builder frame: ${builderCodes.sorted()}")
                        onBuilderFrame(builderCodes)
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
