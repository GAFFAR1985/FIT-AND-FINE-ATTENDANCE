package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldGreenDark,
    onPrimary = NavyDark,
    primaryContainer = EmeraldGreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = MintAccent,
    onSecondary = NavyDark,
    secondaryContainer = NavyLight,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldGreenLight,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = NavyDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ProfessionalGreen600,
    onPrimary = Color.White,
    primaryContainer = ProfessionalGreen100,
    onPrimaryContainer = ProfessionalGreen800,
    secondary = ProfessionalBlue900,
    onSecondary = Color.White,
    secondaryContainer = ProfessionalBlue100,
    onSecondaryContainer = ProfessionalBlue900,
    tertiary = ProfessionalBlue700,
    background = ProfessionalSlate50,
    surface = Color.White,
    surfaceVariant = ProfessionalSlate100,
    onBackground = ProfessionalSlate900,
    onSurface = ProfessionalSlate900,
    onSurfaceVariant = ProfessionalSlate600,
  )

@Composable
fun FitAndFineTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}

