package com.splitmate.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.splitmate.app.R

/**
 * Dynamic DiceBear Open-Peeps SVG Avatar (`https://api.dicebear.com/9.x/open-peeps/svg?seed={seed}`)
 * decoded natively via Coil + SvgDecoder.Factory() and cached indefinitely to disk, with
 * local Vector Drawable (`R.drawable.ic_avatar_placeholder`) fallback.
 */
@Composable
fun DiceBearAvatar(
    seed: String,
    contentDescription: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanSeed = seed.trim().ifEmpty { "SplitMateUser" }.replace(" ", "_")
    val svgUrl = "https://api.dicebear.com/9.x/open-peeps/svg?seed=$cleanSeed&backgroundColor=d7e8b6,fed8c8,dce3fd"

    val request = ImageRequest.Builder(context)
        .data(svgUrl)
        .diskCacheKey("dicebear_peep_$cleanSeed")
        .memoryCacheKey("dicebear_peep_$cleanSeed")
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
        error = painterResource(id = R.drawable.ic_avatar_placeholder),
        fallback = painterResource(id = R.drawable.ic_avatar_placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BuckwheatSageContainer, CircleShape)
            .border(1.dp, BuckwheatBorder, CircleShape)
    )
}

/**
 * Material 3 Expressive `HorizontalFloatingToolbar`:
 * Provides the primary data-entry cluster (NumPad trigger, quick-add chips `+$1`, `+$5`, `+$10`,
 * `+18% Tip`, and Commit CTA) at the bottom of the screen, collapsing smoothly with spring physics
 * into a Floating Action Button when the user scrolls down the ledger/receipt list.
 *
 * All interactive elements strictly enforce `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`.
 */
@Composable
fun HorizontalFloatingToolbar(
    collapsed: Boolean,
    currencySymbol: String,
    onQuickAddCents: (Long) -> Unit,
    onApplyEighteenPercentTip: () -> Unit,
    onOpenNumPadSheet: () -> Unit,
    onCommitExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("HorizontalFloatingToolbar"),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                    scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            scaleOut(spring(stiffness = Spring.StiffnessMedium))
                    )
            },
            label = "FloatingToolbarCollapseSpring"
        ) { isCollapsed ->
            if (isCollapsed) {
                ExtendedFloatingActionButton(
                    onClick = onOpenNumPadSheet,
                    containerColor = BuckwheatCharcoal,
                    contentColor = BuckwheatCanvas,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("CollapsedFloatingToolbarFab")
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = "Expand NumPad & Quick Entry")
                    Spacer(Modifier.width(8.dp))
                    Text("NumPad + Item", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = BuckwheatCharcoal,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ExpandedFloatingToolbarCluster")
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open Full NumPad Trigger (min 48x48dp)
                        FilledTonalButton(
                            onClick = onOpenNumPadSheet,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BuckwheatSageContainer,
                                contentColor = BuckwheatOlivePrimary
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("ToolbarNumPadBtn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("NumPad", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        // Quick Add Chips (+100c, +500c, +1000c) enforcing >= 48dp touch targets
                        listOf(100L to "+${currencySymbol}1", 500L to "+${currencySymbol}5", 1000L to "+${currencySymbol}10").forEach { (cents, label) ->
                            FilledTonalButton(
                                onClick = { onQuickAddCents(cents) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF332F2C),
                                    contentColor = BuckwheatCanvas
                                ),
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("QuickChip_$cents")
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // +18% Tip Quick Chip (min 48x48dp)
                        FilledTonalButton(
                            onClick = onApplyEighteenPercentTip,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BuckwheatPeachContainer,
                                contentColor = BuckwheatTerracottaDark
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("QuickChip_Tip18")
                        ) {
                            Text("+18% Tip", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        // Commit Collaborative Split Button (min 48x48dp)
                        Button(
                            onClick = onCommitExpense,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BuckwheatOlivePrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("CommitExpenseBtn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Split", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
