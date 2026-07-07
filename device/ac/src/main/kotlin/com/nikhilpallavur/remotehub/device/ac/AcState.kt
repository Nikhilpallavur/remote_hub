package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * The full state an air conditioner IR frame carries. Unlike NEC-style TV remotes, AC protocols
 * are stateful: every button press retransmits the complete settings block, so the connection
 * keeps one of these and re-encodes it wholesale on each command.
 *
 * Defaults must stay in sync with the UI's optimistic mirror (feature/remote `ClimateSettings`),
 * which assumes the same fresh baseline whenever a connection is (re)established.
 */
data class AcState(
    val power: Boolean = false,
    val mode: ClimateMode = ClimateMode.COOL,
    val temperatureC: Int = 24,
    val fan: FanSpeed = FanSpeed.AUTO,
    val swing: Boolean = false,
)
