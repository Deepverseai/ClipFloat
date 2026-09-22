package io.antigravity.clipfloat.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ClipFloatTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.canDrawOverlays(this)) {
            // Cannot start without overlay permission
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
            return
        }

        val serviceIntent = Intent(this, FloatingHUDService::class.java)
        if (qsTile.state == Tile.STATE_ACTIVE) {
            stopService(serviceIntent)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (Settings.canDrawOverlays(this@ClipFloatTileService)) {
                Tile.STATE_INACTIVE
            } else {
                Tile.STATE_UNAVAILABLE
            }
            updateTile()
        }
    }
}
