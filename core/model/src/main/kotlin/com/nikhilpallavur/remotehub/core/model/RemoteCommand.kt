package com.nikhilpallavur.remotehub.core.model

/**
 * A single action requested of a device, independent of how it is delivered. Discrete buttons
 * travel as [Press]; continuous appliance controls (temperature, brightness, colour, …) carry
 * their value inline. Adding a new kind of control means adding a new subtype here and teaching
 * the drivers that support it — existing drivers and UI are untouched (Open/Closed).
 */
sealed interface RemoteCommand {

    /** A discrete key press — the vocabulary shared by TVs, streamers, set-top boxes, etc. */
    data class Press(val key: RemoteKey) : RemoteCommand

    /** Free text typed into a focused field on the device (where supported). */
    data class TypeText(val text: String) : RemoteCommand

    /** A relative touchpad movement, in device-independent pixels. */
    data class Touch(val dx: Float, val dy: Float) : RemoteCommand

    /** Absolute target temperature in whole degrees Celsius (air conditioners, heaters). */
    data class SetTemperature(val celsius: Int) : RemoteCommand

    /** Fan speed selection (air conditioners, smart fans). */
    data class SetFanSpeed(val speed: FanSpeed) : RemoteCommand

    /** Toggle oscillation / louvre swing. */
    data class SetSwing(val enabled: Boolean) : RemoteCommand

    /** Operating mode selection (air conditioners). */
    data class SetMode(val mode: ClimateMode) : RemoteCommand

    /** Absolute brightness, 0..100 percent (lights, projectors, dimmable devices). */
    data class SetBrightness(val percent: Int) : RemoteCommand

    /** Absolute colour as packed ARGB (smart lights). */
    data class SetColor(val argb: Int) : RemoteCommand

    companion object {
        fun press(key: RemoteKey): RemoteCommand = Press(key)
    }
}

enum class FanSpeed { AUTO, LOW, MEDIUM, HIGH }

enum class ClimateMode { AUTO, COOL, HEAT, DRY, FAN }
