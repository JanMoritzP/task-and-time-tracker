package com.example.taskandtimemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft neomorphic container that mimics an extruded surface using paired light/dark shadows.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 10.dp,
    innerPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    // Base surface color keeps in sync with light/dark theme.
    val background: Color = colorScheme.surface

    // Simple derived light/dark shadow tones; tuned to remain subtle in dark mode.
    val darkShadow: Color =
        if (isDark) colorScheme.background.copy(alpha = 0.9f) else colorScheme.outline.copy(alpha = 0.4f)
    val lightShadow: Color =
        if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f)

    val shape = RoundedCornerShape(cornerRadius)

    // Outer shadow for extruded look; we bias slightly downward/right to emphasize depth.
    val baseModifier =
        modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = darkShadow,
                spotColor = darkShadow,
            )
            .clip(shape)

    val clickableModifier =
        if (onClick != null) {
            baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
        } else {
            baseModifier
        }

    Surface(
        modifier = clickableModifier,
        color = background,
        contentColor = colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = shape,
    ) {
        // Use an inner Box to provide consistent padding while keeping the Surface clean.
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
