package com.example.util

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.example.data.repository.GeminiRepository

data class DocumentAiAnalysisResult(
    val corners: List<Offset>,
    val documentTitle: String,
    val textSummary: String,
    val confidence: Float
)

/**
 * Adapter delegating document boundary analysis to GeminiRepository.
 */
class GeminiDocumentAnalyzer(
    private val geminiRepository: GeminiRepository = GeminiRepository()
) {

    /**
     * Analyzes document photo using Gemini AI to detect the exact 4 boundaries of the paper/notebook,
     * ignoring background clutter (bed, table, floor, bedsheet).
     */
    suspend fun analyzeDocumentBounds(bitmap: Bitmap): DocumentAiAnalysisResult {
        val result = geminiRepository.getAutoCropAndDeskewInstructions(bitmap)
        val instructions = result.getOrElse { geminiRepository.getFallbackCropInstructions(bitmap) }

        return DocumentAiAnalysisResult(
            corners = instructions.corners,
            documentTitle = instructions.suggestedTitle,
            textSummary = "Processed via Gemini 3.8 Flash (${instructions.documentType})",
            confidence = instructions.confidence
        )
    }
}

