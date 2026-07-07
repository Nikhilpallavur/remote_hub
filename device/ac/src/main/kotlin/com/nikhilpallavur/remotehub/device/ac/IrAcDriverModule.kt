package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface IrAcDriverModule {

    @Binds
    @IntoSet
    fun bindCoolixAc(driver: CoolixAcDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindLgAc(driver: LgAcDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindSamsungAc(driver: SamsungAcDriver): DeviceDriver

    @Binds
    @IntoSet
    fun bindDaikinAc(driver: DaikinAcDriver): DeviceDriver
}
