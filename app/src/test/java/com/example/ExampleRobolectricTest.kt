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
}

