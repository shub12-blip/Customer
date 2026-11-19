package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AicsmDarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = OnSkyPrimaryDark,
    primaryContainer = SkyPrimaryContainerDark,
    onPrimaryContainer = OnSkyPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = OnAmberSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = OnAmberSecondaryContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = OnMintTertiaryDark,
    tertiaryContainer = MintTertiaryContainerDark,
    onTertiaryContainer = OnMintTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark
)

private val AicsmLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = OnCobaltPrimary,
    primaryContainer = CobaltPrimaryContainer,
    onPrimaryContainer = OnCobaltPrimaryContainer,
    secondary = SaffronSecondary,
    onSecondary = OnSaffronSecondary,
    secondaryContainer = SaffronSecondaryContainer,
    onSecondaryContainer = OnSaffronSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight
)

val AicsmShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AicsmPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AicsmDarkColorScheme
        else -> AicsmLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AicsmTypography,
        shapes = AicsmShapes,
        content = content
    )
}
