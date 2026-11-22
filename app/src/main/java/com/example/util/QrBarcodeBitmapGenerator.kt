package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBarcodeBitmapGenerator {

    fun is1DBarcode(formatCode: Int?): Boolean {
        return when (formatCode) {
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODABAR -> true
            else -> false
        }
    }

    fun generateCodeBitmap(
        content: String,
        formatCode: Int? = null
    ): Bitmap? {
        if (content.isBlank()) return null

        val targetFormat = mapMlKitToZxingFormat(formatCode)
        val is1D = is1DBarcode(formatCode)
        val width = if (is1D) 720 else 560
        val height = if (is1D) 240 else 560

        return tryEncode(content, targetFormat, width, height)
            ?: if (is1D) {
                tryEncode(content, BarcodeFormat.CODE_128, 720, 240)
            } else {
                null
            }
            ?: tryEncode(content, BarcodeFormat.QR_CODE, 560, 560)
    }

    private fun tryEncode(
        content: String,
        format: BarcodeFormat,
        width: Int,
        height: Int
    ): Bitmap? {
        return try {
            val hints = mutableMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            if (format == BarcodeFormat.QR_CODE) {
                hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            }
            val matrix: BitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)
            val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(matrix.width * matrix.height)
            for (y in 0 until matrix.height) {
                val offset = y * matrix.width
                for (x in 0 until matrix.width) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            bitmap.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun mapMlKitToZxingFormat(formatCode: Int?): BarcodeFormat {
        return when (formatCode) {
            Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
            Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
            Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
            Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
            Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
            Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
            Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
            Barcode.FORMAT_CODE_93 -> BarcodeFormat.CODE_93
            Barcode.FORMAT_ITF -> BarcodeFormat.ITF
            Barcode.FORMAT_CODABAR -> BarcodeFormat.CODABAR
            Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
            Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
            Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
            else -> BarcodeFormat.QR_CODE
        }
    }
}
