package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.AppStrings
import com.example.model.BuiltInThemes
import com.example.model.ButtonShapeStyle
import com.example.model.IconStyle
import com.example.model.InterfaceMode
import com.example.model.TrakasThemeConfig
import com.example.model.stringsFor

@Immutable
data class TrakasVisualTokens(
    val config: TrakasThemeConfig,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val buttonColor: Color,
    val textColor: Color,
    val mutedTextColor: Color,
    val progressColor: Color,
    val visualizerColor: Color,
    val cardAlpha: Float,
    val buttonCornerDp: Int,
    val effectIntensity: Float,
    val animationsEnabled: Boolean,
    val interfaceMode: InterfaceMode
)

val LocalTrakasTokens = staticCompositionLocalOf {
    val c = BuiltInThemes.trakasDark
    TrakasVisualTokens(
        config = c,
        primary = Color(c.primaryArgb),
        secondary = Color(c.secondaryArgb),
        background = Color(c.backgroundArgb),
        surfaceCard = Color(c.cardArgb),
        surfaceElevated = Color(0xFF1B243B),
        buttonColor = Color(c.buttonArgb),
        textColor = Color(c.textArgb),
        mutedTextColor = Color(c.textArgb).copy(alpha = 0.68f),
        progressColor = Color(c.progressArgb),
        visualizerColor = Color(c.visualizerArgb),
        cardAlpha = c.cardAlpha,
        buttonCornerDp = 14,
        effectIntensity = c.effectIntensity,
        animationsEnabled = true,
        interfaceMode = InterfaceMode.TRAKAS_PLAYER
    )
}

val LocalIconStyle = staticCompositionLocalOf { IconStyle.NEON }
val LocalAppStrings = staticCompositionLocalOf { stringsFor(AppLanguage.SPANISH) }

@Composable
fun TrakasTheme(
    themeConfig: TrakasThemeConfig = BuiltInThemes.trakasDark,
    iconStyle: IconStyle = IconStyle.NEON,
    language: AppLanguage = AppLanguage.SPANISH,
    animationsEnabled: Boolean = true,
    interfaceMode: InterfaceMode = InterfaceMode.TRAKAS_PLAYER,
    textScaleFactor: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isNightMode = interfaceMode == InterfaceMode.TRAKAS_NIGHT
    val primary = Color(themeConfig.primaryArgb)
    val secondary = Color(themeConfig.secondaryArgb)
    val bg = if (isNightMode) Color(0xFF000000) else Color(themeConfig.backgroundArgb)
    val btn = Color(themeConfig.buttonArgb)
    val text = Color(themeConfig.textArgb)
    val progressCol = Color(themeConfig.progressArgb)
    val visualizerCol = Color(themeConfig.visualizerArgb)

    val rawCard = if (isNightMode) Color(0xFF07090F) else Color(themeConfig.cardArgb)
    val surfaceBase = rawCard.copy(alpha = themeConfig.cardAlpha.coerceIn(0.35f, 0.98f))
    val surfaceHigh = Color(
        red = (rawCard.red + 0.04f).coerceIn(0f, 1f),
        green = (rawCard.green + 0.05f).coerceIn(0f, 1f),
        blue = (rawCard.blue + 0.07f).coerceIn(0f, 1f),
        alpha = 0.96f
    )

    val buttonStyle = ButtonShapeStyle.fromId(themeConfig.buttonShapeId)
    val corner = buttonStyle.cornerDp

    val tokens = TrakasVisualTokens(
        config = themeConfig,
        primary = primary,
        secondary = secondary,
        background = bg,
        surfaceCard = surfaceBase,
        surfaceElevated = surfaceHigh,
        buttonColor = btn,
        textColor = text,
        mutedTextColor = text.copy(alpha = 0.66f),
        progressColor = progressCol,
        visualizerColor = visualizerCol,
        cardAlpha = themeConfig.cardAlpha,
        buttonCornerDp = corner,
        effectIntensity = themeConfig.effectIntensity,
        animationsEnabled = animationsEnabled && themeConfig.animationsEnabled,
        interfaceMode = interfaceMode
    )

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = primary.contrastText(),
        primaryContainer = primary.copy(alpha = 0.20f),
        onPrimaryContainer = text,
        secondary = secondary,
        onSecondary = secondary.contrastText(),
        secondaryContainer = secondary.copy(alpha = 0.22f),
        onSecondaryContainer = text,
        tertiary = btn,
        onTertiary = btn.contrastText(),
        background = bg,
        onBackground = text,
        surface = surfaceBase,
        onSurface = text,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = text.copy(alpha = 0.72f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surfaceBase,
        surfaceContainer = surfaceBase,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHigh.adjustBrightness(1.2f),
        outline = primary.copy(alpha = 0.35f),
        outlineVariant = text.copy(alpha = 0.14f),
        error = Color(0xFFFF5252),
        onError = Color.White
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape((corner / 2).coerceAtLeast(4).dp),
        small = RoundedCornerShape((corner * 0.7f).toInt().coerceAtLeast(6).dp),
        medium = RoundedCornerShape(corner.dp),
        large = RoundedCornerShape((corner + 6).coerceAtMost(28).dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

    val strings: AppStrings = stringsFor(language)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = (baseDensity.fontScale * textScaleFactor).coerceIn(0.85f, 1.8f)
    )

    CompositionLocalProvider(
        LocalTrakasTokens provides tokens,
        LocalIconStyle provides iconStyle,
        LocalAppStrings provides strings,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrakasTypography,
            shapes = shapes,
            content = content
        )
    }
}
