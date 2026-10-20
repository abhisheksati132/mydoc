package com.abhisheksati.mydoc.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.abhisheksati.mydoc.MainActivity
import com.abhisheksati.mydoc.MyDocApp

class WifiTransferTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            val app = applicationContext as? MyDocApp
            val wifiManager = app?.wifiServerManager
            val isRunning = wifiManager?.isRunning?.value ?: false

            tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "PC Wi-Fi"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val url = wifiManager?.serverUrl?.value
                tile.subtitle = if (isRunning && url != null) {
                    url.substringAfter("://").substringBefore("?")
                } else {
                    "Transfer Files"
                }
            }
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? MyDocApp
        val wifiManager = app?.wifiServerManager
        val isRunning = wifiManager?.isRunning?.value ?: false

        if (isRunning) {
            wifiManager?.stopServer()
            updateTileState()
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_WIFI_TRANSFER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    102,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
