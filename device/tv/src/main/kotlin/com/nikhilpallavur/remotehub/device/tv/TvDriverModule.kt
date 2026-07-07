package com.nikhilpallavur.remotehub.device.tv

import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Contributes every TV/streamer driver into the app-wide driver set. This module is the only place
 * these drivers are named — the registry, discovery, and UI find them purely through the set.
 */
@Module
@InstallIn(SingletonComponent::class)
interface TvDriverModule {
    @Binds
    @IntoSet
    fun bindAndroidTv(driver: AndroidTvDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindRoku(driver: RokuDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindSamsung(driver: SamsungDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindLgWebOs(driver: LgWebOsDriver): DeviceDriver
}
