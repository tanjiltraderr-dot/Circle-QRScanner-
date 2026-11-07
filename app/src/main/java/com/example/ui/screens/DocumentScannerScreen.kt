package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DocumentScanner
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QrGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.ScanViewModel
import com.example.util.GoogleDriveScannerHelper
import com.example.util.findActivity
import java.io.File
import java.util.concurrent.Executors

@Composable
fun DocumentScannerScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
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

    // Photo picker for importing documents from gallery -> takes to Crop/Editor screen
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.triggerShutterCapture(photoUri = uri.toString(), context = context)
        }
    }

    // Google Drive Document Scanner Launcher -> passes scanned image directly to Crop/Editor screen
    val googleDriveScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val gmsResult = GoogleDriveScannerHelper.parseScanResult(
                activityResult.resultCode,
                activityResult.data
            )
            if (gmsResult != null) {
                val firstUri = gmsResult.pages?.firstOrNull()?.imageUri?.toString()
                    ?: gmsResult.pdf?.uri?.toString()
                if (firstUri != null) {
                    viewModel.openScannedImageInCropEditor(
                        imageUri = firstUri,
                        context = context,
                        pdfUri = gmsResult.pdf?.uri?.toString(),
                        pageCount = gmsResult.pdf?.pageCount ?: 1
                    )
                } else {
                    viewModel.handleGoogleDriveScanResult(
                        context = context,
                        pdfUri = gmsResult.pdf?.uri,
                        pageUris = gmsResult.pages?.map { it.imageUri },
                        pageCount = gmsResult.pdf?.pageCount ?: 1
                    )
                }
            }
        }
    }

    // References to active camera and ImageCapture use-case
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

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
                    // Rebind camera on front/back toggle
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
                        tint = AccentBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Access Needed",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Circle Scanner requires camera access to capture documents with edge detection and filters.",
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
                    modifier = Modifier.testTag("request_doc_camera_perm_button")
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Viewfinder Targeting Frame with Corner Alignment Brackets
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            val frameW = width * 0.85f
            val frameH = height * 0.60f
            val frameLeft = (width - frameW) / 2f
            val frameTop = (height - frameH) / 2f - 30f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Subtle Dimmed vignette around document bounds
                drawRect(
                    color = Color.Black.copy(alpha = if (uiState.isDocFlashOn) 0.28f else 0.42f),
                    size = size
                )

                // Clear document framing box
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameW, frameH),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // 4 Sharp Corner Brackets
                val bracketLen = 42f
                val strokeW = 6f
                val bracketColor = Color.White

                // Top-Left
                drawLine(bracketColor, Offset(frameLeft, frameTop + bracketLen), Offset(frameLeft, frameTop), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(frameLeft, frameTop), Offset(frameLeft + bracketLen, frameTop), strokeW, StrokeCap.Round)

                // Top-Right
                drawLine(bracketColor, Offset(frameLeft + frameW - bracketLen, frameTop), Offset(frameLeft + frameW, frameTop), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(frameLeft + frameW, frameTop), Offset(frameLeft + frameW, frameTop + bracketLen), strokeW, StrokeCap.Round)

                // Bottom-Left
                drawLine(bracketColor, Offset(frameLeft, frameTop + frameH - bracketLen), Offset(frameLeft, frameTop + frameH), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(frameLeft, frameTop + frameH), Offset(frameLeft + bracketLen, frameTop + frameH), strokeW, StrokeCap.Round)

                // Bottom-Right
                drawLine(bracketColor, Offset(frameLeft + frameW - bracketLen, frameTop + frameH), Offset(frameLeft + frameW, frameTop + frameH), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(frameLeft + frameW, frameTop + frameH - bracketLen), Offset(frameLeft + frameW, frameTop + frameH), strokeW, StrokeCap.Round)

                // Animated Laser Line
                val laserY = frameTop + (frameH * laserProgress)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AccentBlue.copy(alpha = 0.8f),
                            Color.White,
                            AccentBlue.copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startX = frameLeft,
                        endX = frameLeft + frameW
                    ),
                    start = Offset(frameLeft + 15f, laserY),
                    end = Offset(frameLeft + frameW - 15f, laserY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Top Action Bar: Back Button, Flashlight Toggle, Camera Switch (Rear/Front), and Google Drive Scan Trigger
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.navigateTo(ScreenDestination.HOME) }
                        .testTag("doc_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Google Drive Scanner Quick-Launch Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable {
                            if (activity != null) {
                                GoogleDriveScannerHelper.startScan(
                                    activity = activity,
                                    onIntentSenderReady = { intentSender ->
                                        try {
                                            googleDriveScannerLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        } catch (e: Exception) {
                                            viewModel.showSnackbar("Could not open Google Drive Scanner: ${e.message}")
                                        }
                                    },
                                    onError = { exc ->
                                        viewModel.showSnackbar("Google Drive Scanner: ${exc.message ?: "Google Play Services needed"}")
                                    }
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("quick_google_drive_scan_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Drive Scanner",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Right Actions: Flashlight Toggle and Camera Flip
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Flashlight
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isDocFlashOn) AccentBlue else Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.toggleDocFlash() }
                        .testTag("doc_flash_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isDocFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flashlight Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Camera Switch (Front <-> Rear)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { viewModel.switchCamera() }
                            .testTag("doc_camera_switch"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = "Switch Camera Front or Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Bottom Controls: Mode Selector, Big Shutter Capture Button, and Gallery Import Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector Tabs (Single, Batch, ID Card)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    CameraMode.SINGLE to "Single",
                    CameraMode.BATCH to "Batch",
                    CameraMode.ID_CARD to "ID Card"
                ).forEach { (mode, label) ->
                    val isSelected = uiState.cameraMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AccentBlue else Color.Transparent)
                            .clickable { viewModel.setCameraMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PureWhite else Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Shutter Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Import Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("doc_gallery_import"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Import from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Big Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(shutterScale)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .clickable {
                            shutterClicked = true
                            val imageCapture = imageCaptureUseCase
                            if (imageCapture != null) {
                                val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                imageCapture.takePicture(
                                    outputOptions,
                                    cameraExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            viewModel.triggerShutterCapture(
                                                photoUri = photoFile.absolutePath,
                                                context = context
                                            )
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.showSnackbar("Capture failed: ${exception.message}")
                                            // Fallback simulated photo
                                            viewModel.triggerShutterCapture(photoUri = null, context = context)
                                        }
                                    }
                                )
                            } else {
                                viewModel.triggerShutterCapture(photoUri = null, context = context)
                            }
                        }
                        .testTag("doc_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentBlue)
                    )
                }

                // Grid Guide Toggle Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isGridVisible) AccentBlue.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { viewModel.toggleDocGrid() }
                        .testTag("doc_grid_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridOn,
                        contentDescription = "Toggle Grid Alignment",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 5. Processing Document Loading Overlay (Auto-triggers on capture -> seamlessly navigates to Crop/Editor)
        AnimatedVisibility(
            visible = uiState.isProcessingCapture,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = AccentBlue,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = uiState.processingMessage,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 15.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Detecting boundaries & preparing Editor...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
