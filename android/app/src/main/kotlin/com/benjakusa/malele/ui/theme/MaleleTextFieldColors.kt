package com.benjakusa.malele.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared OutlinedTextField color scheme for the whole app.
 *
 * - Idle (unfocused): no visible outline, container blends with card surface,
 *   placeholder is muted gray (not near-black onSurfaceVariant).
 * - Focused: subtle primary-colored outline so the field is still discoverable.
 */
@Composable
fun maleleTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    // Idle = no line
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    // Focus = subtle primary line (accessibility)
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    // Container blends with card surface so only the page background reads as "shadow"
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    // Gray placeholders, not black
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    // Text stays full-contrast
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    // Cursor + labels
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
)
