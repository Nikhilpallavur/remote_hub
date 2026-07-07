package com.nikhilpallavur.remotehub.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography built on the platform default family so the app ships no font binaries yet
// stays tunable in one place. Weights and tracking are tightened for a flagship feel.
internal val RemoteHubTypography = Typography().run {
    val display = FontFamily.Default
    copy(
        displaySmall = displaySmall.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
    )
}
