package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenDestination
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.EmptyHistoryState
import com.example.ui.components.QuickScanBottomSheet
import com.example.ui.components.RecentScanCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.ScanViewModel

@Composable
fun DocumentsLibraryScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val filteredScans by viewModel.filteredScans.collectAsState()

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
                        text = "All Documents",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredScans.size} items in library",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text("Search library...", color = TextSubtle, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OffWhite,
                    unfocusedContainerColor = OffWhite,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Documents", "QR Codes", "Favorites").forEach { tab ->
                    val isSelected = uiState.activeFilterTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilterTab(tab) },
                        label = { Text(tab, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = PureWhite,
                            containerColor = OffWhite,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AccentBlue else SurfaceBorder
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Documents List
            if (filteredScans.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredScans, key = { it.id }) { item ->
                        RecentScanCard(
                            item = item,
                            onClick = { viewModel.openItemDetail(item) },
                            onShare = {
                                if (item.pdfFilePath != null) {
                                    val file = java.io.File(item.pdfFilePath)
                                    if (file.exists()) {
                                        com.example.util.PdfGenerator.sharePdfFile(context, file, item.title)
                                        return@RecentScanCard
                                    }
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Circle Scanner: ${item.title}\n${item.content}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                            },
                            onDelete = { viewModel.deleteItem(item.id) },
                            onFavorite = { viewModel.toggleFavorite(item.id) }
                        )
                    }
                }
            }
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
