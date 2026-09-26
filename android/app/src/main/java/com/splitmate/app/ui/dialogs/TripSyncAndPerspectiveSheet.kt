package com.splitmate.app.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.BuckwheatOlivePrimary
import com.splitmate.app.ui.BuckwheatPeachContainer
import com.splitmate.app.ui.BuckwheatSageContainer
import com.splitmate.app.ui.BuckwheatTerracottaDark
import com.splitmate.app.ui.DesignSystemBindings
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.SplitMateTnumMonospace
import com.splitmate.app.ui.SplitMateViewModel
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.formatIndianRupeesFromCents
import com.splitmate.app.ui.performCrispTactileHaptic
import kotlin.math.abs

/**
 * Compact Buckwheat Sage header pill displaying `"Viewing as: <Member> (You)"` with `Icons.Rounded.Sync`
 * and `Icons.Rounded.KeyboardArrowDown`.
 *
 * Enforces WCAG 2.5.5 `48.dp` touch target bounds via `.minimumInteractiveComponentSize()` and
 * `.defaultMinSize(minHeight = 48.dp)` while preserving compact visual pill geometry.
 */
@Composable
fun PerspectiveAndSyncHeaderPill(
    activeMember: GroupMemberEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PerspectiveAndSyncHeaderPill(
        activeMemberName = activeMember?.name?.takeIf { it.isNotBlank() } ?: "Select Member",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun PerspectiveAndSyncHeaderPill(
    members: List<GroupMemberEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMember = remember(members) {
        members.find { it.isCurrentUser } ?: members.firstOrNull()
    }
    PerspectiveAndSyncHeaderPill(
        activeMember = activeMember,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun PerspectiveAndSyncHeaderPill(
    activeMemberName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val isDark = SplitMateTheme.isDark

    val pillBg = if (isDark) Color(0xFF233216) else BuckwheatSageContainer
    val pillText = if (isDark) BuckwheatSageContainer else BuckwheatOlivePrimary
    val pillBorder = if (isDark) Color(0xFF3E651E) else Color(0xFFB9D48B)
    val displayMember = activeMemberName.trim().ifEmpty { "Select Member" }

    Surface(
        onClick = {
            performCrispTactileHaptic(context, localView, heavy = false)
            onClick()
        },
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg, CircleShape)
                    .border(1.dp, pillBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Switch Perspective and Sync Trip",
                    tint = pillText,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Viewing as: $displayMember (You)",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = pillText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = pillText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Canonical Material 3 ModalBottomSheet for:
 * 1. 1-Tap Perspective Switching (`"Who are you in this trip?"`) with `performCrispTactileHaptic(context)`
 *    and tabular `tnum` per-member net balances.
 * 2. $0.00-Server-Cost GZIP+Base64url Sync Capsule Export (`SM2_<base64url>`) via WhatsApp or Clipboard.
 * 3. Auto-Detected Clipboard Capsule Banner & Manual Paste Union Merge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSyncAndPerspectiveSheet(
    viewModel: SplitMateViewModel,
    groupId: String = viewModel.uiState.value.activeGroupId,
    groupName: String = "",
    members: List<GroupMemberEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = SplitMateTheme.isDark

    val resolvedGroupId = groupId.ifBlank { uiState.activeGroup?.groupId ?: uiState.activeGroupId }
    val resolvedGroup = remember(uiState.groups, resolvedGroupId) {
        uiState.groups.find { it.groupId == resolvedGroupId } ?: uiState.activeGroup
    }
    val resolvedGroupName = groupName.ifBlank { resolvedGroup?.name ?: "Trip Hub" }
    val resolvedMembers = remember(members, uiState.members, resolvedGroupId) {
        uiState.members.filter { it.groupId == resolvedGroupId }.ifEmpty { members }
    }

    val netBalancesMap = remember(
        resolvedGroupId,
        resolvedMembers,
        uiState.expenses,
        uiState.splits,
        uiState.settlements
    ) {
        viewModel.computeGroupMemberNetBalances(resolvedGroupId)
    }

    val exportBundle = remember(
        resolvedGroupId,
        resolvedGroupName,
        resolvedMembers,
        uiState.expenses,
        uiState.splits,
        uiState.settlements
    ) {
        viewModel.exportGroupSyncPayload(resolvedGroupId)
    }

    var detectedClipboardCapsule by remember { mutableStateOf<String?>(null) }
    var manualPasteInput by remember { mutableStateOf("") }
    var feedbackBannerText by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }
    var shortInviteUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resolvedGroupId) {
        viewModel.generateShortInviteLink(resolvedGroupId) { generatedUrl ->
            shortInviteUrl = generatedUrl
        }
    }

    LaunchedEffect(resolvedGroupId, exportBundle?.syncToken) {
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            
            // Try extracting SM2 code or short link g parameter
            val urlRegex = Regex("""https://akshaykaradkar\.github\.io/splitmate/join\?g=([A-Za-z0-9_-]+)""")
            val shortKeyMatch = urlRegex.find(clipText)?.groupValues?.getOrNull(1)
            
            val extracted = SplitMateViewModel.extractSyncTokenFromRawInput(clipText) ?: shortKeyMatch
            detectedClipboardCapsule = if (extracted != null && extracted != exportBundle?.syncToken) {
                extracted
            } else {
                null
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SplitMateTheme.ScreenBg,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SplitMateTheme.BorderLight,
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .animateContentSize(animationSpec = DesignSystemBindings.tactileSpring()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF233216) else BuckwheatSageContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = if (isDark) BuckwheatSageContainer else BuckwheatOlivePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Perspective & P2P Trip Sync",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                        Text(
                            text = "$resolvedGroupName · Zero-Cost Serverless Capsule",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = SplitMateTheme.TextSecondary
                        )
                    }
                }
            }

            // Status / Feedback Banner
            AnimatedVisibility(visible = !feedbackBannerText.isNullOrBlank()) {
                val bannerBg = when {
                    isFeedbackError && isDark -> Color(0xFF3A2019)
                    isFeedbackError -> BuckwheatPeachContainer
                    isDark -> Color(0xFF233216)
                    else -> BuckwheatSageContainer
                }
                val bannerText = when {
                    isFeedbackError && isDark -> Color(0xFFFECDD3)
                    isFeedbackError -> BuckwheatTerracottaDark
                    isDark -> BuckwheatSageContainer
                    else -> BuckwheatOlivePrimary
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bannerBg,
                    border = BorderStroke(1.dp, bannerText.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isFeedbackError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = bannerText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = feedbackBannerText.orEmpty(),
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = bannerText
                        )
                    }
                }
            }

            // =========================================================================
            // SECTION 1: Invite Friends to Trip
            // =========================================================================
            if (exportBundle != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SplitMateTheme.SurfaceWhite,
                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Invite Friends to Trip",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SplitMateTheme.SurfaceMuted,
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${exportBundle.memberCount} members · ${exportBundle.expenseCount} expenses · ${exportBundle.compressedBytesSize} bytes",
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = shortInviteUrl ?: "Generating secure short link...",
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = SplitMateTheme.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val activeShareText = if (shortInviteUrl != null) {
                                "Join my trip on SplitMate!\n$shortInviteUrl"
                            } else {
                                exportBundle.whatsappShareText
                            }

                            Button(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, activeShareText)
                                        setPackage("com.whatsapp")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(whatsappIntent)
                                    } catch (_: Exception) {
                                        val fallbackIntent = Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, activeShareText)
                                            },
                                            "Share SplitMate Trip Invite"
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(fallbackIntent) }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BuckwheatOlivePrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share Invite Link",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    performCrispTactileHaptic(context, localView, heavy = false)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("SplitMate Trip Sync", activeShareText)
                                    )
                                    isFeedbackError = false
                                    feedbackBannerText = "Copied invite link to clipboard"
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy Capsule",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SplitMateTheme.PrimaryDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION 2: Auto-Detected Clipboard Capsule Banner & Manual Paste Merge
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SplitMateTheme.SurfaceWhite,
                border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MergeType,
                            contentDescription = null,
                            tint = SplitMateTheme.SageText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Have an invite link or code? Paste here",
                            fontFamily = FigtreeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }

                    // Auto-detected clipboard banner when a valid SM2_ token or short link is on the clipboard
                    val clipToken = detectedClipboardCapsule
                    if (!clipToken.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF233216) else BuckwheatSageContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isDark) BuckwheatSageContainer.copy(alpha = 0.4f) else BuckwheatOlivePrimary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sync Capsule Detected in Clipboard",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isDark) BuckwheatSageContainer else BuckwheatOlivePrimary
                                )
                                Text(
                                    text = clipToken,
                                    style = TextStyle(
                                        fontFamily = SplitMateTnumMonospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isDark) BuckwheatSageContainer.copy(alpha = 0.85f) else BuckwheatOlivePrimary.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Button(
                                    onClick = {
                                        performCrispTactileHaptic(context, localView, heavy = false)
                                        if (!clipToken.startsWith("SM2_") && clipToken.length < 50) {
                                            viewModel.resolveAndMergeShortInviteKey(clipToken)
                                            isFeedbackError = false
                                            feedbackBannerText = "Fetching invite link..."
                                            detectedClipboardCapsule = null
                                        } else {
                                            val result = viewModel.importAndMergeGroupSyncPayload(
                                                rawPayloadOrMessage = clipToken,
                                                openGroupAfterMerge = true
                                            )
                                            isFeedbackError = !result.success
                                            feedbackBannerText = result.message
                                            if (result.success) {
                                                detectedClipboardCapsule = null
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BuckwheatOlivePrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Merge Clipboard Capsule",
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = SplitMateTheme.BorderLight)
                    }

                    OutlinedTextField(
                        value = manualPasteInput,
                        onValueChange = { manualPasteInput = it },
                        placeholder = {
                            Text(
                                text = "Paste WhatsApp sync message or SM2_... capsule",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 12.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = SplitMateTnumMonospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = SplitMateTheme.PrimaryDark,
                            fontFeatureSettings = "tnum"
                        ),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuckwheatOlivePrimary,
                            unfocusedBorderColor = SplitMateTheme.BorderLight,
                            focusedContainerColor = SplitMateTheme.SurfaceMuted,
                            unfocusedContainerColor = SplitMateTheme.SurfaceMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                if (clipText.isNotBlank()) {
                                    manualPasteInput = clipText
                                    val extracted = SplitMateViewModel.extractSyncTokenFromRawInput(clipText)
                                    if (extracted != null && extracted != exportBundle?.syncToken) {
                                        detectedClipboardCapsule = extracted
                                    }
                                } else {
                                    isFeedbackError = true
                                    feedbackBannerText = "Clipboard is empty"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paste from Clipboard",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SplitMateTheme.PrimaryDark,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                performCrispTactileHaptic(context, localView, heavy = false)
                                
                                val urlRegex = Regex("""https://akshaykaradkar\.github\.io/splitmate/join\?g=([A-Za-z0-9_-]+)""")
                                val shortKeyMatch = urlRegex.find(manualPasteInput)?.groupValues?.getOrNull(1)
                                
                                if (shortKeyMatch != null || (!manualPasteInput.startsWith("SM2_") && manualPasteInput.trim().length < 50)) {
                                    val key = shortKeyMatch ?: manualPasteInput.trim()
                                    viewModel.resolveAndMergeShortInviteKey(key)
                                    isFeedbackError = false
                                    feedbackBannerText = "Fetching invite link..."
                                    manualPasteInput = ""
                                    detectedClipboardCapsule = null
                                } else {
                                    val result = viewModel.importAndMergeGroupSyncPayload(
                                        rawPayloadOrMessage = manualPasteInput,
                                        openGroupAfterMerge = true
                                    )
                                    isFeedbackError = !result.success
                                    feedbackBannerText = result.message
                                    if (result.success) {
                                        manualPasteInput = ""
                                        detectedClipboardCapsule = null
                                    }
                                }
                            },
                            enabled = manualPasteInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) BuckwheatSageContainer else Color(0xFF23201E),
                                contentColor = if (isDark) BuckwheatOlivePrimary else Color.White,
                                disabledContainerColor = SplitMateTheme.BorderLight,
                                disabledContentColor = SplitMateTheme.TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MergeType,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Merge Trip Capsule",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
