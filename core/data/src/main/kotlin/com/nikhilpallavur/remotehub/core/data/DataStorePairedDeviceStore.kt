package com.nikhilpallavur.remotehub.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nikhilpallavur.remotehub.core.drivers.PairedDeviceStore
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore by preferencesDataStore(name = "remotehub_devices")

/**
 * Persists remembered devices and their credentials in a private DataStore as a single JSON blob.
 * Reconnecting a known device is silent because its token/client-key survives here. Upsert keeps a
 * device's [RemoteDevice.favorite] flag so a routine reconnect never clears a user's favorite.
 */
@Singleton
class DataStorePairedDeviceStore @Inject constructor(
    @ApplicationContext context: Context,
) : PairedDeviceStore {

    private val dataStore: DataStore<Preferences> = context.deviceDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("devices_json")

    override val devices: Flow<List<RemoteDevice>> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { prefs -> decode(prefs[key]) }

    override suspend fun upsert(device: RemoteDevice) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            val existing = current.firstOrNull { it.id == device.id }
            val merged = device.copy(
                paired = true,
                favorite = device.favorite || (existing?.favorite ?: false),
            )
            prefs[key] = json.encodeToString(current.filterNot { it.id == device.id } + merged)
        }
    }

    override suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filterNot { it.id == id })
        }
    }

    override suspend fun get(id: String): RemoteDevice? =
        decode(dataStore.data.first()[key]).firstOrNull { it.id == id }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        dataStore.edit { prefs ->
            val updated = decode(prefs[key]).map { if (it.id == id) it.copy(favorite = favorite) else it }
            prefs[key] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<RemoteDevice> =
        raw?.let { runCatching { json.decodeFromString<List<RemoteDevice>>(it) }.getOrNull() }
            ?: emptyList()
}
