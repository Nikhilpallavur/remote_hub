package com.nikhilpallavur.remotehub.device.ac

/**
 * One ready-to-transmit infrared frame: the carrier frequency plus the alternating on/off
 * microsecond durations `ConsumerIrManager.transmit` expects (starting with an "on").
 *
 * Note: [pattern] is an array, so the generated equals/hashCode compare it by reference —
 * tests must assert on [pattern] contents directly, never on whole-frame equality.
 */
data class IrFrame(
    val carrierHz: Int,
    val pattern: IntArray,
)
