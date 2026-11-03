package com.example.data.repository

import com.example.R
import com.example.data.model.CategoryItem
import com.example.data.model.ScanType
import com.example.data.model.ScannedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanRepository {

    private val _items = MutableStateFlow<List<ScannedItem>>(getInitialScans())
    val items: StateFlow<List<ScannedItem>> = _items.asStateFlow()

    fun addItem(item: ScannedItem) {
        _items.update { listOf(item) + it }
    }

    fun removeItem(id: String) {
        _items.update { list -> list.filterNot { it.id == id } }
    }

    fun toggleFavorite(id: String) {
        _items.update { list ->
            list.map {
                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun getItemById(id: String): ScannedItem? {
        return _items.value.find { it.id == id }
    }

    companion object {
        fun getInitialCategories(): List<CategoryItem> = listOf(
            CategoryItem("cat_smart_scan", "Google Drive Scanner", "scanner", "HOT", "Google Drive official engine"),
            CategoryItem("cat_pdf_tools", "PDF Tools", "picture_as_pdf", "HOT", "Merge, split & lock"),
            CategoryItem("cat_import_img", "Import Image", "add_photo_alternate", null, "From photo gallery"),
            CategoryItem("cat_import_file", "Import File", "folder_open", null, "PDF, DOCX, TXT"),
            CategoryItem("cat_id_card", "ID Card Scanner", "badge", "2-Sided", "Front & Back on A4"),
            CategoryItem("cat_ocr", "Extract Text", "document_scanner", "OCR", "Instant text copy"),
            CategoryItem("cat_image_tools", "Image Tools", "auto_fix_high", null, "Enhance & filter"),
            CategoryItem("cat_all", "All Categories", "grid_view", "24+", "Full utilities hub")
        )

        private fun getInitialScans(): List<ScannedItem> = listOf(
            ScannedItem(
                id = "scan-1",
                title = "Employment Agreement_Signed",
                type = ScanType.DOCUMENT,
                timestamp = "10 mins ago",
                pageCount = 3,
                fileSize = "2.4 MB",
                content = "This Employment Agreement is entered into as of October 2026 by and between TechCorp Inc. and the Employee. Section 1: Responsibilities and Scope of Employment...",
                isFavorite = true,
                filterApplied = "Magic Color",
                thumbnailRes = R.drawable.banner_scanner
            ),
            ScannedItem(
                id = "scan-2",
                title = "WiFi: Office_5G_Guest",
                type = ScanType.QR_CODE,
                timestamp = "45 mins ago",
                pageCount = 1,
                fileSize = "12 KB",
                content = "WIFI:S:Office_5G_Guest;T:WPA;P:SecurePass2026!;;",
                qrPayloadType = "WIFI",
                isFavorite = false
            ),
            ScannedItem(
                id = "scan-3",
                title = "National Identity Card",
                type = ScanType.ID_CARD,
                timestamp = "Yesterday, 4:15 PM",
                pageCount = 2,
                fileSize = "1.8 MB",
                content = "FRONT: Name: Alexander Vance | ID: 9482-1049-3829 | DOB: 14/08/1992 | BACK: Address: 742 Evergreen Terrace, Springfield",
                isFavorite = true,
                filterApplied = "B&W Document"
            ),
            ScannedItem(
                id = "scan-4",
                title = "https://github.com/android/compose",
                type = ScanType.QR_CODE,
                timestamp = "Yesterday, 11:30 AM",
                pageCount = 1,
                fileSize = "18 KB",
                content = "https://github.com/android/compose",
                qrPayloadType = "URL",
                isFavorite = false
            ),
            ScannedItem(
                id = "scan-5",
                title = "Medical Invoice & Receipt",
                type = ScanType.DOCUMENT,
                timestamp = "Oct 28, 2026",
                pageCount = 1,
                fileSize = "890 KB",
                content = "Saint Jude Medical Center. Total Amount Due: $350.00. Payment Status: PAID IN FULL via Card ending 4821. Thank you for your visit.",
                isFavorite = false,
                filterApplied = "Magic Color"
            ),
            ScannedItem(
                id = "scan-6",
                title = "Lecture Notes: Deep Learning",
                type = ScanType.OCR_TEXT,
                timestamp = "Oct 26, 2026",
                pageCount = 4,
                fileSize = "45 KB",
                content = "Chapter 4: Gradient Descent Optimizers. SGD with Momentum helps accelerate gradients vectors in the right directions, leading to faster converging...",
                isFavorite = true
            )
        )
    }
}
