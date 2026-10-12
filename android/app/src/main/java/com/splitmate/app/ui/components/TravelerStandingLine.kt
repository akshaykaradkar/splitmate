package com.splitmate.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splitmate.app.MoneyTrustRules
import com.splitmate.app.ui.a11y.SpokenMoney
import com.splitmate.app.ui.formatIndianRupeesFromCents

/**
 * v2.4.0 D5: "Paid ₹3,000 · Share ₹2,250 · Gets back ₹750" with a thin paid-vs-share bar.
 *
 * The bar is the official M3 Expressive determinate [LinearProgressIndicator] (flat, never wavy:
 * wavy is reserved for in-flight network work). It fills to how much of their part a traveler has
 * covered (olive when they owe nothing, terracotta while they still owe) and springs in with
 * [ProgressIndicatorDefaults.ProgressAnimationSpec]; under "Remove animations" it shows the final
 * value at once. The words always carry the meaning, so colour is never the only signal.
 */
@Composable
fun TravelerStandingLine(
    standing: MoneyTrustRules.TravelerStanding,
    modifier: Modifier = Modifier
) {
    val net = standing.netCents
    val paid = formatIndianRupeesFromCents(standing.paidCents)
    val share = formatIndianRupeesFromCents(standing.shareCents)
    val outcome = when {
        net > 0L -> "Gets back ${formatIndianRupeesFromCents(net)}"
        net < 0L -> "Owes ${formatIndianRupeesFromCents(-net)}"
        else -> "Settled"
    }
    val spokenOutcome = when {
        net > 0L -> "gets back ${SpokenMoney.rupees(net)}"
        net < 0L -> "owes ${SpokenMoney.rupees(-net)}"
        else -> "settled"
    }
    val owes = net < 0L
    val target = standing.coveredFraction
    val fill = remember { Animatable(0f) }
    LaunchedEffect(target) { fill.animateTo(target, ProgressIndicatorDefaults.ProgressAnimationSpec) }

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Paid ${SpokenMoney.rupees(standing.paidCents)}, share ${SpokenMoney.rupees(standing.shareCents)}, $spokenOutcome"
        },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Paid $paid · Share $share · $outcome",
            style = MaterialTheme.typography.labelSmall.merge(TextStyle(fontFeatureSettings = "tnum")),
            color = if (owes) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { fill.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = if (owes) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {}
        )
    }
}
