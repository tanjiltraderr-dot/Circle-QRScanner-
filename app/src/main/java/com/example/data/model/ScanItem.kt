package com.example.data.model

import java.util.UUID

enum class ScanType {
    DOCUMENT,
    QR_CODE,
    ID_CARD,
    OCR_TEXT,
    PDF_FILE
}

data class ScannedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ScanType,
    val timestamp: String,
    val pageCount: Int = 1,
    val fileSize: String = "1.2 MB",
    val content: String = "",
    val qrPayloadType: String? = null, // e.g. "URL", "WIFI", "TEXT"
    val isFavorite: Boolean = false,
    val filterApplied: String = "Magic Color",
    val thumbnailRes: Int? = null,
    val customNotes: String = "",
    val pdfFilePath: String? = null
)

data class CapturedPageData(
    val pageNumber: Int,
    val title: String = "Page $pageNumber",
    val filterApplied: String = "Magic Color",
    val timestamp: Long = System.currentTimeMillis()
)

data class CategoryItem(
    val id: String,
    val title: String,
    val iconName: String,
    val badge: String? = null,
    val description: String = ""
)

enum class CameraMode {
    SINGLE,
    BATCH,
    TEXT_OCR,
    ID_CARD,
    BOOK
}

enum class QrScanTargetMode {
    QR_CODE,
    BARCODE
}

enum class DocFilter {
    ORIGINAL,
    LIGHTEN,
    ENHANCE,
    MAGIC_COLOR,
    NO_SHADOW
}

data class CropCoordinates(
    val topLeft: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(0.10f, 0.12f),
    val topRight: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(0.90f, 0.12f),
    val bottomRight: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(0.90f, 0.88f),
    val bottomLeft: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset(0.10f, 0.88f)
)

enum class ScreenDestination {
    HOME,
    TOOLS,
    DOCUMENTS,
    SETTINGS,
    QR_SCANNER,
    DOC_SCANNER,
    CROP_EDITOR,
    ID_CARD_SCANNER,
    OCR_SCREEN,
    PDF_TOOLS,
    IMAGE_TOOLS,
    DOCUMENT_DETAIL
}
