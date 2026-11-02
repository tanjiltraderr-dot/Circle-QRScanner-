package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ScreenDestination
import com.example.ui.screens.CropEditorScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentScannerScreen
import com.example.ui.screens.DocumentsLibraryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IdCardScannerScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ScanViewModel = viewModel()
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Crossfade(
        targetState = uiState.currentDestination,
        label = "screen_transition"
    ) { destination ->
        when (destination) {
            ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
            ScreenDestination.QR_SCANNER -> QrScannerScreen(viewModel = viewModel)
            ScreenDestination.DOC_SCANNER -> DocumentScannerScreen(viewModel = viewModel)
            ScreenDestination.CROP_EDITOR -> CropEditorScreen(viewModel = viewModel)
            ScreenDestination.ID_CARD_SCANNER -> IdCardScannerScreen(viewModel = viewModel)
            ScreenDestination.OCR_SCREEN -> OcrScreen(viewModel = viewModel)
            ScreenDestination.PDF_TOOLS -> PdfToolsScreen(viewModel = viewModel)
            ScreenDestination.IMAGE_TOOLS -> DocumentScannerScreen(viewModel = viewModel)
            ScreenDestination.DOCUMENTS -> DocumentsLibraryScreen(viewModel = viewModel)
            ScreenDestination.TOOLS -> ToolsScreen(viewModel = viewModel)
            ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            ScreenDestination.DOCUMENT_DETAIL -> DocumentDetailScreen(viewModel = viewModel)
        }
    }
}
