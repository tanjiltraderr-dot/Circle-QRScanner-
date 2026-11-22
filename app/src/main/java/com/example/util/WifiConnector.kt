package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import java.util.Locale

object WifiConnector {

    fun connectToWifi(
        context: Context,
        wifiInfo: WifiDetails,
        onLaunchAddNetworks: (Intent) -> Unit,
        onStatusMessage: (String) -> Unit
    ) {
        val ssid = wifiInfo.ssid
        val password = wifiInfo.password
        val secUpper = wifiInfo.security.uppercase(Locale.US)
        val isOpen = secUpper == "NOPASS" || secUpper == "NONE" || secUpper == "OPEN" || password.isEmpty()
        val isWep = secUpper == "WEP"
        val isWpa3 = secUpper == "WPA3" || secUpper == "SAE"

        // Always make sure Wi-Fi system knows about it on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isWep) {
                // Android 10+ WifiNetworkSuggestion does not support legacy WEP
                copyPasswordAndOpenSettings(context, wifiInfo, onStatusMessage)
                return
            }

            try {
                val suggestionBuilder = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                if (wifiInfo.isHidden) {
                    suggestionBuilder.setIsHiddenSsid(true)
                }
                if (!isOpen) {
                    if (isWpa3) {
                        suggestionBuilder.setWpa3Passphrase(password)
                    } else {
                        suggestionBuilder.setWpa2Passphrase(password)
                    }
                }
                val suggestion = suggestionBuilder.build()

                // Also register suggestion with WifiManager for automatic connection
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                runCatching {
                    wifiManager?.addNetworkSuggestions(listOf(suggestion))
                }

                // Launch native Android 11+ system "Add & Connect to Wi-Fi" modal
                val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                    putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
                }
                onLaunchAddNetworks(intent)
                return
            } catch (e: Exception) {
                // Fallback to WifiNetworkSpecifier / Settings below
                copyPasswordAndOpenSettings(context, wifiInfo, onStatusMessage)
                return
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            if (isWep) {
                copyPasswordAndOpenSettings(context, wifiInfo, onStatusMessage)
                return
            }
            try {
                val suggestionBuilder = WifiNetworkSuggestion.Builder().setSsid(ssid)
                if (!isOpen) {
                    if (isWpa3) suggestionBuilder.setWpa3Passphrase(password)
                    else suggestionBuilder.setWpa2Passphrase(password)
                }
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiManager?.addNetworkSuggestions(listOf(suggestionBuilder.build()))

                val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
                if (!isOpen) {
                    if (isWpa3) specifierBuilder.setWpa3Passphrase(password)
                    else specifierBuilder.setWpa2Passphrase(password)
                }
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifierBuilder.build())
                    .build()

                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        onStatusMessage("Connected to $ssid")
                    }

                    override fun onUnavailable() {
                        onStatusMessage("Connection cancelled or unavailable")
                    }
                })
                onStatusMessage("Connecting to $ssid...")
                return
            } catch (_: Exception) {
                copyPasswordAndOpenSettings(context, wifiInfo, onStatusMessage)
                return
            }
        } else {
            // Legacy Android (< API 29)
            @Suppress("DEPRECATION")
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    if (!wifiManager.isWifiEnabled) {
                        wifiManager.isWifiEnabled = true
                    }
                    val conf = WifiConfiguration().apply {
                        SSID = "\"$ssid\""
                        hiddenSSID = wifiInfo.isHidden
                        when {
                            isOpen -> allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                            isWep -> {
                                wepKeys[0] = "\"$password\""
                                wepTxKeyIndex = 0
                                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                                allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                            }
                            else -> preSharedKey = "\"$password\""
                        }
                    }
                    val netId = wifiManager.addNetwork(conf)
                    if (netId != -1) {
                        wifiManager.disconnect()
                        wifiManager.enableNetwork(netId, true)
                        wifiManager.reconnect()
                        onStatusMessage("Connecting to $ssid automatically...")
                        return
                    }
                }
            } catch (_: Exception) {}
            copyPasswordAndOpenSettings(context, wifiInfo, onStatusMessage)
        }
    }

    fun copyPasswordAndOpenSettings(
        context: Context,
        wifiInfo: WifiDetails,
        onStatusMessage: (String) -> Unit
    ) {
        if (wifiInfo.password.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", wifiInfo.password))
            onStatusMessage("Password copied! Select '${wifiInfo.ssid}' in Wi-Fi settings.")
        } else {
            onStatusMessage("Select '${wifiInfo.ssid}' in Wi-Fi settings.")
        }
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
