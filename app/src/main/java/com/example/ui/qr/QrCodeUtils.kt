package com.example.ui.qr

import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.ByteBuffer

object QrCodeGenerator {
    /**
     * Generates a square QR Code Bitmap using ZXing
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        foregroundHex: Int = 0xFF0A2540.toInt(), // Deep Navy Blue
        backgroundHex: Int = 0xFFFFFFFF.toInt()  // Pure White
    ): Bitmap {
        val writer = QRCodeWriter()
        val hints = HashMap<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"

        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundHex else backgroundHex
            }
        }

        val bitmap = createBitmap(width, height)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}

object SoundFeedback {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            // Ignore if audio is restricted in environment
        }
    }

    fun playSuccess() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun playWarning() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 220)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    private var lastScannedTimestamp = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Throttle scans to avoid duplicate rapid frame events (at least 1.5s delay)
        if (currentTime - lastScannedTimestamp < 1500) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val buffer = mediaImage.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    bytes,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                val result = reader.decodeWithState(binaryBitmap)
                if (result != null && result.text.isNotBlank()) {
                    lastScannedTimestamp = currentTime
                    onQrCodeScanned(result.text.trim())
                }
            } catch (e: Exception) {
                // Framing didn't detect QR code in this frame, expected during scanning
            } finally {
                reader.reset()
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}
