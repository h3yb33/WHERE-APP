package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WhereLightColorScheme = lightColorScheme(
    primary = WhereDeepGreen,
    onPrimary = WhereWhite,
    primaryContainer = WhereMintSurface,
    onPrimaryContainer = WhereInfoTitle,
    secondary = LandingMintAccent,
    onSecondary = WhereInfoTitle,
    secondaryContainer = WhereInfoBg,
    onSecondaryContainer = WhereInfoTitle,
    tertiary = OverchargeAmber,
    onTertiary = WhereWhite,
    tertiaryContainer = OverchargeSoftBg,
    onTertiaryContainer = WhereMainText,
    background = WhereWhite,
    onBackground = WhereMainText,
    surface = WhereWhite,
    onSurface = WhereMainText,
    surfaceVariant = WhereMintSurface,
    onSurfaceVariant = WhereMutedText,
    outline = WhereSubtleBorder,
    error = AlertRed,
    onError = WhereWhite,
    errorContainer = AlertSoftBg,
    onErrorContainer = AlertRed
)

val WhereShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun WhereTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WhereLightColorScheme,
        typography = Typography,
        shapes = WhereShapes,
        content = content
    )
}
