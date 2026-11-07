package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.data.model.ScreenDestination
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.HeaderSection
import com.example.ui.components.HomeCategoriesGrid
import com.example.ui.components.MainBannerCards
import com.example.ui.components.PdfSuccessBottomSheet
import com.example.ui.components.ProfileDialog
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.components.QuickScanBottomSheet
import com.example.ui.components.RecentHistorySection
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.ScanViewModel
import com.example.util.GoogleDriveScannerHelper
import com.example.util.findActivity

@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filteredScans by viewModel.filteredScans.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Google Drive Document Scanner Launcher:
    // Directly launches Google Drive scanner when user clicks "Document Scanner" card on Home Screen
    // Once user crops/scans in Google Drive scanner, the resulting image is passed directly into CropEditorScreen!
    val googleDriveScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val gmsResult = GoogleDriveScannerHelper.parseScanResult(
                activityResult.resultCode,
                activityResult.data
            )
            if (gmsResult != null) {
                val firstUri = gmsResult.pages?.firstOrNull()?.imageUri?.toString()
                    ?: gmsResult.pdf?.uri?.toString()
                if (firstUri != null) {
                    viewModel.openScannedImageInCropEditor(
                        imageUri = firstUri,
                        context = context,
                        pdfUri = gmsResult.pdf?.uri?.toString(),
                        pageCount = gmsResult.pdf?.pageCount ?: 1
                    )
                } else {
                    viewModel.handleGoogleDriveScanResult(
                        context = context,
                        pdfUri = gmsResult.pdf?.uri,
                        pageUris = gmsResult.pages?.map { it.imageUri },
                        pageCount = gmsResult.pdf?.pageCount ?: 1
                    )
                }
            }
        }
    }

    val launchGoogleDriveScanner: () -> Unit = {
        val currentActivity = context.findActivity()
        if (currentActivity != null) {
            GoogleDriveScannerHelper.startScan(
                activity = currentActivity,
                onIntentSenderReady = { intentSender ->
                    try {
                        googleDriveScannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    } catch (e: Exception) {
                        viewModel.showSnackbar("Unable to open Google Drive Scanner: ${e.message}")
                    }
                },
                onError = { exc ->
                    viewModel.showSnackbar("Google Drive Scanner: ${exc.message ?: "Please update Google Play Services"}")
                }
            )
        } else {
            viewModel.showSnackbar("Please wait, initializing scanner...")
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigationBar(
                currentDestination = uiState.currentDestination,
                onNavigate = { viewModel.navigateTo(it) },
                onQuickScanClick = { viewModel.showQuickScanSheet(true) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(scrollState)
        ) {
            // 1. Header Section: App Name "Circle Scanner", PRO Button, Profile Icon, Search Bar
            HeaderSection(
                appName = "Circle Scanner",
                isProUser = uiState.isProUser,
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onProClick = { viewModel.showProModal(true) },
                onProfileClick = { viewModel.showProfileModal(true) }
            )

            // 2. Main Action Banner / Cards: Dual-Card layout (QR Scanner & Document Scanner)
            // Clicking Document Scanner directly opens Google Drive's Scanner and passes cropped image to CropEditorScreen!
            MainBannerCards(
                onQrScannerClick = { viewModel.navigateTo(ScreenDestination.QR_SCANNER) },
                onDocScannerClick = { launchGoogleDriveScanner() }
            )

            // 3. Home Categories (Grid View - 2 Rows of 4 Items = 8 Total Categories)
            HomeCategoriesGrid(
                categories = viewModel.categories,
                onCategoryClick = { category ->
                    when (category.id) {
                        "cat_smart_scan" -> launchGoogleDriveScanner()
                        "cat_pdf_tools" -> viewModel.navigateTo(ScreenDestination.PDF_TOOLS)
                        "cat_import_img" -> launchGoogleDriveScanner()
                        "cat_import_file" -> {
                            viewModel.navigateTo(ScreenDestination.DOCUMENTS)
                            viewModel.showSnackbar("Showing all imported documents")
                        }
                        "cat_id_card" -> viewModel.navigateTo(ScreenDestination.ID_CARD_SCANNER)
                        "cat_ocr" -> viewModel.navigateTo(ScreenDestination.OCR_SCREEN)
                        "cat_image_tools" -> launchGoogleDriveScanner()
                        "cat_all" -> viewModel.navigateTo(ScreenDestination.TOOLS)
                    }
                }
            )

            // 4. Recent History Section (Below Categories)
            RecentHistorySection(
                items = filteredScans,
                activeFilterTab = uiState.activeFilterTab,
                onFilterTabChange = { viewModel.setFilterTab(it) },
                onItemClick = { viewModel.openItemDetail(it) },
                onShareClick = { item ->
                    if (item.pdfFilePath != null) {
                        val file = java.io.File(item.pdfFilePath)
                        if (file.exists()) {
                            com.example.util.PdfGenerator.sharePdfFile(context, file, item.title)
                            return@RecentHistorySection
                        }
                    }
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Circle Scanner: ${item.title}\n${item.content}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                },
                onDeleteClick = { viewModel.deleteItem(it.id) },
                onFavoriteToggle = { viewModel.toggleFavorite(it.id) },
                onViewAllClick = { viewModel.navigateTo(ScreenDestination.DOCUMENTS) }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Interactive Modals
    if (uiState.showProModal) {
        ProUpgradeDialog(
            isPro = uiState.isProUser,
            onDismiss = { viewModel.showProModal(false) },
            onUpgrade = { viewModel.activateProUpgrade() }
        )
    }

    if (uiState.showProfileModal) {
        ProfileDialog(
            isProUser = uiState.isProUser,
            onDismiss = { viewModel.showProfileModal(false) },
            onUpgradeClick = { viewModel.showProModal(true) }
        )
    }

    if (uiState.showQuickScanSheet) {
        QuickScanBottomSheet(
            onDismiss = { viewModel.showQuickScanSheet(false) },
            onDocScanClick = { launchGoogleDriveScanner() },
            onQrScanClick = { viewModel.navigateTo(ScreenDestination.QR_SCANNER) },
            onIdCardScanClick = { viewModel.navigateTo(ScreenDestination.ID_CARD_SCANNER) },
            onOcrScanClick = { viewModel.navigateTo(ScreenDestination.OCR_SCREEN) }
        )
    }

    if (uiState.showPdfSuccessSheet && uiState.pdfCompilationResult != null) {
        PdfSuccessBottomSheet(
            result = uiState.pdfCompilationResult!!,
            onDismiss = { viewModel.dismissPdfSuccessSheet() },
            onViewInLibrary = {
                viewModel.dismissPdfSuccessSheet()
                viewModel.navigateTo(ScreenDestination.DOCUMENTS)
            }
        )
    }
}
