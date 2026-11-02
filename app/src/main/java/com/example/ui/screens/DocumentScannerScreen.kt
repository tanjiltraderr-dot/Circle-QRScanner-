package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.CameraMode
import com.example.data.model.ScreenDestination
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ScanViewModel
import java.io.File

@Composable
fun DocumentScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var shutterClicked by remember { mutableStateOf(false) }

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

    // Photo picker for importing documents from gallery
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.triggerShutterCapture(photoUri = uri.toString())
        }
    }

    // References to active camera and ImageCapture use-case
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    // React to flashlight toggle
    LaunchedEffect(uiState.isDocFlashOn) {
        try {
            activeCamera?.cameraControl?.enableTorch(uiState.isDocFlashOn)
        } catch (_: Exception) {}
    }

    val shutterScale by animateFloatAsState(
        targetValue = if (shutterClicked) 0.86f else 1.0f,
        animationSpec = tween(120),
        label = "shutterScale",
        finishedListener = { shutterClicked = false }
    )

    // Viewfinder laser scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    // Bracket pulse animation
    val bracketAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracketAlpha"
    )

    BackHandler {
        viewModel.navigateTo(ScreenDestination.HOME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
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

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureUseCase = imageCapture

                            val cameraSelector = if (uiState.isFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                            activeCamera = boundCamera

                            boundCamera.cameraControl.enableTorch(uiState.isDocFlashOn)

                        } catch (exc: Exception) {
                            android.util.Log.e("DocScanner", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    // Update camera binding if camera flipped
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
                        tint = AccentBlue,
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
                    text = "Circle Scanner requires camera access to capture documents and receipts in high definition.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("request_camera_perm_doc_button")
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Camera Viewfinder with Document Alignment Guides & Corner Brackets
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            // Document frame bounds
            val left = canvasWidth * 0.10f
            val right = canvasWidth * 0.90f
            val top = canvasHeight * 0.15f
            val bottom = canvasHeight * 0.70f
            val bracketLength = 48f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Rule of thirds grid if enabled
                if (uiState.isGridVisible) {
                    val gridColor = Color.White.copy(alpha = 0.10f)
                    drawLine(gridColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), 1f)
                    drawLine(gridColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), 1f)
                    drawLine(gridColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), 1f)
                    drawLine(gridColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), 1f)
                }

                // Semi-transparent shaded outer mask outside document guide
                val maskColor = Color.Black.copy(alpha = 0.38f)
                drawRect(maskColor, Offset(0f, 0f), Size(size.width, top))
                drawRect(maskColor, Offset(0f, bottom), Size(size.width, size.height - bottom))
                drawRect(maskColor, Offset(0f, top), Size(left, bottom - top))
                drawRect(maskColor, Offset(right, top), Size(size.width - right, bottom - top))

                // Document Frame dashed boundary
                drawRect(
                    color = AccentBlue.copy(alpha = 0.35f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 2f)
                )

                // 4 Sharp Corner Brackets ⌜ ⌝ ⌞ ⌟
                val bracketColor = AccentBlue.copy(alpha = bracketAlpha)
                val bracketStroke = 6f

                // Top-Left ⌜
                drawLine(bracketColor, Offset(left - 2, top), Offset(left + bracketLength, top), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, top - 2), Offset(left, top + bracketLength), bracketStroke, StrokeCap.Round)

                // Top-Right ⌝
                drawLine(bracketColor, Offset(right + 2, top), Offset(right - bracketLength, top), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, top - 2), Offset(right, top + bracketLength), bracketStroke, StrokeCap.Round)

                // Bottom-Left ⌞
                drawLine(bracketColor, Offset(left - 2, bottom), Offset(left + bracketLength, bottom), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, bottom + 2), Offset(left, bottom - bracketLength), bracketStroke, StrokeCap.Round)

                // Bottom-Right ⌟
                drawLine(bracketColor, Offset(right + 2, bottom), Offset(right - bracketLength, bottom), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, bottom + 2), Offset(right, bottom - bracketLength), bracketStroke, StrokeCap.Round)

                // Animated Vertical Laser Scan Beam
                val laserY = top + (bottom - top) * laserProgress
                val laserBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AccentBlue.copy(alpha = 0.85f),
                        AccentBlue,
                        AccentBlue.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = right
                )
                drawLine(
                    brush = laserBrush,
                    start = Offset(left, laserY),
                    end = Offset(right, laserY),
                    strokeWidth = 3f
                )
            }

            // Center Guidance Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Text(
                        text = "Align document within frame",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // 3. Top Bar: Back, Page Count, Flashlight, Camera Switch, Grid Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("scanner_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mode / Page Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentBlue)
                    )
                    Text(
                        text = "Page ${uiState.docPageCount}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Right Action Controls: Flash, Camera Switch (Front/Back), Grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flashlight Toggle
                IconButton(
                    onClick = { viewModel.toggleDocFlash() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isDocFlashOn) AccentBlue else Color.Black.copy(alpha = 0.6f))
                        .testTag("scanner_flash_toggle")
                ) {
                    Icon(
                        imageVector = if (uiState.isDocFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Flashlight",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Camera Switch (Front / Back)
                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("scanner_switch_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Grid Toggle
                IconButton(
                    onClick = { viewModel.toggleDocGrid() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isGridVisible) AccentBlue.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.6f))
                        .testTag("scanner_grid_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Bottom Camera Controls: Mode Selector + Center Big Shutter + Gallery Import
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Selector Strip: Single, Batch, Text (OCR), ID Card, Book
                val modes = listOf(
                    CameraMode.SINGLE to "Single",
                    CameraMode.BATCH to "Batch",
                    CameraMode.TEXT_OCR to "Text (OCR)",
                    CameraMode.ID_CARD to "ID Card",
                    CameraMode.BOOK to "Book"
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    items(modes) { (mode, title) ->
                        val isSelected = uiState.cameraMode == mode
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AccentBlue else Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    if (mode == CameraMode.ID_CARD) {
                                        viewModel.navigateTo(ScreenDestination.ID_CARD_SCANNER)
                                    } else {
                                        viewModel.setCameraMode(mode)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("scanner_mode_${mode.name}")
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) PureWhite else Slate400
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Bar: [Gallery Import] - [Big Center Shutter] - [Batch Pages / Settings]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Batch / Multi-Page Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.showSnackbar("Batch mode: ${uiState.docPageCount} pages captured")
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pages",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            )
                            Text(
                                text = "${uiState.docPageCount - 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                            )
                        }
                    }

                    // Center: Big Shutter / Capture Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(shutterScale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable {
                                shutterClicked = true
                                val capture = imageCaptureUseCase
                                if (capture != null && hasCameraPermission) {
                                    val photoFile = File(
                                        context.cacheDir,
                                        "scan_doc_${System.currentTimeMillis()}.jpg"
                                    )
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    capture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                viewModel.triggerShutterCapture(photoUri = photoFile.absolutePath)
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Graceful fallback to processing simulation
                                                viewModel.triggerShutterCapture(photoUri = null)
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.triggerShutterCapture(photoUri = null)
                                }
                            }
                            .testTag("doc_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(3.dp, AccentBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Capture Document",
                                tint = AccentBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Right: Gallery Import Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("gallery_import_btn")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = "Import Gallery",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gallery",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Processing Overlay (Triggered instantly on Shutter tap)
        AnimatedVisibility(
            visible = uiState.isProcessingCapture,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = uiState.processingMessage,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 15.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Auto-detecting borders & straightening perspective...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
