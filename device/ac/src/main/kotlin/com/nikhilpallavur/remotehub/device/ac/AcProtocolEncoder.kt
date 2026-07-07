package com.nikhilpallavur.remotehub.device.ac

/**
 * Encodes a complete [AcState] into one brand family's infrared frame. One implementation per
 * protocol family (Coolix, LG, Samsung, Daikin); adding a brand means adding an encoder and a
 * thin driver, nothing else.
 *
 * Bit layouts, timings, and checksums follow the IRremoteESP8266 reference implementations
 * (ir_Coolix / ir_LG / ir_Samsung / ir_Daikin).
 */
interface AcProtocolEncoder {
    /** Human-readable protocol/brand family name, e.g. "LG". */
    val displayName: String

    /** Target temperatures the protocol can express, in whole degrees Celsius. */
    val temperatureRange: IntRange

    /** Builds the full IR frame for [state]; out-of-range temperatures are clamped. */
    fun encode(state: AcState): IrFrame

    /**
     * Some protocols (Coolix, LG) treat louvre swing as a dedicated toggle command instead of a
     * bit inside the state frame. When this returns non-null the connection transmits it for
     * swing changes; when null, swing is stateful and [encode] carries it.
     */
    fun swingToggleFrame(state: AcState): IrFrame? = null
}
