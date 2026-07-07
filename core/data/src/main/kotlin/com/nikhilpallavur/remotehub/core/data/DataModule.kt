package com.nikhilpallavur.remotehub.core.data

import com.nikhilpallavur.remotehub.core.drivers.PairedDeviceStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindPairedDeviceStore(impl: DataStorePairedDeviceStore): PairedDeviceStore
}
