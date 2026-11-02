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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenDestination
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.components.QuickScanBottomSheet
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberLight
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.ScanViewModel

@Composable
fun SettingsScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var autoSaveToGallery by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }
    var highResolutionScan by remember { mutableStateOf(true) }

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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Preferences & Configurations",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )
                }
            }

            // PRO Upgrade Banner Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AmberLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .clickable { viewModel.showProModal(true) }
                    .testTag("settings_pro_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AmberGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = PureWhite)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isProUser) "Circle Scanner PRO Active" else "Upgrade to Circle Scanner PRO",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        )
                        Text(
                            text = if (uiState.isProUser) "All premium scanning features unlocked" else "Unlimited OCR, HD 300 DPI, and zero ads",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB45309),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = AmberGold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scanner Preferences
            Text(
                text = "Scanner Configuration",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    // Switch 1: Auto save
                    SettingSwitchRow(
                        title = "Auto-save to Gallery",
                        subtitle = "Automatically export high-res JPG to camera roll",
                        icon = Icons.Filled.PhotoAlbum,
                        checked = autoSaveToGallery,
                        onCheckedChange = { autoSaveToGallery = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch 2: High Resolution
                    SettingSwitchRow(
                        title = "Ultra HD 300 DPI Quality",
                        subtitle = "Sharper text and maximum clarity for legal documents",
                        icon = Icons.Filled.HighQuality,
                        checked = highResolutionScan,
                        onCheckedChange = { highResolutionScan = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch 3: Haptics
                    SettingSwitchRow(
                        title = "Haptic Shutter Feedback",
                        subtitle = "Vibrate on successful document border capture",
                        icon = Icons.Filled.Vibration,
                        checked = hapticFeedback,
                        onCheckedChange = { hapticFeedback = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Data & Cache
            Text(
                text = "Data & Storage",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showSnackbar("Temporary cache cleared (18.4 MB freed)!") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OffWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.CleaningServices, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear Cache & Temp Files", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary))
                            Text("18.4 MB temporary preview data", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                        }
                        Text("Clear", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // About
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OffWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Circle Scanner v3.1.0", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary))
                            Text("Fast, Private & Secure On-Device Processing", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (uiState.showProModal) {
        ProUpgradeDialog(
            isPro = uiState.isProUser,
            onDismiss = { viewModel.showProModal(false) },
            onUpgrade = { viewModel.activateProUpgrade() }
        )
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
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OffWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = AccentBlue
            )
        )
    }
}
