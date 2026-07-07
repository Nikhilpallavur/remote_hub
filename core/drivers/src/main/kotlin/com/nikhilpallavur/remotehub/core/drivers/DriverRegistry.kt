package com.nikhilpallavur.remotehub.core.drivers

import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single lookup point for every [DeviceDriver] contributed anywhere in the app. Hilt injects
 * the multibound set, so this class never names a concrete driver — adding one changes nothing here.
 */
@Singleton
class DriverRegistry @Inject constructor(
    private val drivers: Set<@JvmSuppressWildcards DeviceDriver>,
) {
    fun all(): List<DeviceDriver> = drivers.sortedBy { it.descriptor.displayName }

    fun descriptors(): List<DriverDescriptor> = all().map { it.descriptor }

    fun byId(id: String): DeviceDriver? = drivers.firstOrNull { it.descriptor.id == id }

    fun forCategory(category: DeviceCategory): List<DeviceDriver> =
        all().filter { it.descriptor.category == category }

    fun discoverable(): List<DeviceDriver> = all().filter { it.descriptor.discovery.isDiscoverable }

    /** Distinct categories that at least one registered driver can control, for the Home grid. */
    fun categories(): List<DeviceCategory> =
        drivers.map { it.descriptor.category }.distinct().sortedBy { it.ordinal }
}

/**
 * Declares the multibound driver set so Hilt is happy even before any driver module is on the
 * classpath (an empty set is valid). Each device module adds `@Binds @IntoSet` contributions.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DriverRegistryModule {
    @Multibinds
    fun drivers(): Set<DeviceDriver>
}
