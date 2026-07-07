package com.nikhilpallavur.remotehub

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nikhilpallavur.remotehub.core.designsystem.theme.RemoteHubTheme
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.core.transport.connection.RemoteConnectionManager
import com.nikhilpallavur.remotehub.navigation.RemoteHubNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Shared owner of the live connection, so the phone's physical keys can drive the TV. */
    @Inject
    lateinit var connectionManager: RemoteConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RemoteHubTheme {
                RemoteHubNavHost()
            }
        }
    }

    /**
     * When a volume-capable device is connected, the phone's side volume rocker (and mute key)
     * controls the TV instead of the phone: holding a volume key auto-repeats for a smooth ramp,
     * while mute toggles once per press. Any other key — or no active connection — falls straight
     * through to the system, so the phone's own volume works normally everywhere else.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val command = remoteVolumeCommand(keyCode)
        if (command != null && connectionManager.activeSupports(command)) {
            val isMuteRepeat = command.key == RemoteKey.MUTE && (event?.repeatCount ?: 0) > 0
            if (!isMuteRepeat) connectionManager.send(command)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Swallow the matching key-up so the system volume panel doesn't surface after we handled it. */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val command = remoteVolumeCommand(keyCode)
        if (command != null && connectionManager.activeSupports(command)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun remoteVolumeCommand(keyCode: Int): RemoteCommand.Press? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> RemoteCommand.Press(RemoteKey.VOLUME_UP)
        KeyEvent.KEYCODE_VOLUME_DOWN -> RemoteCommand.Press(RemoteKey.VOLUME_DOWN)
        KeyEvent.KEYCODE_VOLUME_MUTE -> RemoteCommand.Press(RemoteKey.MUTE)
        else -> null
    }
}
