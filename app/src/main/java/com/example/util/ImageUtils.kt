package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * Creates a temporary image file in the app cache and returns the content Uri via FileProvider.
     */
    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val storageDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val tempFile = File.createTempFile("captured_${System.currentTimeMillis()}_", ".jpg", storageDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(uri, tempFile)
    }

    /**
     * Compresses and resizes an image from Uri to a compact JPEG ByteArray suitable for Firebase Storage.
     * Respects EXIF orientation so photos taken with the camera are not rotated sideways.
     */
    fun compressImageFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 600,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Read image bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate inSampleSize
            var sampleSize = 1
            val maxEdge = max(origWidth, origHeight)
            while (maxEdge / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // 3. Decode scaled down bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 4. Handle EXIF rotation
            val rotationAngle = getExifOrientationAngle(context, uri)
            val orientedBitmap = if (rotationAngle != 0) {
                val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    sampledBitmap, 0, 0,
                    sampledBitmap.width, sampledBitmap.height,
                    matrix, true
                )
                if (rotated != sampledBitmap) sampledBitmap.recycle()
                rotated
            } else {
                sampledBitmap
            }

            // 5. Final scale down to maxDimension if still larger
            val finalWidth = orientedBitmap.width
            val finalHeight = orientedBitmap.height
            val finalBitmap = if (max(finalWidth, finalHeight) > maxDimension) {
                val ratio = maxDimension.toFloat() / max(finalWidth, finalHeight).toFloat()
                val targetW = (finalWidth * ratio).toInt().coerceAtLeast(1)
                val targetH = (finalHeight * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
                if (scaled != orientedBitmap) orientedBitmap.recycle()
                scaled
            } else {
                orientedBitmap
            }

            // 6. Compress to JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            finalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image from URI: $uri", e)
            null
        }
    }

    private fun getExifOrientationAngle(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Saves compressed image bytes to the app's internal persistent directory.
     * Returns the File.
     */
    fun saveImageLocally(context: Context, memberId: String, imageBytes: ByteArray): File {
        val dir = File(context.filesDir, "member_photos").apply { mkdirs() }
        val file = File(dir, "$memberId.jpg")
        FileOutputStream(file).use { it.write(imageBytes) }
        return file
    }

    fun deleteLocalMemberPhoto(context: Context, memberId: String): Boolean {
        val file = File(context.filesDir, "member_photos/$memberId.jpg")
        return if (file.exists()) file.delete() else true
    }
}
