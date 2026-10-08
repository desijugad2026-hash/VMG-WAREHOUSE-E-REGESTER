package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarehouseLightColorScheme = lightColorScheme(
  primary = PrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = Slate100,
  onPrimaryContainer = Slate900,
  secondary = Slate700,
  onSecondary = Color.White,
  secondaryContainer = Slate100,
  onSecondaryContainer = Slate800,
  tertiary = SoftTealIcon,
  background = Color(0xFFF9FAFB),
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate50,
  onSurfaceVariant = Slate700,
  outline = Slate200,
  outlineVariant = Slate100
)

@Composable
fun WarehouseRegisterTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = WarehouseLightColorScheme,
    typography = Typography,
    content = content
  )
}
