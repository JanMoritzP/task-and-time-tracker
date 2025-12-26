package com.example.taskandtimemanager.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
 * Neomorphic-styled text field wrapping Material3 OutlinedTextField.
 * Uses soft shadows and high-contrast text while preserving accessibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 14.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val background: Color = colorScheme.surface
    val textColor: Color = colorScheme.onSurface
    val placeholderColor: Color = colorScheme.onSurfaceVariant
    val outlineColor: Color = if (isDark) colorScheme.outlineVariant else colorScheme.outline

    val darkShadow: Color =
        if (isDark) colorScheme.background.copy(alpha = 0.9f) else colorScheme.outline.copy(alpha = 0.35f)

    val shape = RoundedCornerShape(cornerRadius)

    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = darkShadow,
                spotColor = darkShadow,
            )
            .clip(shape),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        label = label?.let { text ->
            { Text(text = text, style = MaterialTheme.typography.labelLarge) }
        },
        placeholder = placeholder?.let { text ->
            { Text(text = text, style = MaterialTheme.typography.bodyMedium, color = placeholderColor) }
        },
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = background,
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = outlineColor,
            disabledBorderColor = outlineColor.copy(alpha = 0.5f),
            cursorColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant,
            disabledLabelColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            focusedLeadingIconColor = colorScheme.primary,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            focusedTrailingIconColor = colorScheme.primary,
            unfocusedTrailingIconColor = colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        ),
        shape = shape,
    )
}
