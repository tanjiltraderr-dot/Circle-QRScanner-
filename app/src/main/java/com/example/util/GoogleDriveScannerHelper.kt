package com.example.util

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private const val TAG = "GoogleDriveScanner"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

object GoogleDriveScannerHelper {

    fun buildScannerOptions(fullMode: Boolean = true): GmsDocumentScannerOptions {
        val builder = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(50)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
        if (fullMode) {
            builder.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        } else {
            builder.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
        }
        return builder.build()
    }

    fun startScan(
        activity: Activity,
        onIntentSenderReady: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val fullOptions = buildScannerOptions(fullMode = true)
            val fullScanner = GmsDocumentScanning.getClient(fullOptions)
            fullScanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    Log.d(TAG, "Google Drive Scanner intent sender ready (FULL mode)")
                    onIntentSenderReady(intentSender)
                }
                .addOnFailureListener { fullEx ->
                    Log.w(TAG, "Full scanner mode unavailable (${fullEx.message}), retrying BASE mode...", fullEx)
                    try {
                        val baseOptions = buildScannerOptions(fullMode = false)
                        val baseScanner = GmsDocumentScanning.getClient(baseOptions)
                        baseScanner.getStartScanIntent(activity)
                            .addOnSuccessListener { baseIntentSender ->
                                Log.d(TAG, "Google Drive Scanner intent sender ready (BASE mode)")
                                onIntentSenderReady(baseIntentSender)
                            }
                            .addOnFailureListener { baseEx ->
                                Log.e(TAG, "Base scanner mode failed: ${baseEx.message}", baseEx)
                                onError(baseEx)
                            }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "startScan exception: ${e.message}", e)
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
