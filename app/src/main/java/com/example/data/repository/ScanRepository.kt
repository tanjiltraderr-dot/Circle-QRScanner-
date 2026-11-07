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
            CategoryItem("cat_smart_scan", "Smart Scanner", "scanner", null, "Auto-crop edge detection"),
            CategoryItem("cat_pdf_tools", "PDF Tools", "picture_as_pdf", null, "Merge, split & lock"),
            CategoryItem("cat_import_img", "Import Image", "add_photo_alternate", null, "From photo gallery"),
            CategoryItem("cat_import_file", "Import File", "folder_open", null, "PDF, DOCX, TXT"),
            CategoryItem("cat_id_card", "ID Card Scanner", "badge", null, "Front & Back on A4"),
            CategoryItem("cat_ocr", "Extract Text", "document_scanner", null, "Instant text copy"),
            CategoryItem("cat_image_tools", "Image Tools", "auto_fix_high", null, "Enhance & filter"),
            CategoryItem("cat_all", "All Categories", "grid_view", null, "Full utilities hub")
        )

        private fun getInitialScans(): List<ScannedItem> = emptyList()
    }
}
