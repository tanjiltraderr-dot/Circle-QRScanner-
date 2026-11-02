package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Circle Scanner", appName)
  }

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
    org.junit.Assert.assertTrue("PDF dialog should be open", viewModel.uiState.value.showPdfNameDialog)

    viewModel.dismissPdfNameDialog()
    org.junit.Assert.assertFalse("PDF dialog should be closed", viewModel.uiState.value.showPdfNameDialog)
  }
}
