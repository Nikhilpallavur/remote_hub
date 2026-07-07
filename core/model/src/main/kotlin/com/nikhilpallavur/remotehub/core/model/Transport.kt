package com.nikhilpallavur.remotehub.core.model

/**
 * How commands physically reach a device. The rest of the app is written against this abstraction
 * so a new medium (e.g. Thread, Zigbee bridge) is added here and in a driver, nowhere else.
 */
enum class Transport(val displayName: String) {
    WIFI("Wi-Fi"),
    INFRARED("Infrared"),
    BLUETOOTH("Bluetooth"),
}
