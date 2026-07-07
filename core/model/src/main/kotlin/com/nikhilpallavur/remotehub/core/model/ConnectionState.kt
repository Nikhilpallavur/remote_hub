package com.nikhilpallavur.remotehub.core.model

/** How a user authorizes a pairing-capable device. */
enum class PairingMode {
    /** Device shows a PIN the user types back into the app (e.g. Android TV). */
    PIN_CODE,

    /** Device shows an allow/deny prompt the user confirms on the device (e.g. Samsung, LG). */
    CONFIRM_ON_DEVICE,
}

/** The lifecycle of a single active connection, surfaced to the ViewModel/UI. */
sealed interface ConnectionState {
    data object Idle : ConnectionState

    data class Connecting(val device: RemoteDevice) : ConnectionState

    data class AwaitingPairing(
        val device: RemoteDevice,
        val mode: PairingMode,
    ) : ConnectionState

    data class Connected(val device: RemoteDevice) : ConnectionState

    data class Failed(
        val device: RemoteDevice?,
        val reason: String,
    ) : ConnectionState
}
