package com.nikhilpallavur.remotehub.core.designsystem.motion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a guaranteed key-press tick for remote buttons.
 *
 * `View.performHapticFeedback(VIRTUAL_KEY)` is routed through the platform's *touch feedback*
 * setting, which several OEM skins (MIUI in particular) leave off by default — so the button
 * presses feel dead even though the code "fires". This drives the [Vibrator] directly instead,
 * which is only gated by the vibration-intensity / DND settings, so the tick is actually felt.
 *
 * Requires the `VIBRATE` permission (already declared in the app manifest). No-ops on devices
 * without a vibrator.
 */
@Composable
fun rememberHaptics(): () -> Unit {
    val context = LocalContext.current
    val vibrator = remember(context) { context.resolveVibrator() }
    return remember(vibrator) {
        {
            if (vibrator?.hasVibrator() == true) {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                    else -> @Suppress("DEPRECATION") vibrator.vibrate(12)
                }
            }
        }
    }
}

private fun Context.resolveVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
