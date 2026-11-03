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
}
