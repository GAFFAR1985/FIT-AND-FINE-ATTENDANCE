package com.example.ui.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.print.PrintHelper
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object MemberCardGenerator {

    /**
     * Generates a high-resolution, crisp membership card bitmap (800 x 1200 px).
     */
    fun generateCardBitmap(context: Context, member: Member): Bitmap {
        val width = 800
        val height = 1200
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 1. Base card background (White with subtle corner radius)
        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(cardRect, bgPaint)

        // 2. Top Header Navy Banner with Linear Gradient
        val headerHeight = 280f
        val headerRect = RectF(0f, 0f, width.toFloat(), headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), headerHeight,
                "#0A2540".toColorInt(), "#1E3A8A".toColorInt(),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRect(headerRect, headerPaint)

        // 3. Emerald Accent Trim Bar
        val trimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#10B981".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, headerHeight - 6f, width.toFloat(), headerHeight, trimPaint)

        // 4. Header Branding Texts
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FIT & FINE NUTRITION CLUB", width / 2f, 90f, titlePaint)

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#34D399".toColorInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL DIGITAL MEMBERSHIP PASS", width / 2f, 132f, subTitlePaint)

        val mottoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#94A3B8".toColorInt()
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Health • Nutrition • Active Lifestyle", width / 2f, 168f, mottoPaint)

        // 5. Member Avatar Circle (overlapping header and body)
        val avatarCenterX = width / 2f
        val avatarCenterY = headerHeight + 50f
        val avatarRadius = 64f

        // Outer white border ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius + 6f, ringPaint)

        // Emerald accent ring
        val emeraldRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#10B981".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius + 5f, emeraldRingPaint)

        // Try loading photo from local file or URI
        var photoDrawn = false
        val localPhotoFile = File(context.filesDir, "member_photos/${member.memberId}.jpg")
        val photoBitmap: Bitmap? = try {
            if (localPhotoFile.exists()) {
                BitmapFactory.decodeFile(localPhotoFile.absolutePath)
            } else if (member.photoUrl.isNotBlank() && member.photoUrl.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(member.photoUrl))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else if (member.photoUrl.isNotBlank() && member.photoUrl.startsWith("file://")) {
                BitmapFactory.decodeFile(Uri.parse(member.photoUrl).path)
            } else null
        } catch (e: Exception) {
            null
        }

        if (photoBitmap != null) {
            try {
                val avatarPath = android.graphics.Path().apply {
                    addCircle(avatarCenterX, avatarCenterY, avatarRadius, android.graphics.Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(avatarPath)
                val srcRect = if (photoBitmap.width > photoBitmap.height) {
                    val offset = (photoBitmap.width - photoBitmap.height) / 2
                    Rect(offset, 0, offset + photoBitmap.height, photoBitmap.height)
                } else {
                    val offset = (photoBitmap.height - photoBitmap.width) / 2
                    Rect(0, offset, photoBitmap.width, offset + photoBitmap.width)
                }
                val dstRect = RectF(
                    avatarCenterX - avatarRadius,
                    avatarCenterY - avatarRadius,
                    avatarCenterX + avatarRadius,
                    avatarCenterY + avatarRadius
                )
                canvas.drawBitmap(photoBitmap, srcRect, dstRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
                photoDrawn = true
            } catch (e: Exception) {
                photoDrawn = false
            }
        }

        if (!photoDrawn) {
            // Avatar fill
            val avatarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#E2E8F0".toColorInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarFillPaint)

            // Member initials
            val initials = member.fullName.trim().split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifBlank { "M" }

            val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#0A2540".toColorInt()
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textBounds = Rect()
            initialsPaint.getTextBounds(initials, 0, initials.length, textBounds)
            canvas.drawText(initials, avatarCenterX, avatarCenterY + textBounds.height() / 2f, initialsPaint)
        }

        // 6. Member Full Name
        var currentY = avatarCenterY + avatarRadius + 44f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#0F172A".toColorInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(member.fullName, width / 2f, currentY, namePaint)

        // 7. Member ID Pill & Status Badge Row
        currentY += 42f
        val idText = "MEMBER ID: ${member.memberId}"
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#0A2540".toColorInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val idWidth = idPaint.measureText(idText) + 40f
        val idPillRect = RectF((width - idWidth) / 2f, currentY - 26f, (width + idWidth) / 2f, currentY + 12f)
        val idPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F1F5F9".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(idPillRect, 18f, 18f, idPillPaint)
        canvas.drawText(idText, width / 2f, currentY - 2f, idPaint)

        // Status Badge Pill
        currentY += 46f
        val isActive = member.status == MemberStatus.ACTIVE
        val statusText = if (isActive) "● ACTIVE • সক্রিয়" else "● INACTIVE • নিষ্ক্রিয়"
        val statusTextColor = if (isActive) "#16A34A".toColorInt() else "#DC2626".toColorInt()
        val statusBgColor = if (isActive) "#DCFCE7".toColorInt() else "#FEE2E2".toColorInt()

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusTextColor
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusWidth = statusPaint.measureText(statusText) + 36f
        val statusRect = RectF((width - statusWidth) / 2f, currentY - 22f, (width + statusWidth) / 2f, currentY + 10f)
        val statusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusBgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(statusRect, 16f, 16f, statusBgPaint)
        canvas.drawText(statusText, width / 2f, currentY - 2f, statusPaint)

        // Phone Number (if available)
        if (member.mobileNumber.isNotBlank()) {
            currentY += 34f
            val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#475569".toColorInt()
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Phone: ${member.mobileNumber}", width / 2f, currentY, phonePaint)
        }

        // 8. QR Code Container Box
        currentY += 40f
        val qrBoxSize = 380f
        val qrBoxRect = RectF((width - qrBoxSize) / 2f, currentY, (width + qrBoxSize) / 2f, currentY + qrBoxSize)

        // White QR card container with border
        val qrContainerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#E2E8F0".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(qrBoxRect, 20f, 20f, qrContainerPaint)
        canvas.drawRoundRect(qrBoxRect, 20f, 20f, qrBorderPaint)

        // Generate the exact QR Bitmap
        val qrBitmap = QrCodeGenerator.generateQrBitmap(
            content = member.qrPayload,
            size = (qrBoxSize - 40f).toInt(),
            foregroundHex = "#0A2540".toColorInt(),
            backgroundHex = Color.WHITE
        )
        val qrLeft = (width - qrBitmap.width) / 2f
        val qrTop = currentY + 20f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // 9. QR Payload & Instructions
        currentY += qrBoxSize + 34f
        val qrInstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#334155".toColorInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scan QR code for daily attendance check-in", width / 2f, currentY, qrInstructionPaint)

        currentY += 26f
        val payloadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#94A3B8".toColorInt()
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Identifier: ${member.qrPayload}", width / 2f, currentY, payloadPaint)

        // 10. Card Footer
        val footerHeight = 60f
        val footerRect = RectF(0f, height - footerHeight, width.toFloat(), height.toFloat())
        val footerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F8FAFC".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(footerRect, footerBgPaint)

        val footerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#E2E8F0".toColorInt()
            strokeWidth = 2f
        }
        canvas.drawLine(0f, height - footerHeight, width.toFloat(), height - footerHeight, footerDividerPaint)

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#64748B".toColorInt()
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "FIT & FINE NUTRITION CLUB • Official Verified Pass",
            width / 2f,
            height - 24f,
            footerTextPaint
        )

        // Outer border around whole card
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#CBD5E1".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), outerBorderPaint)

        return bitmap
    }

    /**
     * Saves the card bitmap to cache and returns its FileProvider Uri.
     */
    fun getShareableCardUri(context: Context, bitmap: Bitmap, memberId: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "membership_cards").apply { mkdirs() }
            val cleanId = memberId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(cacheDir, "fitfine_card_${cleanId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads/Saves the card as a high-quality PNG image into device storage (Pictures/FitFine).
     */
    fun downloadCardToDevice(context: Context, bitmap: Bitmap, member: Member): Boolean {
        val cleanId = member.memberId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "FitFine_Card_${cleanId}_${System.currentTimeMillis()}.png"

        return try {
            var outputStream: OutputStream? = null
            var savedSuccessfully = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FitFineCards")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val fitFineDir = File(imagesDir, "FitFineCards").apply { mkdirs() }
                val imageFile = File(fitFineDir, fileName)
                outputStream = FileOutputStream(imageFile)
            }

            outputStream?.use { out ->
                savedSuccessfully = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            // Also keep a copy in app files directory as fallback
            val internalDir = File(context.filesDir, "cards").apply { mkdirs() }
            val internalFile = File(internalDir, fileName)
            FileOutputStream(internalFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            savedSuccessfully
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shares the member card PNG image through native Android share sheet (WhatsApp, Email, etc.).
     * Falls back to text sharing if URI/file sharing is not possible.
     */
    fun shareCard(context: Context, bitmap: Bitmap, member: Member) {
        val uri = getShareableCardUri(context, bitmap, member.memberId)

        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Fit & Fine Nutrition Club Member Pass: ${member.fullName}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "FIT & FINE NUTRITION CLUB\n" +
                    "Official Membership Card\n\n" +
                    "Member Name: ${member.fullName}\n" +
                    "Member ID: ${member.memberId}\n" +
                    (if (member.mobileNumber.isNotBlank()) "Phone: ${member.mobileNumber}\n" else "") +
                    "Status: ${if (member.status == MemberStatus.ACTIVE) "Active" else "Inactive"}\n\n" +
                    "Please scan this QR card at the club entrance to mark daily attendance."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Member QR Card"))
        } else {
            // Fallback to text sharing
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "FIT & FINE NUTRITION CLUB\n" +
                    "Member: ${member.fullName}\n" +
                    "ID: ${member.memberId}\n" +
                    "QR Payload: ${member.qrPayload}"
                )
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Member Pass"))
        }
    }

    /**
     * Sends the member card bitmap directly to the Android print system.
     * Contains only the professional membership card without dashboard or extraneous UI elements.
     */
    fun printCard(context: Context, bitmap: Bitmap, member: Member) {
        try {
            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            val jobName = "FitFine_MemberCard_${member.memberId}"
            printHelper.printBitmap(jobName, bitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
