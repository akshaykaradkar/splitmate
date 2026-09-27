package com.splitmate.app.ui

import com.splitmate.app.SplitMateTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dynamic DiceBear 9.x SVG Avatar wrapper (`DiceBearAvatar`) delegated to
 * `SplitMateCharacterAvatar` with Coil disk/memory caching and offline monogram fallback.
 */
@Composable
fun DiceBearAvatar(
    seed: String,
    @Suppress("UNUSED_PARAMETER") contentDescription: String = "",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    SplitMateCharacterAvatar(
        name = seed,
        size = size,
        modifier = modifier
    )
}

/**
 * Shared Buckwheat Material 3 Expressive OutlinedTextField (`SplitMateTextField`)
 * with full IME action (`ImeAction.Next` / `ImeAction.Done`) and `KeyboardActions` support
 * so keyboard navigation never steals or drops focus unexpectedly.
 */
@Composable
fun SplitMateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let {
            {
                Text(
                    text = it,
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    fontFamily = FigtreeFontFamily,
                    color = SplitMateTheme.TextSecondary
                )
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SplitMateTheme.PrimaryDark,
            unfocusedTextColor = SplitMateTheme.PrimaryDark,
            focusedBorderColor = SplitMateTheme.PrimaryDark,
            unfocusedBorderColor = SplitMateTheme.BorderLight,
            focusedContainerColor = SplitMateTheme.SurfaceWhite,
            unfocusedContainerColor = SplitMateTheme.SurfaceWhite,
            cursorColor = SplitMateTheme.PrimaryDark
        )
    )
}
