package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenDestination
import com.example.ui.components.PdfNameAndCompileDialog
import com.example.ui.components.PdfSuccessBottomSheet
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSubtle
import com.example.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CropEditorScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Corner animation trigger (for auto snap animation)
    var isSnappedAnimationActive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Trigger smooth perspective straightening animation on entry
        delay(200)
        isSnappedAnimationActive = true
    }

    val backDestination = if (uiState.previousDestination == ScreenDestination.CROP_EDITOR) {
        ScreenDestination.HOME
    } else {
        uiState.previousDestination
    }

    BackHandler {
        viewModel.navigateTo(backDestination)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Sleek dark canvas background to highlight pure white document
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar: Back, Document Title, Auto-Crop Snap, Rotate, Save Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Retake
                IconButton(
                    onClick = { viewModel.navigateTo(backDestination) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("crop_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title & Page indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Crop & Enhance",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Page ${(uiState.docPageCount - 1).coerceAtLeast(1)} • Auto-Straightened",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Action buttons: Auto-Fit, Rotate, Save
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto-Straighten toggle
                    IconButton(
                        onClick = { viewModel.autoStraightenCorners() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("auto_straighten_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitScreen,
                            contentDescription = "Auto Straighten",
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Rotate 90° button
                    IconButton(
                        onClick = { viewModel.rotateEditor() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("rotate_doc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RotateRight,
                            contentDescription = "Rotate",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save / Next Button
                    Button(
                        onClick = { viewModel.openPdfNameDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("crop_save_button")
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. Middle Interactive Document Viewport & Quadrilateral Crop Editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Cropper Canvas
                DocumentCropperCanvas(
                    photoUri = uiState.activeCapturedPhotoUri,
                    corners = uiState.cropCorners,
                    rotationDegrees = uiState.activeEditorRotation,
                    filterApplied = uiState.activeEditorFilter,
                    signatureText = uiState.digitalSignature,
                    isMarkupEnabled = uiState.isMarkupEnabled,
                    onCornerDragged = { index, newOffset ->
                        viewModel.updateCropCorner(index, newOffset)
                    }
                )
            }

            // 3. Bottom Toolbar Container: Enhancement Filters + Tools Row
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 12.dp)
                ) {
                    // Filters Title & Filter Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color Filters",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "AI Enhanced",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Filter Chips: Original, Lighten, Enhance, Magic Color, No Shadow
                    val filterList = listOf("Original", "Lighten", "Enhance", "Magic Color", "No Shadow")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) {
                        items(filterList) { filterName ->
                            val isSelected = uiState.activeEditorFilter == filterName
                            Box(
                                modifier = Modifier
                                    .testTag("filter_chip_$filterName")
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AccentBlue else Slate100)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentBlue else SurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setEditorFilter(filterName) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = filterName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PureWhite else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Tools Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SurfaceBorder)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Main Action Tools: Extract Text (OCR), Markup, Sign, Rotate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tool 1: Extract Text (OCR)
                        EditorToolButton(
                            title = "Extract Text",
                            subtitle = "OCR",
                            icon = Icons.Filled.TextFields,
                            isActive = false,
                            onClick = { viewModel.showEditorOcrModal(true) },
                            testTag = "editor_tool_ocr"
                        )

                        // Tool 2: Markup
                        EditorToolButton(
                            title = "Markup",
                            subtitle = if (uiState.isMarkupEnabled) "Pen ON" else "Pen",
                            icon = Icons.Filled.Draw,
                            isActive = uiState.isMarkupEnabled,
                            onClick = { viewModel.toggleMarkup() },
                            testTag = "editor_tool_markup"
                        )

                        // Tool 3: Sign (Signature Stamp)
                        EditorToolButton(
                            title = "Sign",
                            subtitle = if (uiState.digitalSignature != null) "Signed" else "Add Stamp",
                            icon = Icons.Filled.Edit,
                            isActive = uiState.digitalSignature != null,
                            onClick = { viewModel.showSignatureDialog(true) },
                            testTag = "editor_tool_sign"
                        )

                        // Tool 4: Rotate
                        EditorToolButton(
                            title = "Rotate",
                            subtitle = "${uiState.activeEditorRotation}°",
                            icon = Icons.Filled.RotateRight,
                            isActive = false,
                            onClick = { viewModel.rotateEditor() },
                            testTag = "editor_tool_rotate"
                        )
                    }
                }
            }
        }

        // Signature Dialog
        if (uiState.showSignatureDialog) {
            SignatureEntryDialog(
                currentSignature = uiState.digitalSignature,
                onDismiss = { viewModel.showSignatureDialog(false) },
                onApply = { viewModel.setDigitalSignature(it) },
                onClear = { viewModel.setDigitalSignature(null) }
            )
        }

        // OCR Preview Modal
        if (uiState.showEditorOcrModal) {
            EditorOcrSheet(
                pageNumber = uiState.docPageCount - 1,
                onDismiss = { viewModel.showEditorOcrModal(false) },
                onCopy = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    viewModel.showSnackbar("Extracted text copied to clipboard!")
                    viewModel.showEditorOcrModal(false)
                }
            )
        }

        // PDF Name & Compilation Dialog
        if (uiState.showPdfNameDialog) {
            val defaultName = "CircleScan_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}"
            val effectivePageCount = uiState.capturedPagesList.size.coerceAtLeast(uiState.capturedDocPages.size).coerceAtLeast(1)
            PdfNameAndCompileDialog(
                initialFileName = defaultName,
                pageCount = effectivePageCount,
                filterApplied = uiState.activeEditorFilter,
                isCompiling = uiState.isCompilingPdf,
                onDismiss = { viewModel.dismissPdfNameDialog() },
                onCompilePdf = { name, isLetterSize, watermark ->
                    viewModel.compileCapturedPagesToPdf(
                        context = context,
                        fileName = name,
                        isLetterSize = isLetterSize,
                        watermark = watermark
                    )
                }
            )
        }

        // PDF Success Bottom Sheet
        if (uiState.showPdfSuccessSheet && uiState.pdfCompilationResult != null) {
            PdfSuccessBottomSheet(
                result = uiState.pdfCompilationResult!!,
                onDismiss = { viewModel.dismissPdfSuccessSheet() },
                onViewInLibrary = {
                    viewModel.dismissPdfSuccessSheet()
                    viewModel.navigateTo(ScreenDestination.DOCUMENTS)
                }
            )
        }
    }
}

@Composable
private fun DocumentCropperCanvas(
    photoUri: String? = null,
    corners: List<Offset>,
    rotationDegrees: Int,
    filterApplied: String,
    signatureText: String?,
    isMarkupEnabled: Boolean,
    onCornerDragged: (Int, Offset) -> Unit
) {
    val markupStrokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val density = LocalDensity.current.density

        // Paper aspect ratio container (approx A4 1:1.41)
        val paperWidth = (canvasWidth * 0.88f)
        val paperHeight = (canvasHeight * 0.90f)

        // Calculate absolute corner pixel positions
        val p0 = Offset(corners[0].x * canvasWidth, corners[0].y * canvasHeight)
        val p1 = Offset(corners[1].x * canvasWidth, corners[1].y * canvasHeight)
        val p2 = Offset(corners[2].x * canvasWidth, corners[2].y * canvasHeight)
        val p3 = Offset(corners[3].x * canvasWidth, corners[3].y * canvasHeight)

        // Document Paper Canvas with applied rotation and realistic document texture
        Box(
            modifier = Modifier
                .size((paperWidth / density).dp, (paperHeight / density).dp)
                .rotate(rotationDegrees.toFloat())
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (filterApplied) {
                        "Lighten" -> Color(0xFFFAFAFA)
                        "Enhance" -> Color(0xFFF1F5F9)
                        "Magic Color" -> PureWhite
                        "No Shadow" -> Color(0xFFFCFCFC)
                        else -> Color(0xFFFBF8F1) // Natural paper tone
                    }
                )
                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                .pointerInput(isMarkupEnabled) {
                    if (isMarkupEnabled) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                currentStroke = listOf(startOffset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    markupStrokes.add(currentStroke)
                                    currentStroke = emptyList()
                                }
                            }
                        )
                    }
                }
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Captured Document",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Simulated high-clarity document layout lines & header
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header Bar simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .background(if (filterApplied == "Magic Color") AccentBlue else Slate700, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(8.dp)
                            .background(Slate400.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Heading lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .background(Slate900, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(10.dp)
                        .background(Slate400, RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Simulated body paragraph lines
                repeat(7) { i ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (i % 3 == 0) 0.95f else if (i % 2 == 0) 0.88f else 0.72f)
                            .height(7.dp)
                            .background(
                                if (filterApplied == "Magic Color") Slate900 else Color(0xFF334155),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Table / Box simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Slate100, RoundedCornerShape(6.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).background(Slate400, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(6.dp).background(Slate400.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer & Signature Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Box(modifier = Modifier.width(60.dp).height(6.dp).background(Slate400, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OFFICIAL DOCUMENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        )
                    }

                    // Render Digital Signature Stamp if added
                    if (signatureText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentBlue.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = signatureText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Digitally Signed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

            // Render Digital Signature Stamp over photo if photo is present
            if (photoUri != null && signatureText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentBlue.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = signatureText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "Digitally Signed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }


            // Canvas for freehand markup pen strokes
            Canvas(modifier = Modifier.fillMaxSize()) {
                markupStrokes.forEach { stroke ->
                    for (i in 0 until stroke.size - 1) {
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = stroke[i],
                            end = stroke[i + 1],
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                for (i in 0 until currentStroke.size - 1) {
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = currentStroke[i],
                        end = currentStroke[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Perspective Crop Polygon Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val quadPath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Shaded mask outside quad
            drawPath(
                path = quadPath,
                color = AccentBlue.copy(alpha = 0.08f)
            )

            // Dotted crop outline
            drawPath(
                path = quadPath,
                color = AccentBlue,
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            // Draw corner anchor circles
            listOf(p0, p1, p2, p3).forEach { point ->
                drawCircle(color = AccentBlue, radius = 18f, center = point)
                drawCircle(color = PureWhite, radius = 9f, center = point)
            }

            // Draw mid-point indicators along the edges
            val midTop = (p0 + p1) / 2f
            val midRight = (p1 + p2) / 2f
            val midBottom = (p2 + p3) / 2f
            val midLeft = (p3 + p0) / 2f
            listOf(midTop, midRight, midBottom, midLeft).forEach { mid ->
                drawCircle(color = PureWhite, radius = 10f, center = mid)
                drawCircle(color = AccentBlue, radius = 6f, center = mid)
            }
        }

        // 4 Interactive Draggable Touch Targets at corners
        corners.forEachIndexed { index, corner ->
            val cornerX = corner.x * canvasWidth
            val cornerY = corner.y * canvasHeight

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.TopStart)
                    .offset(
                        x = (cornerX / density - 27).dp,
                        y = (cornerY / density - 27).dp
                    )
                    .testTag("crop_handle_$index")
                    .pointerInput(index) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val current = corners[index]
                            val newX = current.x + (dragAmount.x / canvasWidth)
                            val newY = current.y + (dragAmount.y / canvasHeight)
                            onCornerDragged(index, Offset(newX, newY))
                        }
                    }
            )
        }
    }
}

@Composable
private fun EditorToolButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isActive) AccentBlue else Slate100)
                .border(1.dp, if (isActive) AccentBlue else SurfaceBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) PureWhite else Slate700,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isActive) AccentBlue else TextPrimary
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                color = TextMuted
            )
        )
    }
}

@Composable
private fun SignatureEntryDialog(
    currentSignature: String?,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    onClear: () -> Unit
) {
    var signatureInput by remember { mutableStateOf(currentSignature ?: "Dr. Eleanor Vance") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Digital Signature",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Type your name or signatory title to stamp on the document:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = signatureInput,
                    onValueChange = { signatureInput = it },
                    placeholder = { Text("Signatory Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("signature_text_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Signature Preview",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = signatureInput.ifBlank { "Sign Here" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (signatureInput.isNotBlank()) {
                        onApply(signatureInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.testTag("apply_signature_button")
            ) {
                Text("Apply Stamp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentSignature != null) {
                    TextButton(onClick = onClear) {
                        Text("Remove", color = Color(0xFFEF4444))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        }
    )
}

@Composable
private fun EditorOcrSheet(
    pageNumber: Int,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val sampleExtractedText = """
INVOICE / AGREEMENT
Document Reference: CS-2026-0994
Date: ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}

Terms of Service & Execution Statement:
1. Deliverables provided under high-clarity digital scan validation.
2. Verified with optical character recognition with 99.4% confidence.
3. Timestamp logged and sealed in accordance with mobile export standard.
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TextFields,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extracted Text (Page $pageNumber)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "AI OCR text extracted from document bounds:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sampleExtractedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCopy(sampleExtractedText) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.testTag("copy_ocr_button")
            ) {
                Text("Copy All Text", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TextMuted)
            }
        }
    )
}
