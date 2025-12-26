package com.example.taskandtimemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple two-state pill toggle styled with a soft neomorphic background.
 */
@Composable
fun NeoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelOn: String = "On",
    labelOff: String = "Off",
    cornerRadius: Dp = 999.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val background: Color = colorScheme.surface
    val activeColor: Color = colorScheme.primary

    val darkShadow: Color =
        if (isDark) colorScheme.background.copy(alpha = 0.9f) else colorScheme.outline.copy(alpha = 0.4f)

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier =
        modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = darkShadow,
                spotColor = darkShadow,
            )
            .clip(shape)
            .clickable { onCheckedChange(!checked) },
        color = background,
        contentColor = colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (checked) labelOn else labelOff,
                color = if (checked) activeColor else colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
