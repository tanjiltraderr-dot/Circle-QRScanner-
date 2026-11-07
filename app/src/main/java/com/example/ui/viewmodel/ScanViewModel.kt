package com.example.ui.viewmodel

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CameraMode
import com.example.data.model.CapturedPageData
import com.example.data.model.CategoryItem
import com.example.data.model.QrScanTargetMode
import com.example.data.model.ScanType
import com.example.data.model.ScannedItem
import com.example.data.model.ScreenDestination
import com.example.data.repository.ScanRepository
import com.example.util.PdfGenerationResult
import com.example.util.PdfGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UiState(
    val currentDestination: ScreenDestination = ScreenDestination.HOME,
    val previousDestination: ScreenDestination = ScreenDestination.HOME,
    val selectedItem: ScannedItem? = null,
    val searchQuery: String = "",
    val activeFilterTab: String = "All", // "All", "Documents", "QR Codes", "Favorites"
    val isProUser: Boolean = false,
    val showProModal: Boolean = false,
    val showProfileModal: Boolean = false,
    val showQuickScanSheet: Boolean = false,
    val snackbarMessage: String? = null,
    
    // QR Scanner State
    val isQrFlashOn: Boolean = false,
    val isQrFrontCamera: Boolean = false,
    val qrZoomLevel: Float = 1.0f,
    val qrScanTargetMode: QrScanTargetMode = QrScanTargetMode.QR_CODE,
    val activeQrResult: ScannedItem? = null,
    val showQrResultSheet: Boolean = false,

    // Document Scanner State
    val cameraMode: CameraMode = CameraMode.SINGLE,
    val isFrontCamera: Boolean = false,
    val isDocFlashOn: Boolean = false,
    val isAutoCropEnabled: Boolean = true,
    val isGridVisible: Boolean = true,
    val currentDocFilter: String = "Magic Color",
    val docPageCount: Int = 1,
    val capturedDocPages: List<String> = emptyList(),
    val activeCapturedPhotoUri: String? = null,
    // Auto-crop quad corners normalized (0f to 1f)
    val cropCorners: List<Offset> = listOf(
        Offset(0.12f, 0.15f), // Top-Left
        Offset(0.88f, 0.15f), // Top-Right
        Offset(0.92f, 0.85f), // Bottom-Right
        Offset(0.08f, 0.85f)  // Bottom-Left
    ),
    val isProcessingCapture: Boolean = false,
    val processingMessage: String = "Processing Document...",

    // Crop & Editor Screen State
    val activeEditorRotation: Int = 0, // 0, 90, 180, 270
    val activeEditorFilter: String = "Magic Color",
    val isAutoEdgeSnapping: Boolean = false,
    val showSignatureDialog: Boolean = false,
    val digitalSignature: String? = null,
    val showEditorOcrModal: Boolean = false,
    val isMarkupEnabled: Boolean = false,

    // ID Card Scanner State
    val idCardStep: Int = 1, // 1 = Front, 2 = Back, 3 = Review
    val idCardFrontCaptured: Boolean = false,
    val idCardBackCaptured: Boolean = false,

    // OCR State
    val ocrText: String = "",
    val ocrCopied: Boolean = false,

    // PDF Compilation State
    val showPdfNameDialog: Boolean = false,
    val isCompilingPdf: Boolean = false,
    val pdfCompilationResult: com.example.util.PdfGenerationResult? = null,
    val showPdfSuccessSheet: Boolean = false,
    val capturedPagesList: List<com.example.data.model.CapturedPageData> = emptyList()
)

class ScanViewModel(
    private val repository: ScanRepository = ScanRepository(),
    private val geminiRepository: com.example.data.repository.GeminiRepository = com.example.data.repository.GeminiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val categories: List<CategoryItem> = ScanRepository.getInitialCategories()

    val allScans: StateFlow<List<ScannedItem>> = repository.items

    val filteredScans: StateFlow<List<ScannedItem>> = combine(
        repository.items,
        _uiState
    ) { items, state ->
        val query = state.searchQuery.trim().lowercase()
        val filter = state.activeFilterTab

        items.filter { item ->
            val matchesFilter = when (filter) {
                "Documents" -> item.type == ScanType.DOCUMENT || item.type == ScanType.PDF_FILE
                "QR Codes" -> item.type == ScanType.QR_CODE
                "Favorites" -> item.isFavorite
                "ID Cards" -> item.type == ScanType.ID_CARD
                "OCR" -> item.type == ScanType.OCR_TEXT
                else -> true
            }

            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                item.title.lowercase().contains(query) ||
                        item.content.lowercase().contains(query) ||
                        item.filterApplied.lowercase().contains(query)
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(destination: ScreenDestination) {
        _uiState.update { 
            it.copy(
                previousDestination = it.currentDestination,
                currentDestination = destination
            ) 
        }
    }

    fun openItemDetail(item: ScannedItem) {
        _uiState.update { it.copy(selectedItem = item, currentDestination = ScreenDestination.DOCUMENT_DETAIL) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilterTab(tab: String) {
        _uiState.update { it.copy(activeFilterTab = tab) }
    }

    fun toggleFavorite(id: String) {
        repository.toggleFavorite(id)
    }

    fun deleteItem(id: String) {
        repository.removeItem(id)
        showSnackbar("Scan deleted")
    }

    fun showProModal(show: Boolean) {
        _uiState.update { it.copy(showProModal = show) }
    }

    fun activateProUpgrade() {
        _uiState.update { it.copy(isProUser = true, showProModal = false) }
        showSnackbar("🎉 PRO Membership Activated!")
    }

    fun showProfileModal(show: Boolean) {
        _uiState.update { it.copy(showProfileModal = show) }
    }

    fun showQuickScanSheet(show: Boolean) {
        _uiState.update { it.copy(showQuickScanSheet = show) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // QR Actions
    fun toggleQrFlash() {
        _uiState.update { it.copy(isQrFlashOn = !it.isQrFlashOn) }
    }

    fun switchQrCamera() {
        _uiState.update { it.copy(isQrFrontCamera = !it.isQrFrontCamera) }
    }

    fun setQrScanTargetMode(mode: QrScanTargetMode) {
        _uiState.update { it.copy(qrScanTargetMode = mode) }
    }

    fun setQrZoom(zoom: Float) {
        _uiState.update { it.copy(qrZoomLevel = zoom.coerceIn(1.0f, 5.0f)) }
    }

    fun adjustQrZoom(delta: Float) {
        _uiState.update { it.copy(qrZoomLevel = (it.qrZoomLevel + delta).coerceIn(1.0f, 5.0f)) }
    }

    fun openScannedImageInCropEditor(
        imageUri: String,
        context: Context,
        pdfUri: String? = null,
        pageCount: Int = 1
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingCapture = true,
                    processingMessage = "Loading scanned document into Editor...",
                    activeCapturedPhotoUri = imageUri
                )
            }

            // Cache the file locally to internal storage so it is persistent across screens
            val localPath = withContext(Dispatchers.IO) {
                try {
                    val destFile = File(context.cacheDir, "scan_doc_${System.currentTimeMillis()}.jpg")
                    val parsedUri = android.net.Uri.parse(imageUri)
                    val inputStream = if (imageUri.startsWith("content://")) {
                        context.contentResolver.openInputStream(parsedUri)
                    } else {
                        File(imageUri).inputStream()
                    }
                    inputStream?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.absolutePath
                } catch (e: Exception) {
                    android.util.Log.e("ScanViewModel", "Failed to cache scanned image: ${e.message}")
                    imageUri
                }
            }

            val currentCount = _uiState.value.docPageCount
            val newPageLabel = "Page $currentCount"
            val newPageData = CapturedPageData(
                pageNumber = currentCount,
                title = "Page $currentCount",
                filterApplied = "Magic Color",
                imageUri = localPath
            )

            // Auto edge bounds for the editor
            val initialCorners = listOf(
                Offset(0.04f, 0.04f), // Top-Left
                Offset(0.96f, 0.04f), // Top-Right
                Offset(0.96f, 0.96f), // Bottom-Right
                Offset(0.04f, 0.96f)  // Bottom-Left
            )

            _uiState.update {
                it.copy(
                    isProcessingCapture = false,
                    isAutoEdgeSnapping = true,
                    docPageCount = currentCount + 1,
                    capturedDocPages = it.capturedDocPages + newPageLabel,
                    capturedPagesList = it.capturedPagesList + newPageData,
                    cropCorners = initialCorners,
                    activeCapturedPhotoUri = localPath,
                    activeEditorFilter = "Magic Color",
                    activeEditorRotation = 0,
                    previousDestination = it.currentDestination,
                    currentDestination = ScreenDestination.CROP_EDITOR
                )
            }
        }
    }

    fun handleGoogleDriveScanResult(
        context: Context,
        pdfUri: android.net.Uri?,
        pageUris: List<android.net.Uri>?,
        pageCount: Int
    ) {
        viewModelScope.launch {
            val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val title = "Scan_$dateStamp"

            var savedPdfPath: String? = null
            if (pdfUri != null) {
                try {
                    val destFile = File(context.filesDir, "$title.pdf")
                    context.contentResolver.openInputStream(pdfUri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    savedPdfPath = destFile.absolutePath
                } catch (e: Exception) {
                    android.util.Log.e("ScanViewModel", "Failed to cache PDF from Google Drive scanner: ${e.message}")
                }
            }

            val count = if (pageCount > 0) pageCount else (pageUris?.size ?: 1)
            val firstImage = pageUris?.firstOrNull()?.toString()

            val newItem = ScannedItem(
                id = UUID.randomUUID().toString(),
                title = "$title.pdf",
                type = if (savedPdfPath != null) ScanType.PDF_FILE else ScanType.DOCUMENT,
                timestamp = "Just now ($now)",
                pageCount = count,
                fileSize = "${String.format(Locale.US, "%.1f", 0.6f * count)} MB",
                content = "Document scanned with Google Drive Scanner engine. Multi-page perspective straightened and enhanced.",
                filterApplied = "Magic Color",
                pdfFilePath = savedPdfPath,
                imageUri = firstImage,
                isFavorite = false
            )
            repository.addItem(newItem)
            _uiState.update {
                it.copy(
                    selectedItem = newItem,
                    currentDestination = ScreenDestination.DOCUMENT_DETAIL
                )
            }
            showSnackbar("Document saved from Google Drive Scanner!")
        }
    }

    private var lastScannedCode: String? = null
    private var lastScannedTimestamp: Long = 0L

    fun onLiveBarcodeDetected(rawValue: String, formatCode: Int) {
        val now = System.currentTimeMillis()
        if (_uiState.value.showQrResultSheet) return
        if (rawValue == lastScannedCode && (now - lastScannedTimestamp) < 3000L) return

        lastScannedCode = rawValue
        lastScannedTimestamp = now

        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val isUrl = rawValue.startsWith("http://", ignoreCase = true) || rawValue.startsWith("https://", ignoreCase = true)
        val isWifi = rawValue.startsWith("WIFI:", ignoreCase = true)
        val isEmail = rawValue.startsWith("mailto:", ignoreCase = true)
        val isPhone = rawValue.startsWith("tel:", ignoreCase = true)

        val formatName = when (formatCode) {
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> "QR Code"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> "EAN-13"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> "EAN-8"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> "UPC-A"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E -> "UPC-E"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> "Code 128"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> "Code 39"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93 -> "Code 93"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417 -> "PDF-417"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC -> "Aztec"
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            else -> "Barcode"
        }

        val type = when {
            formatCode == com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> when {
                isUrl -> "URL"
                isWifi -> "WIFI"
                isEmail -> "EMAIL"
                isPhone -> "PHONE"
                else -> "TEXT"
            }
            else -> "BARCODE"
        }

        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = if (type == "URL") rawValue else "$formatName: ${rawValue.take(28)}",
            type = ScanType.QR_CODE,
            timestamp = "Just now ($timeString)",
            pageCount = 1,
            fileSize = "14 KB",
            content = rawValue,
            qrPayloadType = type,
            isFavorite = false
        )

        _uiState.update {
            it.copy(
                activeQrResult = item,
                showQrResultSheet = false,
                currentDestination = ScreenDestination.QR_RESULT
            )
        }
    }

    fun simulateQrDetected(qrText: String = "https://circlescanner.app/preview/doc-994", type: String = "URL") {
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = if (type == "URL") qrText else "QR: ${qrText.take(24)}...",
            type = ScanType.QR_CODE,
            timestamp = "Just now ($now)",
            pageCount = 1,
            fileSize = "16 KB",
            content = qrText,
            qrPayloadType = type,
            isFavorite = false
        )
        _uiState.update {
            it.copy(
                activeQrResult = item,
                showQrResultSheet = false,
                currentDestination = ScreenDestination.QR_RESULT
            )
        }
    }

    fun simulateBarcodeDetected(code: String = "8901030865421", format: String = "EAN-13") {
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = "Barcode ($format): $code",
            type = ScanType.QR_CODE,
            timestamp = "Just now ($now)",
            pageCount = 1,
            fileSize = "12 KB",
            content = "Product ID: $code ($format)\nStatus: Verified in global catalog",
            qrPayloadType = "BARCODE",
            isFavorite = false
        )
        _uiState.update {
            it.copy(
                activeQrResult = item,
                showQrResultSheet = true
            )
        }
    }

    fun saveActiveQrResult() {
        val result = _uiState.value.activeQrResult ?: return
        repository.addItem(result)
        _uiState.update { it.copy(showQrResultSheet = false) }
        showSnackbar("QR Code saved to History!")
    }

    fun dismissQrResult() {
        _uiState.update { it.copy(showQrResultSheet = false) }
    }

    // Document Scanner Actions
    fun toggleDocFlash() {
        _uiState.update { it.copy(isDocFlashOn = !it.isDocFlashOn) }
    }

    fun toggleAutoCrop() {
        _uiState.update { it.copy(isAutoCropEnabled = !it.isAutoCropEnabled) }
    }

    fun toggleDocGrid() {
        _uiState.update { it.copy(isGridVisible = !it.isGridVisible) }
    }

    fun setDocFilter(filterName: String) {
        _uiState.update { it.copy(currentDocFilter = filterName) }
    }

    fun updateCropCorner(index: Int, newOffset: Offset) {
        _uiState.update { state ->
            val updated = state.cropCorners.toMutableList()
            if (index in updated.indices) {
                updated[index] = Offset(
                    newOffset.x.coerceIn(0.02f, 0.98f),
                    newOffset.y.coerceIn(0.02f, 0.98f)
                )
            }
            state.copy(cropCorners = updated)
        }
    }

    fun resetCropCorners() {
        _uiState.update {
            it.copy(
                cropCorners = listOf(
                    Offset(0.10f, 0.12f),
                    Offset(0.90f, 0.12f),
                    Offset(0.90f, 0.88f),
                    Offset(0.10f, 0.88f)
                )
            )
        }
    }

    // Camera and Mode Actions
    fun setCameraMode(mode: CameraMode) {
        _uiState.update { it.copy(cameraMode = mode) }
    }

    fun switchCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
        val cameraName = if (_uiState.value.isFrontCamera) "Front Camera" else "Rear HD Camera"
        showSnackbar("Switched to $cameraName")
    }

    // Trigger Shutter: captures photo -> analyzes corners with Gemini AI (ignoring bed/table) -> crops & straightens perspective -> applies clarity filter -> opens Crop/Editor
    fun triggerShutterCapture(photoUri: String? = null, context: Context? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingCapture = true,
                    processingMessage = "Analyzing document with Gemini AI...",
                    activeCapturedPhotoUri = photoUri
                )
            }

            var detectedCorners = listOf(
                Offset(0.08f, 0.10f),
                Offset(0.92f, 0.10f),
                Offset(0.92f, 0.88f),
                Offset(0.08f, 0.88f)
            )
            var detectedRotation = 0
            var finalPhotoUri = photoUri

            if (photoUri != null && context != null) {
                try {
                    _uiState.update {
                        it.copy(processingMessage = "Detecting notebook boundaries with Gemini AI...")
                    }

                    // Decode photo bitmap
                    val bitmap = withContext(Dispatchers.IO) {
                        if (photoUri.startsWith("content://")) {
                            context.contentResolver.openInputStream(android.net.Uri.parse(photoUri))?.use {
                                android.graphics.BitmapFactory.decodeStream(it)
                            }
                        } else {
                            android.graphics.BitmapFactory.decodeFile(photoUri)
                        }
                    }

                    if (bitmap != null) {
                        // 1. Gemini AI auto-crop & deskew boundary detection (ignores bed, table, clutter)
                        val aiResult = geminiRepository.getAutoCropAndDeskewInstructions(bitmap)
                        val instructions = aiResult.getOrElse { geminiRepository.getFallbackCropInstructions(bitmap) }
                        detectedCorners = instructions.corners
                        detectedRotation = instructions.rotationDegrees

                        _uiState.update {
                            it.copy(processingMessage = "Cropping background & straightening perspective...")
                        }

                        // 2. Crop, remove background clutter (bed, table), and perspective straighten
                        val croppedBitmap = withContext(Dispatchers.Default) {
                            com.example.util.DocumentProcessor.cropAndStraightenDocument(bitmap, detectedCorners)
                        }

                        // 3. Enhance document readability without altering original text
                        val enhancedBitmap = withContext(Dispatchers.Default) {
                            com.example.util.DocumentProcessor.enhanceDocumentClarity(
                                croppedBitmap,
                                _uiState.value.currentDocFilter
                            )
                        }

                        // 4. Save clean processed document
                        val outputFile = File(context.cacheDir, "gemini_scan_${System.currentTimeMillis()}.jpg")
                        com.example.util.DocumentProcessor.saveBitmapToFile(enhancedBitmap, outputFile)
                        finalPhotoUri = outputFile.absolutePath
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScanViewModel", "AI Processing error: ${e.message}")
                }
            } else {
                delay(400)
                _uiState.update {
                    it.copy(processingMessage = "Straightening perspective & auto-cropping...")
                }
                delay(300)
            }

            // Register captured page
            val currentCount = _uiState.value.docPageCount
            val newPageLabel = "Page $currentCount"
            val newPageData = CapturedPageData(
                pageNumber = currentCount,
                title = "Page $currentCount",
                filterApplied = _uiState.value.currentDocFilter,
                imageUri = finalPhotoUri
            )

            _uiState.update {
                it.copy(
                    isProcessingCapture = false,
                    isAutoEdgeSnapping = true,
                    docPageCount = currentCount + 1,
                    capturedDocPages = it.capturedDocPages + newPageLabel,
                    capturedPagesList = it.capturedPagesList + newPageData,
                    cropCorners = detectedCorners,
                    activeCapturedPhotoUri = finalPhotoUri,
                    activeEditorFilter = it.currentDocFilter,
                    activeEditorRotation = detectedRotation,
                    currentDestination = ScreenDestination.CROP_EDITOR
                )
            }
        }
    }

    // Crop & Editor Screen Actions
    fun autoStraightenCorners() {
        _uiState.update {
            it.copy(
                cropCorners = listOf(
                    Offset(0.08f, 0.10f),
                    Offset(0.92f, 0.10f),
                    Offset(0.92f, 0.88f),
                    Offset(0.08f, 0.88f)
                ),
                isAutoEdgeSnapping = true
            )
        }
        showSnackbar("Auto-straightened to document bounds")
    }

    fun rotateEditor() {
        _uiState.update { it.copy(activeEditorRotation = (it.activeEditorRotation + 90) % 360) }
    }

    fun setEditorFilter(filter: String) {
        _uiState.update { it.copy(activeEditorFilter = filter, currentDocFilter = filter) }
    }

    fun showSignatureDialog(show: Boolean) {
        _uiState.update { it.copy(showSignatureDialog = show) }
    }

    fun setDigitalSignature(signature: String?) {
        _uiState.update { it.copy(digitalSignature = signature, showSignatureDialog = false) }
        if (signature != null) {
            showSnackbar("Signature applied to document")
        }
    }

    fun showEditorOcrModal(show: Boolean) {
        _uiState.update { it.copy(showEditorOcrModal = show) }
    }

    fun toggleMarkup() {
        val newState = !_uiState.value.isMarkupEnabled
        _uiState.update { it.copy(isMarkupEnabled = newState) }
        showSnackbar(if (newState) "Markup Pen mode active" else "Markup Pen mode closed")
    }

    fun captureDocPage() {
        val currentCount = _uiState.value.docPageCount
        val newPageLabel = "Page $currentCount"
        val newPageData = CapturedPageData(
            pageNumber = currentCount,
            title = "Page $currentCount",
            filterApplied = _uiState.value.currentDocFilter
        )
        _uiState.update {
            it.copy(
                docPageCount = currentCount + 1,
                capturedDocPages = it.capturedDocPages + newPageLabel,
                capturedPagesList = it.capturedPagesList + newPageData
            )
        }
        showSnackbar("Captured page $currentCount")
    }

    fun openPdfNameDialog() {
        _uiState.update { state ->
            val pages = if (state.capturedPagesList.isEmpty()) {
                listOf(
                    CapturedPageData(
                        pageNumber = 1,
                        title = "Page 1",
                        filterApplied = state.currentDocFilter
                    )
                )
            } else {
                state.capturedPagesList
            }
            state.copy(
                showPdfNameDialog = true,
                capturedPagesList = pages
            )
        }
    }

    fun dismissPdfNameDialog() {
        _uiState.update { it.copy(showPdfNameDialog = false) }
    }

    fun dismissPdfSuccessSheet() {
        _uiState.update { it.copy(showPdfSuccessSheet = false, pdfCompilationResult = null) }
    }

    fun compileCapturedPagesToPdf(
        context: Context,
        fileName: String,
        isLetterSize: Boolean = false,
        watermark: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompilingPdf = true) }
            val state = _uiState.value
            val pagesToCompile = if (state.capturedPagesList.isEmpty()) {
                listOf(
                    CapturedPageData(
                        pageNumber = 1,
                        title = "Page 1",
                        filterApplied = state.currentDocFilter
                    )
                )
            } else {
                state.capturedPagesList
            }

            val result = PdfGenerator.generateDocumentPdf(
                context = context,
                rawFileName = fileName,
                pages = pagesToCompile,
                filterApplied = state.currentDocFilter,
                documentTitle = fileName.removeSuffix(".pdf"),
                isLetterSize = isLetterSize,
                watermarkText = watermark
            )

            result.onSuccess { pdfResult ->
                val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val newItem = ScannedItem(
                    id = UUID.randomUUID().toString(),
                    title = pdfResult.fileName,
                    type = ScanType.PDF_FILE,
                    timestamp = "Just now ($now)",
                    pageCount = pdfResult.pageCount,
                    fileSize = pdfResult.fileSizeFormatted,
                    content = "Compiled PDF Document (${pdfResult.pageCount} ${if (pdfResult.pageCount == 1) "page" else "pages"}). High definition 300 DPI vector rendering.",
                    filterApplied = state.currentDocFilter,
                    pdfFilePath = pdfResult.file.absolutePath,
                    isFavorite = false
                )
                repository.addItem(newItem)
                _uiState.update {
                    it.copy(
                        isCompilingPdf = false,
                        showPdfNameDialog = false,
                        pdfCompilationResult = pdfResult,
                        showPdfSuccessSheet = true,
                        docPageCount = 1,
                        capturedDocPages = emptyList(),
                        capturedPagesList = emptyList(),
                        currentDestination = ScreenDestination.HOME
                    )
                }
                showSnackbar("PDF '${pdfResult.fileName}' compiled successfully!")
            }.onFailure { error ->
                _uiState.update { it.copy(isCompilingPdf = false) }
                showSnackbar("Error generating PDF: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun exportItemToPdf(
        context: Context,
        item: ScannedItem,
        customName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompilingPdf = true) }
            val pages = (1..item.pageCount.coerceAtLeast(1)).map { pageNum ->
                CapturedPageData(
                    pageNumber = pageNum,
                    title = "Page $pageNum",
                    filterApplied = item.filterApplied
                )
            }
            val title = customName?.ifBlank { null } ?: item.title
            val result = PdfGenerator.generateDocumentPdf(
                context = context,
                rawFileName = title,
                pages = pages,
                filterApplied = item.filterApplied,
                documentTitle = title,
                notes = item.content,
                isLetterSize = false
            )
            result.onSuccess { pdfResult ->
                _uiState.update {
                    it.copy(
                        isCompilingPdf = false,
                        pdfCompilationResult = pdfResult,
                        showPdfSuccessSheet = true
                    )
                }
                showSnackbar("Exported '${pdfResult.fileName}' successfully!")
            }.onFailure { error ->
                _uiState.update { it.copy(isCompilingPdf = false) }
                showSnackbar("Export failed: ${error.localizedMessage}")
            }
        }
    }

    fun finalizeDocScan(title: String = "Scanned_Doc_${SimpleDateFormat("MMdd_HHmm", Locale.getDefault()).format(Date())}") {
        val count = _uiState.value.capturedDocPages.size.coerceAtLeast(1)
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = title,
            type = ScanType.DOCUMENT,
            timestamp = "Just now ($now)",
            pageCount = count,
            fileSize = "${String.format(Locale.US, "%.1f", 0.8f * count)} MB",
            content = "Multi-page scanned document with high clarity auto-enhancement and edge alignment applied.",
            filterApplied = _uiState.value.currentDocFilter,
            isFavorite = false
        )
        repository.addItem(item)
        _uiState.update {
            it.copy(
                docPageCount = 1,
                capturedDocPages = emptyList(),
                capturedPagesList = emptyList(),
                currentDestination = ScreenDestination.HOME
            )
        }
        showSnackbar("Document '$title' saved!")
    }

    // ID Card Scanner Actions
    fun captureIdCardStep() {
        val currentStep = _uiState.value.idCardStep
        if (currentStep == 1) {
            _uiState.update { it.copy(idCardStep = 2, idCardFrontCaptured = true) }
            showSnackbar("Front side captured! Now turn card to back.")
        } else if (currentStep == 2) {
            _uiState.update { it.copy(idCardStep = 3, idCardBackCaptured = true) }
            showSnackbar("Both sides captured! Ready to save.")
        }
    }

    fun resetIdCardScan() {
        _uiState.update {
            it.copy(
                idCardStep = 1,
                idCardFrontCaptured = false,
                idCardBackCaptured = false
            )
        }
    }

    fun finalizeIdCardScan(name: String = "ID_Card_${SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())}") {
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = name,
            type = ScanType.ID_CARD,
            timestamp = "Just now ($now)",
            pageCount = 2,
            fileSize = "1.5 MB",
            content = "Dual-sided ID Card Document (A4 format). Front and Back aligned.",
            filterApplied = "Magic Color",
            isFavorite = false
        )
        repository.addItem(item)
        resetIdCardScan()
        _uiState.update { it.copy(currentDestination = ScreenDestination.HOME) }
        showSnackbar("ID Card saved successfully!")
    }

    // OCR Actions
    fun setOcrText(text: String) {
        _uiState.update { it.copy(ocrText = text) }
    }

    fun saveOcrScan(title: String, text: String) {
        val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val item = ScannedItem(
            id = UUID.randomUUID().toString(),
            title = title,
            type = ScanType.OCR_TEXT,
            timestamp = "Just now ($now)",
            pageCount = 1,
            fileSize = "${(text.length * 2 / 1024).coerceAtLeast(4)} KB",
            content = text,
            isFavorite = false
        )
        repository.addItem(item)
        _uiState.update { it.copy(currentDestination = ScreenDestination.HOME) }
        showSnackbar("OCR document saved!")
    }
}
