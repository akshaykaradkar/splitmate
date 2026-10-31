package com.splitmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.splitmate.app.ui.feel.Feel
import com.splitmate.app.ui.feel.feel

/**
 * v2.4.0 D7: the one calm empty state used across the app.
 *
 * One soft M3 Expressive shape (Cookie4Sided, so it never reads as a member avatar), one plain
 * line and at most one action. No cards, no lists of options, no decoration on money. Static:
 * nothing animates, so it is also correct under reduced motion.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalmEmptyState(
    icon: ImageVector,
    line: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(MaterialShapes.Cookie4Sided.toShape())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = line,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(
                onClick = {
                    haptics.feel(Feel.Tick)
                    onAction()
                },
                shapes = ButtonDefaults.shapes(),
                // Sage tonal pair: the default secondaryContainer is peach, which means "owes" here.
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = if (actionIcon != null) {
                    ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight)
                } else {
                    ButtonDefaults.ContentPadding
                }
            ) {
                if (actionIcon != null) {
                    Icon(imageVector = actionIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                }
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
