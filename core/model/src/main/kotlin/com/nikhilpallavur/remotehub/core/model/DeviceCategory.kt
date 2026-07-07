package com.nikhilpallavur.remotehub.core.model

/**
 * The kind of appliance a device is, used to group the Home screen and pick an icon. Categories
 * are intentionally coarse — the concrete driver (see DriverDescriptor) carries the exact
 * vendor/protocol. New categories are added here without touching any existing device driver.
 */
enum class DeviceCategory(val displayName: String) {
    TELEVISION("TV"),
    ANDROID_TV("Android TV"),
    STREAMING_DEVICE("Streaming"),
    SET_TOP_BOX("Set-Top Box"),
    PROJECTOR("Projector"),
    AIR_CONDITIONER("Air Conditioner"),
    SOUNDBAR("Soundbar"),
    SPEAKER("Speaker"),
    HOME_THEATER("Home Theater"),
    MEDIA_PLAYER("Media Player"),
    FAN("Fan"),
    LIGHT("Light"),
    IOT("IoT Device"),
    OTHER("Other Device"),
}
