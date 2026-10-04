package com.splitmate.app.ui.share

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.SplitMateMathEngine
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * v2.3.5 (#2): M3 tonal "Share as image" button. Builds the [SettleUpShareModel] lazily on tap,
 * renders it off the main thread and opens the share sheet. Failures show a short toast.
 */
@Composable
fun SettleUpShareButton(
    buildModel: () -> SettleUpShareModel,
    modifier: Modifier = Modifier,
    label: String = "Share as image"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRendering by remember { mutableStateOf(false) }

    FilledTonalButton(
        onClick = {
            if (isRendering) return@FilledTonalButton
            isRendering = true
            scope.launch {
                val result = try {
                    val model = buildModel()
                    ShareImageHelper.renderAndShare(context, model, ShareImageHelper.captionFor(model))
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    Result.failure(t)
                }
                isRendering = false
                if (result.isFailure) {
                    Toast.makeText(context, "Couldn't create the settle-up image. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        },
        enabled = !isRendering,
        modifier = modifier.semantics { contentDescription = "Share settle-up as image" },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (isRendering) {
                // Short local render (not network / not money data): a small spinner on the button.
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/**
 * Builds the share model for one group from existing UI state (no ViewModel changes). Transfers
 * must be the same [SplitMateMathEngine.simplifyDebtsGreedy] output that the screen shows.
 */
fun buildSettleUpShareModelForGroup(
    groupName: String,
    groupMembers: List<GroupMemberEntity>,
    groupExpenses: List<ExpenseEntity>,
    transfers: List<SplitMateMathEngine.SimplifiedTransfer>,
    currentUserName: String?,
    tripEnded: Boolean = false,
    nowMs: Long = System.currentTimeMillis()
): SettleUpShareModel {
    val stamps = groupExpenses.flatMap { e -> listOfNotNull(e.createdAt, e.scheduledAtEpochMs) }.filter { it > 0L }
    val dateRange = if (stamps.isEmpty()) null else SettleUpShareModel.formatDateRange(stamps.min(), stamps.max())
    return SettleUpShareModel.build(
        tripName = groupName,
        transfers = transfers,
        members = groupMembers.distinctBy { it.memberId }.map { SettleUpShareMember(it.memberId, it.name, it.isCurrentUser) },
        totalSpentCents = groupExpenses.sumOf { it.totalAmountCents },
        currentUserName = currentUserName,
        dateRangeLabel = dateRange,
        tripEnded = tripEnded,
        asOfLabel = SettleUpShareModel.formatAsOf(nowMs)
    )
}
