package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ScreenDestination
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QrGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ScanViewModel
import com.example.util.GoogleDriveScannerHelper

@Composable
fun DocumentScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    var isLaunchingGms by remember { mutableStateOf(false) }
    var gmsLaunchError by remember { mutableStateOf<String?>(null) }
    var hasAttemptedLaunch by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.HOME)
    }

    // Google Drive Document Scanner Launcher
    val googleDriveScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        isLaunchingGms = false
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val gmsResult = GoogleDriveScannerHelper.parseScanResult(
                activityResult.resultCode,
                activityResult.data
            )
            if (gmsResult != null) {
                val pdfUri = gmsResult.pdf?.uri
                val pageCount = gmsResult.pdf?.pageCount ?: gmsResult.pages?.size ?: 1
                val pageUris = gmsResult.pages?.map { it.imageUri }
                viewModel.handleGoogleDriveScanResult(
                    context = context,
                    pdfUri = pdfUri,
                    pageUris = pageUris,
                    pageCount = pageCount
                )
            } else {
                viewModel.navigateTo(ScreenDestination.HOME)
            }
        } else {
            // User backed out or cancelled scan
            viewModel.navigateTo(ScreenDestination.HOME)
        }
    }

    val launchGoogleDriveScanner: () -> Unit = {
        if (activity != null) {
            isLaunchingGms = true
            gmsLaunchError = null
            GoogleDriveScannerHelper.startScan(
                activity = activity,
                onIntentSenderReady = { intentSender ->
                    try {
                        googleDriveScannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    } catch (e: Exception) {
                        isLaunchingGms = false
                        gmsLaunchError = e.localizedMessage ?: "Failed to start scanner"
                    }
                },
                onError = { exc ->
                    isLaunchingGms = false
                    gmsLaunchError = exc.localizedMessage ?: "Google Drive Scanner not available on this device"
                    viewModel.showSnackbar("Opening Scanner: ${exc.localizedMessage ?: "Please try again"}")
                }
            )
        }
    }

    // Auto-launch Google Drive document scanner immediately on opening the screen
    LaunchedEffect(Unit) {
        if (!hasAttemptedLaunch) {
            hasAttemptedLaunch = true
            launchGoogleDriveScanner()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { viewModel.navigateTo(ScreenDestination.HOME) }
                        .testTag("doc_scanner_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Slate900,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Google Drive Scanner",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )

                // Placeholder for symmetry
                Spacer(modifier = Modifier.size(46.dp))
            }

            // Center Content: Branded Google Drive Document Scanner Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05))
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Google Drive Document Scanner",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Google Drive Document Scanner",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Powered by official Google Play Services ML Kit Document Scanner. Automatic edge detection, perspective rectification, shadow removal, and direct PDF generation.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF64748B),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLaunchingGms) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE8F0FE))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF1A73E8),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Starting Google Drive Scanner...",
                            color = Color(0xFF1A73E8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else if (gmsLaunchError != null) {
                    Text(
                        text = "Notice: $gmsLaunchError",
                        color = Color(0xFFD93025),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Bottom Action: Big "Open Google Drive Scanner" Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { launchGoogleDriveScanner() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("launch_google_drive_scanner_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Scan with Google Drive",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                    }
                }

                Button(
                    onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Slate900
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Return to Home",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
