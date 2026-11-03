package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

object DocumentProcessor {

    /**
     * Straightens tilted document, crops out surrounding background (bed, table, floor),
     * and maps the 4 detected corners into a clean, flat rectangular document.
     */
    fun cropAndStraightenDocument(source: Bitmap, corners: List<Offset>): Bitmap {
        if (corners.size < 4) return source

        val width = source.width.toFloat()
        val height = source.height.toFloat()

        val tl = corners[0]
        val tr = corners[1]
        val br = corners[2]
        val bl = corners[3]

        val pTlX = tl.x * width
        val pTlY = tl.y * height
        val pTrX = tr.x * width
        val pTrY = tr.y * height
        val pBrX = br.x * width
        val pBrY = br.y * height
        val pBlX = bl.x * width
        val pBlY = bl.y * height

        // Calculate expected output width & height based on edge lengths
        val topWidth = hypot((pTrX - pTlX).toDouble(), (pTrY - pTlY).toDouble()).toFloat()
        val bottomWidth = hypot((pBrX - pBlX).toDouble(), (pBrY - pBlY).toDouble()).toFloat()
        val outWidth = max(topWidth, bottomWidth).toInt().coerceIn(300, 4000)

        val leftHeight = hypot((pBlX - pTlX).toDouble(), (pBlY - pTlY).toDouble()).toFloat()
        val rightHeight = hypot((pBrX - pTrX).toDouble(), (pBrY - pTrY).toDouble()).toFloat()
        val outHeight = max(leftHeight, rightHeight).toInt().coerceIn(400, 5000)

        val srcPoints = floatArrayOf(
            pTlX, pTlY,
            pTrX, pTrY,
            pBrX, pBrY,
            pBlX, pBlY
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        if (!success) {
            // Fallback: simple bounding box crop
            val minX = (minOf(tl.x, bl.x) * width).toInt().coerceAtLeast(0)
            val maxX = (maxOf(tr.x, br.x) * width).toInt().coerceAtMost(source.width)
            val minY = (minOf(tl.y, tr.y) * height).toInt().coerceAtLeast(0)
            val maxY = (maxOf(bl.y, br.y) * height).toInt().coerceAtMost(source.height)

            val cropW = (maxX - minX).coerceAtLeast(10)
            val cropH = (maxY - minY).coerceAtLeast(10)
            return Bitmap.createBitmap(source, minX, minY, cropW, cropH)
        }

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Enhances document legibility, contrast, and clarity while strictly preserving original handwritten/printed text.
     */
    fun enhanceDocumentClarity(source: Bitmap, filterName: String): Bitmap {
        if (filterName == "Original") return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()

        when (filterName) {
            "Magic Color" -> {
                // High contrast with clean paper background and darkened text ink
                val contrast = 1.35f
                val brightness = 20f
                val matrix = floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(matrix)
            }
            "Lighten" -> {
                // Brighten shadows from uneven room lighting
                val brightness = 35f
                val matrix = floatArrayOf(
                    1.1f, 0f, 0f, 0f, brightness,
                    0f, 1.1f, 0f, 0f, brightness,
                    0f, 0f, 1.1f, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(matrix)
            }
            "Enhance" -> {
                // Sharper text contrast
                val contrast = 1.45f
                val matrix = floatArrayOf(
                    contrast, 0f, 0f, 0f, -15f,
                    0f, contrast, 0f, 0f, -15f,
                    0f, 0f, contrast, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(matrix)
            }
            "No Shadow" -> {
                // Balanced lighting across page
                val matrix = floatArrayOf(
                    1.2f, 0f, 0f, 0f, 25f,
                    0f, 1.2f, 0f, 0f, 25f,
                    0f, 0f, 1.2f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(matrix)
            }
            else -> {
                return source
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Saves processed bitmap to file.
     */
    fun saveBitmapToFile(bitmap: Bitmap, targetFile: File): Boolean {
        return try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
