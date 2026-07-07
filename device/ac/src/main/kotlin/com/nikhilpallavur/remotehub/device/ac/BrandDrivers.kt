package com.nikhilpallavur.remotehub.device.ac

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoolixAcDriver @Inject constructor(@ApplicationContext context: Context) : IrAcDriver(
    context = context,
    encoder = CoolixEncoder,
    driverId = "ir-ac-coolix",
    brandDisplayName = "AC — Voltas / Midea / Blue Star",
    manufacturer = "Voltas / Midea / Blue Star",
)

@Singleton
class LgAcDriver @Inject constructor(@ApplicationContext context: Context) : IrAcDriver(
    context = context,
    encoder = LgAcEncoder,
    driverId = "ir-ac-lg",
    brandDisplayName = "AC — LG",
    manufacturer = "LG",
)

@Singleton
class SamsungAcDriver @Inject constructor(@ApplicationContext context: Context) : IrAcDriver(
    context = context,
    encoder = SamsungAcEncoder,
    driverId = "ir-ac-samsung",
    brandDisplayName = "AC — Samsung",
    manufacturer = "Samsung",
)

@Singleton
class DaikinAcDriver @Inject constructor(@ApplicationContext context: Context) : IrAcDriver(
    context = context,
    encoder = DaikinAcEncoder,
    driverId = "ir-ac-daikin",
    brandDisplayName = "AC — Daikin",
    manufacturer = "Daikin",
)
