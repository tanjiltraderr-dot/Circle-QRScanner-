package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenDestination
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.ScanViewModel

data class WifiInfo(
    val ssid: String,
    val password: String,
    val security: String
)

@Composable
fun QrResultScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val item = uiState.activeQrResult

    // Back to QR Scanner
    BackHandler {
        viewModel.navigateTo(ScreenDestination.QR_SCANNER)
    }

    if (item == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PureWhite),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No scan result found", color = TextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.navigateTo(ScreenDestination.QR_SCANNER) }) {
                    Text("Return to Scanner")
                }
            }
        }
        return
    }

    val rawContent = item.content
    val payloadType = item.qrPayloadType ?: "TEXT"

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val wifiInfo = remember(rawContent) {
        if (payloadType == "WIFI") parseWifiString(rawContent) else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Top Header: Back Button, Title, and Save/Bookmark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button -> Returns cleanly back to QR Scanner
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, SurfaceBorder, CircleShape)
                    .clickable { viewModel.navigateTo(ScreenDestination.QR_SCANNER) }
                    .testTag("qr_result_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Scanner",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Scanned Result",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextPrimary
            )

            // Save to Library / History toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSaved) AccentGreen.copy(alpha = 0.12f) else OffWhite)
                    .border(1.dp, SurfaceBorder, CircleShape)
                    .clickable {
                        viewModel.saveActiveQrResult()
                        isSaved = true
                    }
                    .testTag("qr_result_save_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Save Result",
                    tint = if (isSaved) AccentGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Type Badge & Header Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (payloadType) {
                                "WIFI" -> Color(0xFFEFF6FF)
                                "URL" -> Color(0xFFECFDF5)
                                "BARCODE" -> Color(0xFFF5F3FF)
                                else -> Color(0xFFF1F5F9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (payloadType) {
                            "WIFI" -> Icons.Default.Wifi
                            "URL" -> Icons.Default.OpenInBrowser
                            "BARCODE" -> Icons.Default.QrCode2
                            "PHONE" -> Icons.Default.Call
                            "EMAIL" -> Icons.Default.Email
                            else -> Icons.Default.QrCode2
                        },
                        contentDescription = null,
                        tint = when (payloadType) {
                            "WIFI" -> AccentBlue
                            "URL" -> AccentGreen
                            "BARCODE" -> Color(0xFF7C3AED)
                            else -> AccentBlue
                        },
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (payloadType) {
                                    "WIFI" -> AccentBlue.copy(alpha = 0.12f)
                                    "URL" -> AccentGreen.copy(alpha = 0.12f)
                                    else -> Color(0xFFF1F5F9)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (payloadType) {
                                "WIFI" -> "WI-FI NETWORK"
                                "URL" -> "WEBSITE LINK"
                                "PHONE" -> "PHONE NUMBER"
                                "EMAIL" -> "EMAIL ADDRESS"
                                "BARCODE" -> "BARCODE PRODUCT"
                                else -> "TEXT CONTENT"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = when (payloadType) {
                                    "WIFI" -> AccentBlue
                                    "URL" -> AccentGreen
                                    else -> TextPrimary
                                }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details Container
            if (payloadType == "WIFI" && wifiInfo != null) {
                // Specialized Wi-Fi Details Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OffWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Wi-Fi Network Details",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // SSID Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Network Name (SSID)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                Text(wifiInfo.ssid, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp))
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(wifiInfo.ssid))
                                viewModel.showSnackbar("Network name copied: ${wifiInfo.ssid}")
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy SSID", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Password", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                Text(
                                    text = if (isPasswordVisible) wifiInfo.password else "•".repeat(wifiInfo.password.length.coerceAtLeast(8)),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = if (isPasswordVisible) FontFamily.Monospace else FontFamily.Default,
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            Row {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(wifiInfo.password))
                                    viewModel.showSnackbar("Password copied to clipboard!")
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Security
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Security: ${wifiInfo.security.ifBlank { "WPA/WPA2" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                            )
                        }
                    }
                }
            } else {
                // Standard Content Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OffWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Decoded Content",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            )

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rawContent))
                                    viewModel.showSnackbar("Content copied to clipboard")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = rawContent,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Contextual Action Buttons
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (payloadType) {
                "WIFI" -> {
                    // Connect to Wi-Fi button
                    Button(
                        onClick = {
                            if (wifiInfo != null) {
                                // Auto-copy password and open Wi-Fi settings
                                clipboardManager.setText(AnnotatedString(wifiInfo.password))
                                viewModel.showSnackbar("Password '${wifiInfo.password}' copied! Opening Wi-Fi settings...")
                                try {
                                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showSnackbar("Please connect to ${wifiInfo.ssid} in Settings")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("qr_wifi_connect_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Connect to Wi-Fi Network",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            if (wifiInfo != null) {
                                clipboardManager.setText(AnnotatedString(wifiInfo.password))
                                viewModel.showSnackbar("Wi-Fi Password copied!")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Password", fontWeight = FontWeight.SemiBold)
                    }
                }

                "URL" -> {
                    // Open in Browser
                    Button(
                        onClick = {
                            try {
                                val url = if (rawContent.startsWith("http://", ignoreCase = true) || rawContent.startsWith("https://", ignoreCase = true)) {
                                    rawContent
                                } else {
                                    "https://$rawContent"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showSnackbar("Could not open URL: ${e.message}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("qr_open_url_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Open in Browser", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(rawContent))
                            viewModel.showSnackbar("Link copied to clipboard")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Link", fontWeight = FontWeight.SemiBold)
                    }
                }

                "PHONE" -> {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse(if (rawContent.startsWith("tel:")) rawContent else "tel:$rawContent"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Number", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                "EMAIL" -> {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(if (rawContent.startsWith("mailto:")) rawContent else "mailto:$rawContent"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Email", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                "BARCODE" -> {
                    Button(
                        onClick = {
                            try {
                                val searchUrl = "https://www.google.com/search?q=${Uri.encode(rawContent)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Barcode Product on Web", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(rawContent))
                            viewModel.showSnackbar("Text copied to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Full Text", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Share Action
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, rawContent)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Code"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Content", fontWeight = FontWeight.SemiBold)
            }
        }

        // 3. Bottom Bar: "Scan Another Code" Button
        Surface(
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveActiveQrResult()
                        viewModel.navigateTo(ScreenDestination.QR_SCANNER)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("qr_scan_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Scan Another Code", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun parseWifiString(raw: String): WifiInfo? {
    if (!raw.startsWith("WIFI:", ignoreCase = true)) return null
    val content = raw.substring(5)
    var ssid = ""
    var pass = ""
    var sec = "WPA"

    val tokens = content.split(";")
    for (token in tokens) {
        val trimmed = token.trim()
        when {
            trimmed.startsWith("S:", ignoreCase = true) -> ssid = trimmed.substring(2)
            trimmed.startsWith("P:", ignoreCase = true) -> pass = trimmed.substring(2)
            trimmed.startsWith("T:", ignoreCase = true) -> sec = trimmed.substring(2)
        }
    }
    return if (ssid.isNotEmpty()) WifiInfo(ssid, pass, sec) else null
}
