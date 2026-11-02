package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.QrScanTargetMode
import com.example.data.model.ScreenDestination
import com.example.ui.components.QrResultSheet
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LaserRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QrGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ScanViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        viewModel.navigateTo(ScreenDestination.HOME)
    }

    // Camera permission check and launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker for scanning QR / Barcode from gallery
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val first = barcodes.first()
                            viewModel.onLiveBarcodeDetected(
                                first.rawValue ?: "",
                                first.format
                            )
                        } else {
                            viewModel.showSnackbar("No barcode or QR code detected in selected image")
                        }
                    }
                    .addOnFailureListener {
                        viewModel.showSnackbar("Failed to read image from gallery")
                    }
            } catch (e: Exception) {
                viewModel.showSnackbar("Error processing gallery image: ${e.message}")
            }
        }
    }

    val isBarcodeMode = uiState.qrScanTargetMode == QrScanTargetMode.BARCODE

    // Laser scanning bar animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBarcodeMode) 1600 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    // Animated viewfinder dimensions according to Mode:
    // QR is square (~260dp), Barcode is wide horizontal rectangle (~310dp x 160dp)
    val animatedTargetWidth by animateDpAsState(
        targetValue = if (isBarcodeMode) 310.dp else 260.dp,
        animationSpec = spring(stiffness = 300f),
        label = "viewfinderWidth"
    )
    val animatedTargetHeight by animateDpAsState(
        targetValue = if (isBarcodeMode) 160.dp else 260.dp,
        animationSpec = spring(stiffness = 300f),
        label = "viewfinderHeight"
    )

    // Camera instance reference for controlling torch and zoom
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // React to flashlight toggle
    LaunchedEffect(uiState.isQrFlashOn) {
        try {
            activeCamera?.cameraControl?.enableTorch(uiState.isQrFlashOn)
        } catch (_: Exception) {}
    }

    // React to zoom changes
    LaunchedEffect(uiState.qrZoomLevel) {
        try {
            activeCamera?.cameraControl?.setZoomRatio(uiState.qrZoomLevel)
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newZoom = (uiState.qrZoomLevel * zoom).coerceIn(1.0f, 5.0f)
                    viewModel.setQrZoom(newZoom)
                }
            }
    ) {
        // 1. Live CameraX Preview View
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // ML Kit Barcode Analyzer
                            val barcodeFormats = if (isBarcodeMode) {
                                intArrayOf(
                                    Barcode.FORMAT_EAN_13,
                                    Barcode.FORMAT_EAN_8,
                                    Barcode.FORMAT_UPC_A,
                                    Barcode.FORMAT_UPC_E,
                                    Barcode.FORMAT_CODE_128,
                                    Barcode.FORMAT_CODE_39,
                                    Barcode.FORMAT_CODE_93,
                                    Barcode.FORMAT_ITF,
                                    Barcode.FORMAT_CODABAR,
                                    Barcode.FORMAT_PDF417,
                                    Barcode.FORMAT_AZTEC,
                                    Barcode.FORMAT_DATA_MATRIX
                                )
                            } else {
                                intArrayOf(Barcode.FORMAT_QR_CODE)
                            }

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    barcodeFormats.first(),
                                    *barcodeFormats.drop(1).toIntArray()
                                )
                                .build()

                            val scanner = BarcodeScanning.getClient(options)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !uiState.showQrResultSheet) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty() && !uiState.showQrResultSheet) {
                                                val detected = barcodes.first()
                                                val raw = detected.rawValue
                                                if (!raw.isNullOrBlank()) {
                                                    viewModel.onLiveBarcodeDetected(raw, detected.format)
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            activeCamera = boundCamera

                            // Sync initial zoom and torch
                            boundCamera.cameraControl.setZoomRatio(uiState.qrZoomLevel)
                            boundCamera.cameraControl.enableTorch(uiState.isQrFlashOn)

                        } catch (exc: Exception) {
                            android.util.Log.e("QrScanner", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    // Update camera when mode switches
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Request Fallback Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = QrGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Circle Scanner requires camera access to scan QR codes and barcodes in real-time.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = QrGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("request_camera_perm_button")
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Main Viewfinder Canvas with Cutout and Glowing Corners
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            val density = constraints.maxWidth / maxWidth.value
            val frameW = animatedTargetWidth.value * density
            val frameH = animatedTargetHeight.value * density

            val frameLeft = (width - frameW) / 2f
            val frameTop = (height - frameH) / 2f - 40f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dimmed surrounding overlay
                drawRect(
                    color = Color.Black.copy(alpha = if (uiState.isQrFlashOn) 0.38f else 0.65f),
                    size = size
                )

                // Transparent Cutout for target code
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(24f, 24f),
                    blendMode = BlendMode.Clear
                )

                // Target Corner Brackets
                val cornerLength = if (isBarcodeMode) 38f else 48f
                val strokeWidth = 8f
                val activeThemeColor = if (isBarcodeMode) LaserCyan else QrGreen

                // Top-Left corner
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft, frameTop + cornerLength),
                    end = Offset(frameLeft, frameTop),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft, frameTop),
                    end = Offset(frameLeft + cornerLength, frameTop),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Top-Right corner
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft + frameW - cornerLength, frameTop),
                    end = Offset(frameLeft + frameW, frameTop),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft + frameW, frameTop),
                    end = Offset(frameLeft + frameW, frameTop + cornerLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Bottom-Left corner
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft, frameTop + frameH - cornerLength),
                    end = Offset(frameLeft, frameTop + frameH),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft, frameTop + frameH),
                    end = Offset(frameLeft + cornerLength, frameTop + frameH),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Bottom-Right corner
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft + frameW - cornerLength, frameTop + frameH),
                    end = Offset(frameLeft + frameW, frameTop + frameH),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeThemeColor,
                    start = Offset(frameLeft + frameW, frameTop + frameH - cornerLength),
                    end = Offset(frameLeft + frameW, frameTop + frameH),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Barcode Alignment Guidelines if in Barcode Mode
                if (isBarcodeMode) {
                    val midY = frameTop + frameH / 2f
                    drawLine(
                        color = LaserRed.copy(alpha = 0.55f),
                        start = Offset(frameLeft + 20f, midY),
                        end = Offset(frameLeft + frameW - 20f, midY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // Animated Laser Line
                val laserY = frameTop + (frameH * laserProgress)
                val laserColor = if (isBarcodeMode) LaserRed else QrGreen

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            laserColor,
                            Color.White,
                            laserColor,
                            Color.Transparent
                        ),
                        startX = frameLeft,
                        endX = frameLeft + frameW
                    ),
                    start = Offset(frameLeft + 10f, laserY),
                    end = Offset(frameLeft + frameW - 10f, laserY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )

                // Laser glow gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            laserColor.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startY = laserY - 20f,
                        endY = laserY
                    ),
                    topLeft = Offset(frameLeft + 10f, laserY - 20f),
                    size = Size(frameW - 20f, 20f)
                )
            }
        }

        // 3. Top Navigation & Flashlight Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.navigateTo(ScreenDestination.HOME) }
                        .testTag("qr_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Mode Switcher Tabs ("QR Code" vs "Barcode")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code Mode Button
                    val isQrActive = uiState.qrScanTargetMode == QrScanTargetMode.QR_CODE
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isQrActive) QrGreen else Color.Transparent)
                            .clickable { viewModel.setQrScanTargetMode(QrScanTargetMode.QR_CODE) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("mode_qr_code"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = if (isQrActive) Slate900 else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "QR Code",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isQrActive) Slate900 else Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Barcode Mode Button
                    val isBarcodeActive = uiState.qrScanTargetMode == QrScanTargetMode.BARCODE
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isBarcodeActive) LaserCyan else Color.Transparent)
                            .clickable { viewModel.setQrScanTargetMode(QrScanTargetMode.BARCODE) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("mode_barcode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewStream,
                                contentDescription = null,
                                tint = if (isBarcodeActive) Slate900 else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Barcode",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isBarcodeActive) Slate900 else Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Right Actions: Flashlight Toggle On/Off & Gallery
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight Toggle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isQrFlashOn) QrGreen else Color.Black.copy(alpha = 0.55f))
                            .clickable { viewModel.toggleQrFlash() }
                            .testTag("qr_flash_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isQrFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flashlight Toggle",
                            tint = if (uiState.isQrFlashOn) Slate900 else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Import from gallery button (Active photo picker)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("qr_gallery_import"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Scan Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Subtitle Guidance & Quick Demo Triggers
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = if (isBarcodeMode) 170.dp else 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isBarcodeMode) {
                    "Point camera at barcode inside horizontal box"
                } else {
                    "Point camera at QR code to scan automatically"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Scan Preset Buttons (for instant testing)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isBarcodeMode) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.onLiveBarcodeDetected("8901030865421", Barcode.FORMAT_EAN_13)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.ViewStream, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(14.dp))
                            Text("EAN-13 Sample", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.onLiveBarcodeDetected("012345678905", Barcode.FORMAT_UPC_A)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.ViewStream, contentDescription = null, tint = LaserRed, modifier = Modifier.size(14.dp))
                            Text("UPC-A Sample", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.onLiveBarcodeDetected("https://circlescanner.app/preview/doc-3921", Barcode.FORMAT_QR_CODE)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, tint = QrGreen, modifier = Modifier.size(14.dp))
                            Text("Website URL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.onLiveBarcodeDetected("WIFI:S:CircleOffice_HQ;T:WPA;P:Security2026!;;", Barcode.FORMAT_QR_CODE)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(14.dp))
                            Text("Wi-Fi Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Bottom View: Ultra-Smooth Native-Style Zoom Controls
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFF111827).copy(alpha = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom Label & Live Readout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera Zoom (Pinch or Slide)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Text(
                        text = "${String.format("%.1f", uiState.qrZoomLevel)}x",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (isBarcodeMode) LaserCyan else QrGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Smooth Native-Style Continuous Slider with Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom Out Button (-)
                    IconButton(
                        onClick = { viewModel.adjustQrZoom(-0.25f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("qr_zoom_out")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Continuous Slider (1.0x to 5.0x, steps = 0 for butter-smooth native slide)
                    Slider(
                        value = uiState.qrZoomLevel,
                        onValueChange = { viewModel.setQrZoom(it) },
                        valueRange = 1.0f..5.0f,
                        steps = 0,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isBarcodeMode) LaserCyan else QrGreen,
                            activeTrackColor = if (isBarcodeMode) LaserCyan else QrGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_zoom_slider")
                    )

                    // Zoom In Button (+)
                    IconButton(
                        onClick = { viewModel.adjustQrZoom(0.25f) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("qr_zoom_in")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Zoom Presets: 1.0x, 2.0x, 3.0x, 5.0x
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(1.0f, 2.0f, 3.0f, 5.0f).forEach { zoomVal ->
                        val isSelected = kotlin.math.abs(uiState.qrZoomLevel - zoomVal) < 0.15f
                        val activeColor = if (isBarcodeMode) LaserCyan else QrGreen

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) activeColor else Color.White.copy(alpha = 0.08f))
                                .clickable { viewModel.setQrZoom(zoomVal) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${zoomVal.toInt()}x",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Slate900 else Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // QR & Barcode Result Bottom Sheet
    if (uiState.showQrResultSheet && uiState.activeQrResult != null) {
        QrResultSheet(
            item = uiState.activeQrResult!!,
            onDismiss = { viewModel.dismissQrResult() },
            onSaveToHistory = { viewModel.saveActiveQrResult() }
        )
    }
}
