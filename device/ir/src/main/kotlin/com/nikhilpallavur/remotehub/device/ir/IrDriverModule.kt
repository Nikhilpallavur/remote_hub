package com.nikhilpallavur.remotehub.device.ir

import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface IrDriverModule {
    @Binds
    @IntoSet
    fun bindIrTv(driver: IrTvDriver): DeviceDriver
}
