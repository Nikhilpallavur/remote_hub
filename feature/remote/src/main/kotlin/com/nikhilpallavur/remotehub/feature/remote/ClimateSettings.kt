package com.nikhilpallavur.remotehub.feature.remote

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * The UI's optimistic mirror of an air conditioner's state. Infrared is one-way — there is no
 * acknowledgement or readback — so this is *assumed* state, rebuilt purely from what the user
 * has tapped since connecting. It resets to these defaults on every new connection attempt.
 * The defaults must stay in sync with the fresh `AcState` a newly created IR AC connection
 * assumes; the two types are intentionally decoupled because the feature layer never depends on
 * a concrete device module.
 */
data class ClimateSettings(
    val power: Boolean = false,
    val mode: ClimateMode = ClimateMode.COOL,
    val temperatureC: Int = 24,
    val fan: FanSpeed = FanSpeed.AUTO,
    val swing: Boolean = false,
)

/** UI fallback when the connected driver doesn't declare its own temperature bounds. */
val DEFAULT_TEMPERATURE_RANGE_C = 16..30
