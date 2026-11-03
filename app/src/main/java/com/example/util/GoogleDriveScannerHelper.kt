package com.example.util

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private const val TAG = "GoogleDriveScanner"

object GoogleDriveScannerHelper {

    fun buildScannerOptions(): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    fun startScan(
        activity: Activity,
        onIntentSenderReady: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val options = buildScannerOptions()
        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                Log.d(TAG, "Google Drive Scanner intent sender ready")
                onIntentSenderReady(intentSender)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start Google Drive Scanner: ${e.message}", e)
                onError(e)
            }
    }

    fun parseScanResult(
        resultCode: Int,
        data: android.content.Intent?
    ): GmsDocumentScanningResult? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        return GmsDocumentScanningResult.fromActivityResultIntent(data)
    }
}
