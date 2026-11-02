package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenDestination
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.QuickScanBottomSheet
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.ScanViewModel

@Composable
fun ToolsScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    BackHandler {
        viewModel.navigateTo(ScreenDestination.HOME)
    }

    Scaffold(
        containerColor = PureWhite,
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tools Directory",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Complete suite of scanner & PDF utilities",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: Capture & Scanning Tools
            ToolSectionHeader(title = "Capture & Scanning")
            ToolRowItem(
                title = "Smart Document Scanner",
                subtitle = "Auto-edge detection, perspective warp & filters",
                icon = Icons.Filled.Scanner,
                iconTint = AccentBlue,
                iconBg = Color(0xFFEFF6FF),
                onClick = { viewModel.navigateTo(ScreenDestination.DOC_SCANNER) }
            )
            ToolRowItem(
                title = "QR Code & Barcode Scanner",
                subtitle = "Real-time laser viewfinder & link actions",
                icon = Icons.Filled.QrCode2,
                iconTint = AccentGreen,
                iconBg = Color(0xFFECFDF5),
                onClick = { viewModel.navigateTo(ScreenDestination.QR_SCANNER) }
            )
            ToolRowItem(
                title = "ID Card Scanner (2-Sided)",
                subtitle = "Front & back capture merged onto single sheet",
                icon = Icons.Filled.Badge,
                iconTint = Color(0xFF7C3AED),
                iconBg = Color(0xFFF5F3FF),
                onClick = { viewModel.navigateTo(ScreenDestination.ID_CARD_SCANNER) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: PDF & Document Utilities
            ToolSectionHeader(title = "PDF Operations")
            ToolRowItem(
                title = "PDF Toolbox",
                subtitle = "Merge scans, compress file size & lock PDF",
                icon = Icons.Filled.PictureAsPdf,
                iconTint = Color(0xFFDC2626),
                iconBg = Color(0xFFFEF2F2),
                onClick = { viewModel.navigateTo(ScreenDestination.PDF_TOOLS) }
            )
            ToolRowItem(
                title = "Extract Text (AI OCR)",
                subtitle = "Digitize text from images into editable notes",
                icon = Icons.Filled.DocumentScanner,
                iconTint = Color(0xFF0891B2),
                iconBg = Color(0xFFECFEFF),
                onClick = { viewModel.navigateTo(ScreenDestination.OCR_SCREEN) }
            )
            ToolRowItem(
                title = "Import Device Files",
                subtitle = "Browse local PDF, DOCX or image files to enhance",
                icon = Icons.Filled.FolderOpen,
                iconTint = Color(0xFFD97706),
                iconBg = Color(0xFFFFFBEB),
                onClick = { viewModel.navigateTo(ScreenDestination.DOCUMENTS) }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (uiState.showQuickScanSheet) {
        QuickScanBottomSheet(
            onDismiss = { viewModel.showQuickScanSheet(false) },
            onDocScanClick = { viewModel.navigateTo(ScreenDestination.DOC_SCANNER) },
            onQrScanClick = { viewModel.navigateTo(ScreenDestination.QR_SCANNER) },
            onIdCardScanClick = { viewModel.navigateTo(ScreenDestination.ID_CARD_SCANNER) },
            onOcrScanClick = { viewModel.navigateTo(ScreenDestination.OCR_SCREEN) }
        )
    }
}

@Composable
private fun ToolSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 14.sp
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ToolRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                1.dp,
                SurfaceBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
