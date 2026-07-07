package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import com.nikhilpallavur.remotehub.core.model.DeviceCategory

/** Maps a device category onto a recognisable Material icon for the Home grid and device rows. */
fun DeviceCategory.icon(): ImageVector = when (this) {
    DeviceCategory.TELEVISION -> Icons.Filled.Tv
    DeviceCategory.ANDROID_TV -> Icons.Filled.Tv
    DeviceCategory.STREAMING_DEVICE -> Icons.Filled.Cast
    DeviceCategory.SET_TOP_BOX -> Icons.Filled.Router
    DeviceCategory.PROJECTOR -> Icons.Filled.Cast
    DeviceCategory.AIR_CONDITIONER -> Icons.Filled.AcUnit
    DeviceCategory.SOUNDBAR -> Icons.Filled.Speaker
    DeviceCategory.SPEAKER -> Icons.Filled.Speaker
    DeviceCategory.HOME_THEATER -> Icons.Filled.Speaker
    DeviceCategory.MEDIA_PLAYER -> Icons.Filled.Album
    DeviceCategory.FAN -> Icons.Filled.Air
    DeviceCategory.LIGHT -> Icons.Filled.Lightbulb
    DeviceCategory.IOT -> Icons.Filled.DevicesOther
    DeviceCategory.OTHER -> Icons.Filled.DevicesOther
}
