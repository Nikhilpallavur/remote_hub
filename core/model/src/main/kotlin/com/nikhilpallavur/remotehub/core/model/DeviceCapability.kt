package com.nikhilpallavur.remotehub.core.model

/**
 * Logical control groups a device can expose. The remote screen renders one section per
 * capability a device declares, so a driver's UI is generated from its capability set rather
 * than hand-built — a new device type gets a working remote purely by declaring its capabilities.
 */
enum class DeviceCapability {
    POWER,
    DPAD,
    NUMBER_PAD,
    VOLUME,
    CHANNEL,
    MEDIA,
    TEXT_INPUT,
    TOUCHPAD,
    APP_SHORTCUTS,
    INPUT_SOURCE,
    NAVIGATION,
    TEMPERATURE,
    FAN_SPEED,
    SWING,
    MODE,
    BRIGHTNESS,
    COLOR,
}
