package com.splitmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.SplitMateExpressiveTypography

/**
 * v2.4.0 rc2: the one "Mark paid" control. It's an official M3 Expressive pill [Button] with the
 * library press morph (`ButtonDefaults.shapes()`). It replaces the split button, whose
 * near-square inner corners read as "half square, half round". If there's a real second action
 * (remind on WhatsApp), it's a separate round tonal icon button next to the pill, not a menu.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkPaidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    fillWidth: Boolean = false,
    onRemind: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = (if (fillWidth) Modifier.weight(1f) else Modifier).heightIn(min = 40.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = "Mark paid", style = SplitMateExpressiveTypography.labelLargeEmphasized, maxLines = 1)
        }
        if (onRemind != null) {
            FilledTonalIconButton(
                onClick = onRemind,
                shapes = IconButtonDefaults.shapes(),
                // Sage tonal pair: the default secondaryContainer is peach, which means "owes" here.
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.Chat, contentDescription = "Remind on WhatsApp")
            }
        }
    }
}
