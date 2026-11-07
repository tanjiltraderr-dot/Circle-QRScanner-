package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

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

    // Animated viewfinder dimensions: QR is square (260dp), Barcode is wide rectangle (310dp x 160dp)
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

    // Camera instance reference for controlling torch, flip, and zoom
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Function to bind or rebind camera
    val bindCamera: () -> Unit = {
        val provider = cameraProviderRef
        val pView = previewViewRef
        if (provider != null && pView != null && hasCameraPermission) {
            try {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }

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

                val cameraSelector = if (uiState.isQrFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                activeCamera = boundCamera

                boundCamera.cameraControl.setZoomRatio(uiState.qrZoomLevel)
                if (!uiState.isQrFrontCamera) {
                    boundCamera.cameraControl.enableTorch(uiState.isQrFlashOn)
                }
            } catch (exc: Exception) {
                android.util.Log.e("QrScanner", "Camera binding failed: ${exc.message}", exc)
            }
        }
    }

    // Rebind when switching front/rear camera or QR/Barcode target mode
    LaunchedEffect(uiState.isQrFrontCamera, isBarcodeMode, hasCameraPermission) {
        bindCamera()
    }

    // React to flashlight toggle
    LaunchedEffect(uiState.isQrFlashOn) {
        try {
            if (!uiState.isQrFrontCamera) {
                activeCamera?.cameraControl?.enableTorch(uiState.isQrFlashOn)
            }
        } catch (_: Exception) {}
    }

    // React to zoom level changes
    LaunchedEffect(uiState.qrZoomLevel) {
        try {
            activeCamera?.cameraControl?.setZoomRatio(uiState.qrZoomLevel)
        } catch (_: Exception) {}
    }

    // Elastic Center Slider state for smooth native drag with auto spring-back
    val dragOffsetAnim = remember { Animatable(0f) }
    var baseZoomAtDragStart by remember { mutableFloatStateOf(uiState.qrZoomLevel) }
    val maxDragPx = with(density) { 75.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .pointerInput(Unit) {
                // Support natural pinch-to-zoom on the camera feed
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
                    previewViewRef = previewView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            cameraProviderRef = cameraProviderFuture.get()
                            bindCamera()
                        } catch (exc: Exception) {
                            android.util.Log.e("QrScanner", "ProcessCameraProvider failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Fallback Screen
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

        // 2. Viewfinder Canvas with Cutout and Clean Targeting Corners (NO text around or inside!)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            val densityFactor = constraints.maxWidth / maxWidth.value
            val frameW = animatedTargetWidth.value * densityFactor
            val frameH = animatedTargetHeight.value * densityFactor

            val frameLeft = (width - frameW) / 2f
            val frameTop = (height - frameH) / 2f - 40f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dimmed surrounding overlay
                drawRect(
                    color = Color.Black.copy(alpha = if (uiState.isQrFlashOn) 0.35f else 0.65f),
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

                // Barcode Red Center Guide if in Barcode Mode
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

        // 3. Top Bar: Left Back Button, Right Camera Switch (Rear/Front) and Flashlight Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Back Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { viewModel.navigateTo(ScreenDestination.HOME) }
                    .testTag("qr_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Right Actions: Camera Flip (Front/Rear), Flashlight (On/Off), Gallery Import
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Switch (Front <-> Rear)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isQrFrontCamera) QrGreen else Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable { viewModel.switchQrCamera() }
                        .testTag("qr_camera_switch"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera Front or Back",
                        tint = if (uiState.isQrFrontCamera) Slate900 else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Flashlight Toggle (On / Off)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isQrFlashOn) QrGreen else Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable { viewModel.toggleQrFlash() }
                        .testTag("qr_flash_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isQrFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Flashlight Toggle",
                        tint = if (uiState.isQrFlashOn) Slate900 else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Gallery Import Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
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
                        contentDescription = "Scan Image from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. Bottom Controls: NO background card/surface underneath! Pure floating overlay!
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. Mode Switcher Tabs ("QR Code" vs "Barcode") positioned DIRECTLY ABOVE the zoom control
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // QR Code Tab
                val isQrActive = uiState.qrScanTargetMode == QrScanTargetMode.QR_CODE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isQrActive) QrGreen else Color.Transparent)
                        .clickable { viewModel.setQrScanTargetMode(QrScanTargetMode.QR_CODE) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .testTag("mode_qr_code"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = if (isQrActive) Slate900 else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "QR Code",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isQrActive) Slate900 else Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }

                // Barcode Tab
                val isBarcodeActive = uiState.qrScanTargetMode == QrScanTargetMode.BARCODE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isBarcodeActive) LaserCyan else Color.Transparent)
                        .clickable { viewModel.setQrScanTargetMode(QrScanTargetMode.BARCODE) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .testTag("mode_barcode"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ViewStream,
                            contentDescription = null,
                            tint = if (isBarcodeActive) Slate900 else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Barcode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isBarcodeActive) Slate900 else Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // B. Live Zoom Indicator Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${String.format("%.1f", uiState.qrZoomLevel)}x",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isBarcodeMode) LaserCyan else QrGreen,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // C. Native-style Zoom Control with Plus & Minus Circular Buttons, Center Line, and Auto-Recentering Knob!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Circle: Zoom Out (-)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { viewModel.adjustQrZoom(-0.5f) }
                        .testTag("qr_zoom_out_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Middle Track with Auto-Recentering Slider Handle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Track Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )

                    // Center Neutral Indicator Mark
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.65f))
                    )

                    // Auto-Recentering Draggable Knob (Drags Right -> Zoom In, Drags Left -> Zoom Out, Releases -> Springs to Center)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dragOffsetAnim.value.roundToInt(), 0) }
                            .size(38.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                if (dragOffsetAnim.value > 10f) {
                                    if (isBarcodeMode) LaserCyan else QrGreen
                                } else if (dragOffsetAnim.value < -10f) {
                                    Color(0xFFFFB74D)
                                } else {
                                    Color.White
                                }
                            )
                            .border(
                                2.dp,
                                Color.Black.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        baseZoomAtDragStart = uiState.qrZoomLevel
                                    },
                                    onDragEnd = {
                                        // Animate back to center position automatically
                                        coroutineScope.launch {
                                            dragOffsetAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            dragOffsetAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val newOffset = (dragOffsetAnim.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                                        coroutineScope.launch {
                                            dragOffsetAnim.snapTo(newOffset)
                                        }
                                        // Dynamic continuous zoom calculation:
                                        // Drag right -> zooms in up to 5x. Drag left -> zooms out down to 1x.
                                        val fraction = newOffset / maxDragPx
                                        val dynamicZoom = (baseZoomAtDragStart + fraction * 2.5f).coerceIn(1.0f, 5.0f)
                                        viewModel.setQrZoom(dynamicZoom)
                                    }
                                )
                            }
                            .testTag("qr_zoom_slider_knob"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle center grip indicator inside knob
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (kotlin.math.abs(dragOffsetAnim.value) > 10f) Slate900 else Color.Gray
                            )
                        )
                    }
                }

                // Right Circle: Zoom In (+)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { viewModel.adjustQrZoom(0.5f) }
                        .testTag("qr_zoom_in_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
