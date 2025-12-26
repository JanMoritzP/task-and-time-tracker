package com.example.taskandtimemanager.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neomorphic primary action button that keeps Material semantics but softens the styling.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val background: Color = if (enabled) colorScheme.primary else colorScheme.surfaceVariant
    val contentColor: Color = if (enabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant

    val darkShadow: Color =
        if (isDark) colorScheme.background.copy(alpha = 0.9f) else colorScheme.primary.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(cornerRadius)

    val interactionSource = remember { MutableInteractionSource() }

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
        if (enabled) {
            baseModifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            baseModifier
        }

    Surface(
        modifier = clickableModifier,
        color = background,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = shape,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
