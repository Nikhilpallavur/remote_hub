package com.nikhilpallavur.remotehub.core.drivers

import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for remembered devices and their credentials. The connection manager
 * writes through this the moment a device reports connected; the data layer supplies the
 * concrete, secure implementation (bound via Hilt in :core:data).
 */
interface PairedDeviceStore {
    val devices: Flow<List<RemoteDevice>>

    suspend fun upsert(device: RemoteDevice)

    suspend fun remove(id: String)

    suspend fun get(id: String): RemoteDevice?

    suspend fun setFavorite(id: String, favorite: Boolean)
}
