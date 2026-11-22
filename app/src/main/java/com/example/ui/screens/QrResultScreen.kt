package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.viewmodel.ScanViewModel
import com.example.util.ParsedQrPayload
import com.example.util.QrBarcodeBitmapGenerator
import com.example.util.QrPayloadKind
import com.example.util.QrPayloadParser
import com.example.util.WifiConnector

private data class QrActionButtonModel(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun QrResultScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val item = uiState.activeQrResult

    val backTarget = remember(uiState.previousDestination) {
        when (uiState.previousDestination) {
            ScreenDestination.HOME, ScreenDestination.DOCUMENTS -> uiState.previousDestination
            else -> ScreenDestination.QR_SCANNER
        }
    }

    BackHandler {
        viewModel.navigateTo(backTarget)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
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
    val parsed = remember(rawContent, item.qrPayloadType, item.barcodeFormat) {
        val isBarcodeFormat = QrBarcodeBitmapGenerator.is1DBarcode(item.barcodeFormat) ||
            item.qrPayloadType == "BARCODE"
        QrPayloadParser.parse(
            rawContent = rawContent,
            payloadHint = item.qrPayloadType,
            isBarcode = isBarcodeFormat
        )
    }

    val qrCodeBitmap = remember(rawContent, item.barcodeFormat) {
        QrBarcodeBitmapGenerator.generateCodeBitmap(rawContent, item.barcodeFormat)
    }

    var isPasswordVisible by remember(item.id) { mutableStateOf(false) }
    var isSaved by remember(item.id) { mutableStateOf(false) }
    var pendingPickerTitle by remember { mutableStateOf<String?>(null) }
    var pendingPickerItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingPickerOnSelect by remember { mutableStateOf<(String) -> Unit>({}) }

    // Native Android 11+ Wi-Fi Add Networks Launcher
    val wifiAddLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val ssid = parsed.wifiInfo?.ssid ?: "Wi-Fi"
        if (result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val results = result.data?.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST)
                val code = results?.firstOrNull()
                when (code) {
                    Settings.ADD_WIFI_RESULT_SUCCESS ->
                        viewModel.showSnackbar("Saved & connecting to '$ssid'!")
                    Settings.ADD_WIFI_RESULT_ALREADY_EXISTS ->
                        viewModel.showSnackbar("'$ssid' is already saved. Connecting...")
                    else ->
                        viewModel.showSnackbar("Wi-Fi '$ssid' configured!")
                }
            } else {
                viewModel.showSnackbar("Connecting to '$ssid'...")
            }
        } else {
            viewModel.showSnackbar("Wi-Fi connection cancelled")
        }
    }

    fun copyToClipboard(label: String, text: String, toastMsg: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        viewModel.showSnackbar(toastMsg)
    }

    fun openDialer(phone: String) {
        try {
            val cleanNumber = phone.replace(Regex("[^+0-9*#]"), "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.showSnackbar("Could not open dialer: ${e.message}")
        }
    }

    fun openEmailComposer(email: String, subject: String? = null, body: String? = null) {
        try {
            val mailtoBuilder = StringBuilder("mailto:${Uri.encode(email)}")
            val queryParams = mutableListOf<String>()
            if (!subject.isNullOrBlank()) queryParams.add("subject=${Uri.encode(subject)}")
            if (!body.isNullOrBlank()) queryParams.add("body=${Uri.encode(body)}")
            if (queryParams.isNotEmpty()) {
                mailtoBuilder.append("?").append(queryParams.joinToString("&"))
            }
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoBuilder.toString())).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.showSnackbar("Could not open email app: ${e.message}")
        }
    }

    fun openBrowser(url: String) {
        try {
            val fullUrl = if (url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
            ) {
                url
            } else {
                "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.showSnackbar("Could not open browser: ${e.message}")
        }
    }

    fun openAddToContacts() {
        try {
            val contact = parsed.contactInfo
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                contact?.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                contact?.organization?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                contact?.jobTitle?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
                val phone = parsed.primaryPhone
                if (!phone.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                val email = parsed.primaryEmail
                if (!email.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                contact?.addresses?.firstOrNull()?.let {
                    putExtra(ContactsContract.Intents.Insert.POSTAL, it)
                }
                val notesCombined = buildString {
                    contact?.urls?.firstOrNull()?.let { append("Website: ").append(it).append("\n") }
                    contact?.note?.let { append(it) }
                }.trim()
                if (notesCombined.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.NOTES, notesCombined)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.showSnackbar("Could not open Contacts: ${e.message}")
        }
    }

    fun shareContent() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, parsed.formattedCopyAll)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Code"))
        } catch (_: Exception) {}
    }

    val actionButtons = remember(parsed, rawContent) {
        buildContextualActionButtons(
            parsed = parsed,
            onWifiConnect = {
                val wifi = parsed.wifiInfo
                if (wifi != null) {
                    WifiConnector.connectToWifi(
                        context = context,
                        wifiInfo = wifi,
                        onLaunchAddNetworks = { intent ->
                            try {
                                wifiAddLauncher.launch(intent)
                            } catch (_: Exception) {
                                WifiConnector.copyPasswordAndOpenSettings(context, wifi) { msg ->
                                    viewModel.showSnackbar(msg)
                                }
                            }
                        },
                        onStatusMessage = { msg -> viewModel.showSnackbar(msg) }
                    )
                }
            },
            onCopyAll = {
                val msg = when (parsed.kind) {
                    QrPayloadKind.WIFI -> "All Wi-Fi details (Name, Security, Password) copied!"
                    QrPayloadKind.VCARD -> "All card details copied to clipboard!"
                    else -> "Copied to clipboard!"
                }
                copyToClipboard("Scanned Content", parsed.formattedCopyAll, msg)
            },
            onCopyWifiPasswordOnly = {
                val pass = parsed.wifiInfo?.password.orEmpty()
                if (pass.isNotEmpty()) {
                    copyToClipboard("Wi-Fi Password", pass, "Only Wi-Fi password copied!")
                } else {
                    viewModel.showSnackbar("This Wi-Fi network has no password")
                }
            },
            onDial = {
                val phones = parsed.contactInfo?.phones?.takeIf { it.isNotEmpty() }
                    ?: listOfNotNull(parsed.primaryPhone)
                if (phones.size > 1) {
                    pendingPickerTitle = "Select Phone Number to Dial"
                    pendingPickerItems = phones
                    pendingPickerOnSelect = { selected -> openDialer(selected) }
                } else if (phones.size == 1) {
                    openDialer(phones.first())
                }
            },
            onEmail = {
                val emails = parsed.contactInfo?.emails?.takeIf { it.isNotEmpty() }
                    ?: listOfNotNull(parsed.primaryEmail)
                if (emails.size > 1) {
                    pendingPickerTitle = "Select Email Address"
                    pendingPickerItems = emails
                    pendingPickerOnSelect = { selected ->
                        openEmailComposer(selected, parsed.emailSubject, parsed.emailBody)
                    }
                } else if (emails.size == 1) {
                    openEmailComposer(emails.first(), parsed.emailSubject, parsed.emailBody)
                }
            },
            onOpenUrl = {
                val urls = parsed.contactInfo?.urls?.takeIf { it.isNotEmpty() }
                    ?: listOfNotNull(parsed.primaryUrl)
                if (urls.size > 1) {
                    pendingPickerTitle = "Select Website Link"
                    pendingPickerItems = urls
                    pendingPickerOnSelect = { selected -> openBrowser(selected) }
                } else if (urls.size == 1) {
                    openBrowser(urls.first())
                }
            },
            onAddContact = { openAddToContacts() },
            onSendSms = {
                try {
                    val phone = parsed.primaryPhone.orEmpty()
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                        parsed.smsMessage?.let { putExtra("sms_body", it) }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(smsIntent)
                } catch (e: Exception) {
                    viewModel.showSnackbar("Could not open Messages: ${e.message}")
                }
            },
            onOpenMap = {
                try {
                    val uri = if (!parsed.geoLat.isNullOrBlank() && !parsed.geoLng.isNullOrBlank()) {
                        val query = parsed.geoQuery?.let { "(${Uri.encode(it)})" }.orEmpty()
                        Uri.parse("geo:${parsed.geoLat},${parsed.geoLng}?q=${parsed.geoLat},${parsed.geoLng}$query")
                    } else {
                        val addr = parsed.contactInfo?.addresses?.firstOrNull().orEmpty()
                        Uri.parse("geo:0,0?q=${Uri.encode(addr)}")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    viewModel.showSnackbar("Could not open Maps: ${e.message}")
                }
            },
            onAddCalendarEvent = {
                try {
                    val ev = parsed.calendarDetails
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        ev?.summary?.let { putExtra(CalendarContract.Events.TITLE, it) }
                        ev?.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                        ev?.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    viewModel.showSnackbar("Could not open Calendar: ${e.message}")
                }
            },
            onSearchWeb = {
                openBrowser("https://www.google.com/search?q=${Uri.encode(rawContent)}")
            },
            onShare = { shareContent() }
        )
    }

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
                .padding(innerPadding)
        ) {
            // 1. Top Bar: Back Button, Title, and Bookmark/Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OffWhite)
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable { viewModel.navigateTo(backTarget) }
                        .testTag("qr_result_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
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

                Box(
                    modifier = Modifier
                        .size(48.dp)
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

            // 2. Scrollable Body: Top Details (Serial by Serial) -> Side-by-Side Buttons -> Scanned QR Code Graphic
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                // Header Summary Strip
                val (badgeIcon, badgeTint, badgeBg) = getHeaderVisuals(parsed.kind)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeTint.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = parsed.badgeTitle,
                            tint = badgeTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeTint.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = parsed.badgeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = badgeTint
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = parsed.primaryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = item.timestamp,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Serial-by-Serial Structured Details Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = OffWhite,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_details_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Decoded Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        parsed.fields.forEachIndexed { index, field ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = field.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val displayValue = if (field.isSensitive && !isPasswordVisible) {
                                        "•".repeat(field.value.length.coerceIn(8, 16))
                                    } else {
                                        field.value
                                    }
                                    Text(
                                        text = displayValue,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (field.isSensitive && isPasswordVisible) FontFamily.Monospace else FontFamily.Default,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (field.isSensitive) {
                                        IconButton(
                                            onClick = { isPasswordVisible = !isPasswordVisible },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (field.copyValue.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(
                                                    field.label,
                                                    field.copyValue,
                                                    "${field.label} copied!"
                                                )
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy ${field.label}",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (index < parsed.fields.lastIndex) {
                                HorizontalDivider(
                                    color = SurfaceBorder,
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Contextual Side-by-Side Action Buttons (Home Categories Grid Style)
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                val rows = remember(actionButtons) {
                    if (actionButtons.size <= 4) {
                        listOf(actionButtons)
                    } else {
                        actionButtons.chunked(4)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_actions_grid"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { action ->
                                SideBySideActionTile(
                                    action = action,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keep alignment balanced if second row has fewer than 4 items
                            if (rows.size > 1 && rowItems.size < 4) {
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Scanned QR / Barcode Preview Card Directly Below Details & Action Buttons
                if (qrCodeBitmap != null) {
                    val is1D = QrBarcodeBitmapGenerator.is1DBarcode(item.barcodeFormat) ||
                        parsed.kind == QrPayloadKind.BARCODE
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanned_code_preview_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (is1D) "Scanned Barcode" else "Scanned QR Code",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Generated from the scanned code data above",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PureWhite)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrCodeBitmap.asImageBitmap(),
                                    contentDescription = if (is1D) "Scanned Barcode Graphic" else "Scanned QR Code Graphic",
                                    contentScale = ContentScale.Fit,
                                    modifier = if (is1D) {
                                        Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    } else {
                                        Modifier.size(180.dp)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 3. Bottom Bar: "Scan Another Code" Button
            Surface(
                color = PureWhite,
                border = BorderStroke(1.dp, SurfaceBorder),
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

    // Disambiguation Dialog if card has multiple phone numbers / emails / URLs
    if (pendingPickerTitle != null && pendingPickerItems.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingPickerTitle = null },
            containerColor = PureWhite,
            title = {
                Text(
                    text = pendingPickerTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pendingPickerItems.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OffWhite,
                            border = BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val callback = pendingPickerOnSelect
                                    pendingPickerTitle = null
                                    callback(option)
                                }
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingPickerTitle = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SideBySideActionTile(
    action: QrActionButtonModel,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(action.testTag)
            .clip(RoundedCornerShape(14.dp))
            .clickable { action.onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(action.bgColor)
                .border(1.dp, action.tintColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.tintColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = action.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildContextualActionButtons(
    parsed: ParsedQrPayload,
    onWifiConnect: () -> Unit,
    onCopyAll: () -> Unit,
    onCopyWifiPasswordOnly: () -> Unit,
    onDial: () -> Unit,
    onEmail: () -> Unit,
    onOpenUrl: () -> Unit,
    onAddContact: () -> Unit,
    onSendSms: () -> Unit,
    onOpenMap: () -> Unit,
    onAddCalendarEvent: () -> Unit,
    onSearchWeb: () -> Unit,
    onShare: () -> Unit
): List<QrActionButtonModel> {
    return when (parsed.kind) {
        QrPayloadKind.WIFI -> buildList {
            // Wi-Fi specific side-by-side buttons: Connect, Copy All, Copy Password, Share
            add(
                QrActionButtonModel(
                    id = "wifi_connect",
                    label = "Connect",
                    icon = Icons.Default.Wifi,
                    bgColor = Color(0xFFECFDF5),
                    tintColor = AccentGreen,
                    testTag = "qr_wifi_connect_button",
                    onClick = onWifiConnect
                )
            )
            add(
                QrActionButtonModel(
                    id = "wifi_copy_all",
                    label = "Copy",
                    icon = Icons.Default.ContentCopy,
                    bgColor = Color(0xFFEFF6FF),
                    tintColor = AccentBlue,
                    testTag = "qr_wifi_copy_all_button",
                    onClick = onCopyAll
                )
            )
            add(
                QrActionButtonModel(
                    id = "wifi_copy_pass",
                    label = "Copy Pass",
                    icon = Icons.Default.Key,
                    bgColor = Color(0xFFFFFBEB),
                    tintColor = Color(0xFFD97706),
                    testTag = "qr_wifi_copy_password_button",
                    onClick = onCopyWifiPasswordOnly
                )
            )
            add(
                QrActionButtonModel(
                    id = "wifi_share",
                    label = "Share",
                    icon = Icons.Default.Share,
                    bgColor = Color(0xFFF5F3FF),
                    tintColor = Color(0xFF7C3AED),
                    testTag = "qr_share_button",
                    onClick = onShare
                )
            )
        }

        QrPayloadKind.VCARD -> buildList {
            if (!parsed.primaryPhone.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "card_dial",
                        label = "Contact",
                        icon = Icons.Default.Call,
                        bgColor = Color(0xFFECFDF5),
                        tintColor = AccentGreen,
                        testTag = "qr_contact_dial_button",
                        onClick = onDial
                    )
                )
            }
            if (!parsed.primaryEmail.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "card_email",
                        label = "Email",
                        icon = Icons.Default.Email,
                        bgColor = Color(0xFFEFF6FF),
                        tintColor = AccentBlue,
                        testTag = "qr_email_button",
                        onClick = onEmail
                    )
                )
            }
            if (!parsed.primaryUrl.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "card_url",
                        label = "Website",
                        icon = Icons.Default.OpenInBrowser,
                        bgColor = Color(0xFFECFEFF),
                        tintColor = Color(0xFF0891B2),
                        testTag = "qr_open_url_button",
                        onClick = onOpenUrl
                    )
                )
            }
            add(
                QrActionButtonModel(
                    id = "card_add_contact",
                    label = "Add Contact",
                    icon = Icons.Default.PersonAdd,
                    bgColor = Color(0xFFF5F3FF),
                    tintColor = Color(0xFF7C3AED),
                    testTag = "qr_add_contact_button",
                    onClick = onAddContact
                )
            )
            if (parsed.contactInfo?.addresses?.isNotEmpty() == true) {
                add(
                    QrActionButtonModel(
                        id = "card_map",
                        label = "Location",
                        icon = Icons.Default.LocationOn,
                        bgColor = Color(0xFFFFFBEB),
                        tintColor = Color(0xFFD97706),
                        testTag = "qr_map_button",
                        onClick = onOpenMap
                    )
                )
            }
            add(
                QrActionButtonModel(
                    id = "card_copy",
                    label = "Copy",
                    icon = Icons.Default.ContentCopy,
                    bgColor = Color(0xFFF1F5F9),
                    tintColor = Color(0xFF475569),
                    testTag = "qr_copy_all_button",
                    onClick = onCopyAll
                )
            )
            add(
                QrActionButtonModel(
                    id = "card_share",
                    label = "Share",
                    icon = Icons.Default.Share,
                    bgColor = Color(0xFFFDF2F8),
                    tintColor = Color(0xFFDB2777),
                    testTag = "qr_share_button",
                    onClick = onShare
                )
            )
        }

        QrPayloadKind.URL -> listOf(
            QrActionButtonModel(
                id = "url_open",
                label = "Open Link",
                icon = Icons.Default.OpenInBrowser,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_open_url_button",
                onClick = onOpenUrl
            ),
            QrActionButtonModel(
                id = "url_copy",
                label = "Copy Link",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "url_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.EMAIL -> listOf(
            QrActionButtonModel(
                id = "email_send",
                label = "Email",
                icon = Icons.Default.Email,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_email_button",
                onClick = onEmail
            ),
            QrActionButtonModel(
                id = "email_copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "email_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.PHONE -> listOf(
            QrActionButtonModel(
                id = "phone_call",
                label = "Call",
                icon = Icons.Default.Call,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_contact_dial_button",
                onClick = onDial
            ),
            QrActionButtonModel(
                id = "phone_sms",
                label = "SMS",
                icon = Icons.Default.Sms,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_sms_button",
                onClick = onSendSms
            ),
            QrActionButtonModel(
                id = "phone_save",
                label = "Add Contact",
                icon = Icons.Default.PersonAdd,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_add_contact_button",
                onClick = onAddContact
            ),
            QrActionButtonModel(
                id = "phone_copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFF1F5F9),
                tintColor = Color(0xFF475569),
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            )
        )

        QrPayloadKind.SMS -> listOf(
            QrActionButtonModel(
                id = "sms_send",
                label = "Send SMS",
                icon = Icons.Default.Sms,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_sms_button",
                onClick = onSendSms
            ),
            QrActionButtonModel(
                id = "sms_call",
                label = "Call",
                icon = Icons.Default.Call,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_contact_dial_button",
                onClick = onDial
            ),
            QrActionButtonModel(
                id = "sms_copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFF1F5F9),
                tintColor = Color(0xFF475569),
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "sms_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.GEO -> listOf(
            QrActionButtonModel(
                id = "geo_map",
                label = "Open Map",
                icon = Icons.Default.LocationOn,
                bgColor = Color(0xFFFFFBEB),
                tintColor = Color(0xFFD97706),
                testTag = "qr_map_button",
                onClick = onOpenMap
            ),
            QrActionButtonModel(
                id = "geo_copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "geo_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.CALENDAR -> listOf(
            QrActionButtonModel(
                id = "cal_add",
                label = "Add Event",
                icon = Icons.Default.Event,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_calendar_button",
                onClick = onAddCalendarEvent
            ),
            QrActionButtonModel(
                id = "cal_copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "cal_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.BARCODE -> listOf(
            QrActionButtonModel(
                id = "barcode_search",
                label = "Search Web",
                icon = Icons.Default.Search,
                bgColor = Color(0xFFF5F3FF),
                tintColor = Color(0xFF7C3AED),
                testTag = "qr_search_web_button",
                onClick = onSearchWeb
            ),
            QrActionButtonModel(
                id = "barcode_copy",
                label = "Copy Code",
                icon = Icons.Default.ContentCopy,
                bgColor = Color(0xFFEFF6FF),
                tintColor = AccentBlue,
                testTag = "qr_copy_all_button",
                onClick = onCopyAll
            ),
            QrActionButtonModel(
                id = "barcode_share",
                label = "Share",
                icon = Icons.Default.Share,
                bgColor = Color(0xFFECFDF5),
                tintColor = AccentGreen,
                testTag = "qr_share_button",
                onClick = onShare
            )
        )

        QrPayloadKind.TEXT -> buildList {
            if (!parsed.primaryPhone.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "text_dial",
                        label = "Contact",
                        icon = Icons.Default.Call,
                        bgColor = Color(0xFFECFDF5),
                        tintColor = AccentGreen,
                        testTag = "qr_contact_dial_button",
                        onClick = onDial
                    )
                )
            }
            if (!parsed.primaryEmail.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "text_email",
                        label = "Email",
                        icon = Icons.Default.Email,
                        bgColor = Color(0xFFEFF6FF),
                        tintColor = AccentBlue,
                        testTag = "qr_email_button",
                        onClick = onEmail
                    )
                )
            }
            if (!parsed.primaryUrl.isNullOrBlank()) {
                add(
                    QrActionButtonModel(
                        id = "text_url",
                        label = "Open Link",
                        icon = Icons.Default.OpenInBrowser,
                        bgColor = Color(0xFFECFEFF),
                        tintColor = Color(0xFF0891B2),
                        testTag = "qr_open_url_button",
                        onClick = onOpenUrl
                    )
                )
            }
            add(
                QrActionButtonModel(
                    id = "text_copy",
                    label = "Copy",
                    icon = Icons.Default.ContentCopy,
                    bgColor = Color(0xFFEFF6FF),
                    tintColor = AccentBlue,
                    testTag = "qr_copy_all_button",
                    onClick = onCopyAll
                )
            )
            add(
                QrActionButtonModel(
                    id = "text_search",
                    label = "Search",
                    icon = Icons.Default.Search,
                    bgColor = Color(0xFFFFFBEB),
                    tintColor = Color(0xFFD97706),
                    testTag = "qr_search_web_button",
                    onClick = onSearchWeb
                )
            )
            add(
                QrActionButtonModel(
                    id = "text_share",
                    label = "Share",
                    icon = Icons.Default.Share,
                    bgColor = Color(0xFFF5F3FF),
                    tintColor = Color(0xFF7C3AED),
                    testTag = "qr_share_button",
                    onClick = onShare
                )
            )
        }
    }
}

private fun getHeaderVisuals(kind: QrPayloadKind): Triple<ImageVector, Color, Color> {
    return when (kind) {
        QrPayloadKind.WIFI -> Triple(Icons.Default.Wifi, AccentBlue, Color(0xFFEFF6FF))
        QrPayloadKind.VCARD -> Triple(Icons.Default.ContactPage, Color(0xFF7C3AED), Color(0xFFF5F3FF))
        QrPayloadKind.URL -> Triple(Icons.Default.OpenInBrowser, AccentGreen, Color(0xFFECFDF5))
        QrPayloadKind.EMAIL -> Triple(Icons.Default.Email, AccentBlue, Color(0xFFEFF6FF))
        QrPayloadKind.PHONE -> Triple(Icons.Default.Call, AccentGreen, Color(0xFFECFDF5))
        QrPayloadKind.SMS -> Triple(Icons.Default.Sms, Color(0xFF0891B2), Color(0xFFECFEFF))
        QrPayloadKind.GEO -> Triple(Icons.Default.LocationOn, Color(0xFFD97706), Color(0xFFFFFBEB))
        QrPayloadKind.CALENDAR -> Triple(Icons.Default.Event, Color(0xFF4F46E5), Color(0xFFEEF2FF))
        QrPayloadKind.BARCODE -> Triple(Icons.Default.QrCode2, Color(0xFF7C3AED), Color(0xFFF5F3FF))
        QrPayloadKind.TEXT -> Triple(Icons.Default.QrCode2, AccentBlue, Color(0xFFEFF6FF))
    }
}
