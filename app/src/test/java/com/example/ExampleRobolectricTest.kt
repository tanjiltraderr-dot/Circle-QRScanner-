package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `cleanFileName sanitizes names and removes extension`() {
    assertEquals("Invoice_2026_Q4", com.example.util.PdfGenerator.cleanFileName("Invoice 2026/Q4.pdf"))
    assertEquals("Report_Final", com.example.util.PdfGenerator.cleanFileName("Report Final"))
    assertEquals("Contract_Draft", com.example.util.PdfGenerator.cleanFileName("Contract*Draft?.pdf"))
  }

  @Test
  fun `formatFileSize formats sizes appropriately`() {
    assertEquals("500 KB", com.example.util.PdfGenerator.formatFileSize(512000L))
    assertEquals("1.5 MB", com.example.util.PdfGenerator.formatFileSize(1572864L))
  }

  @Test
  fun `viewmodel handles multi page document capture and pdf dialog state`() {
    val viewModel = com.example.ui.viewmodel.ScanViewModel()
    assertEquals(1, viewModel.uiState.value.docPageCount)
    assertEquals(0, viewModel.uiState.value.capturedDocPages.size)

    viewModel.captureDocPage()
    assertEquals(2, viewModel.uiState.value.docPageCount)
    assertEquals(1, viewModel.uiState.value.capturedDocPages.size)
    assertEquals(1, viewModel.uiState.value.capturedPagesList.size)

    viewModel.captureDocPage()
    assertEquals(3, viewModel.uiState.value.docPageCount)
    assertEquals(2, viewModel.uiState.value.capturedDocPages.size)
    assertEquals(2, viewModel.uiState.value.capturedPagesList.size)

    viewModel.openPdfNameDialog()
    assertTrue("PDF dialog should be open", viewModel.uiState.value.showPdfNameDialog)

    viewModel.dismissPdfNameDialog()
    assertFalse("PDF dialog should be closed", viewModel.uiState.value.showPdfNameDialog)
  }

  @Test
  fun `gemini repository fallback instructions provide valid normalized boundaries`() {
    val geminiRepo = com.example.data.repository.GeminiRepository()
    val fallback = geminiRepo.getFallbackCropInstructions()

    assertEquals(4, fallback.corners.size)
    assertTrue("TopLeft X must be >= 0", fallback.topLeft.x >= 0.0f)
    assertTrue("TopLeft Y must be >= 0", fallback.topLeft.y >= 0.0f)
    assertTrue("TopRight X must be > TopLeft X", fallback.topRight.x > fallback.topLeft.x)
    assertTrue("BottomRight Y must be > TopRight Y", fallback.bottomRight.y > fallback.topRight.y)
    assertTrue("BottomLeft Y must be > TopLeft Y", fallback.bottomLeft.y > fallback.topLeft.y)
    assertEquals(0, fallback.rotationDegrees)
    assertTrue("Confidence should be positive", fallback.confidence > 0.8f)
  }

  @Test
  fun `qr camera and zoom state methods update appropriately`() {
    val viewModel = com.example.ui.viewmodel.ScanViewModel()
    assertFalse("Default camera should be rear", viewModel.uiState.value.isQrFrontCamera)

    viewModel.switchQrCamera()
    assertTrue("Camera should switch to front", viewModel.uiState.value.isQrFrontCamera)

    viewModel.switchQrCamera()
    assertFalse("Camera should switch back to rear", viewModel.uiState.value.isQrFrontCamera)

    assertEquals(1.0f, viewModel.uiState.value.qrZoomLevel, 0.01f)
    viewModel.adjustQrZoom(0.5f)
    assertEquals(1.5f, viewModel.uiState.value.qrZoomLevel, 0.01f)

    viewModel.adjustQrZoom(-1.0f)
    assertEquals(1.0f, viewModel.uiState.value.qrZoomLevel, 0.01f) // Should clamp at min 1.0f

    viewModel.setQrScanTargetMode(com.example.data.model.QrScanTargetMode.BARCODE)
    assertEquals(com.example.data.model.QrScanTargetMode.BARCODE, viewModel.uiState.value.qrScanTargetMode)
  }

  @Test
  fun `google drive scanner helper builds valid options`() {
    val options = com.example.util.GoogleDriveScannerHelper.buildScannerOptions()
    org.junit.Assert.assertNotNull(options)
  }

  @Test
  fun `initial scan history is completely empty without demo data`() {
    val repo = com.example.data.repository.ScanRepository()
    assertTrue("Initial scan history must be empty", repo.items.value.isEmpty())

    val categories = com.example.data.repository.ScanRepository.getInitialCategories()
    assertTrue("All categories should have null badges", categories.all { it.badge == null })
  }

  @Test
  fun `barcode detection transitions to full page QR_RESULT destination`() {
    val viewModel = com.example.ui.viewmodel.ScanViewModel()
    viewModel.onLiveBarcodeDetected("https://google.com", com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
    assertEquals(com.example.data.model.ScreenDestination.QR_RESULT, viewModel.uiState.value.currentDestination)
    org.junit.Assert.assertNotNull(viewModel.uiState.value.activeQrResult)
    assertEquals("https://google.com", viewModel.uiState.value.activeQrResult?.content)
    assertEquals(
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE,
        viewModel.uiState.value.activeQrResult?.barcodeFormat
    )
  }

  @Test
  fun `wifi qr parser extracts name security password in serial order`() {
    val wifiRaw = "WIFI:T:WPA;P:MySecret\\;Pass;S:Office_5G;;"
    val parsed = com.example.util.QrPayloadParser.parse(wifiRaw)

    assertEquals(com.example.util.QrPayloadKind.WIFI, parsed.kind)
    assertEquals(3, parsed.fields.size)
    assertEquals("Wi-Fi Name", parsed.fields[0].label)
    assertEquals("Office_5G", parsed.fields[0].value)
    assertEquals("Security", parsed.fields[1].label)
    assertEquals("WPA/WPA2", parsed.fields[1].value)
    assertEquals("Password", parsed.fields[2].label)
    assertEquals("MySecret;Pass", parsed.fields[2].value)
    assertEquals("MySecret;Pass", parsed.fields[2].copyValue)

    assertTrue(parsed.formattedCopyAll.contains("Wi-Fi Name: Office_5G"))
    assertTrue(parsed.formattedCopyAll.contains("Security: WPA/WPA2"))
    assertTrue(parsed.formattedCopyAll.contains("Password: MySecret;Pass"))
  }

  @Test
  fun `vcard visiting card parser extracts serial fields and dial email url targets`() {
    val vcardRaw = """
      BEGIN:VCARD
      VERSION:3.0
      FN:Tanvir Hasan
      ORG:Circle Tech Ltd
      TITLE:Product Lead
      TEL;TYPE=CELL:+8801711223344
      EMAIL:tanvir@circletech.com
      URL:https://circletech.com
      END:VCARD
    """.trimIndent()

    val parsed = com.example.util.QrPayloadParser.parse(vcardRaw)
    assertEquals(com.example.util.QrPayloadKind.VCARD, parsed.kind)
    assertEquals("Tanvir Hasan", parsed.primaryTitle)
    assertEquals("+8801711223344", parsed.primaryPhone)
    assertEquals("tanvir@circletech.com", parsed.primaryEmail)
    assertEquals("https://circletech.com", parsed.primaryUrl)
    assertTrue(parsed.fields.any { it.label == "Full Name" && it.value == "Tanvir Hasan" })
    assertTrue(parsed.fields.any { it.label == "Phone Number" && it.value == "+8801711223344" })
    assertTrue(parsed.fields.any { it.label == "Email Address" && it.value == "tanvir@circletech.com" })
  }

  @Test
  fun `plain text visiting card with phone and email is detected as card`() {
    val cardText = """
      Md. Karim
      Sales Manager
      Mobile: +8801812345678
      Email: karim@company.com
    """.trimIndent()

    val parsed = com.example.util.QrPayloadParser.parse(cardText)
    assertEquals(com.example.util.QrPayloadKind.VCARD, parsed.kind)
    assertEquals("+8801812345678", parsed.primaryPhone)
    assertEquals("karim@company.com", parsed.primaryEmail)
  }
}

