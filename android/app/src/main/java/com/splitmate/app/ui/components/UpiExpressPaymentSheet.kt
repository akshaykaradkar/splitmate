package com.splitmate.app.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitmate.app.AvatarToken
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.SettlementTransferUiModel
import com.splitmate.app.ui.cleanIndianTenDigitPhone
import com.splitmate.app.ui.toSmartTitleCase
import java.util.Locale

data class UpiHandleSuffixOption(
    val suffix: String,
    val appBadge: String,
    val preferredPackage: String? = null
)

private val CommonIndianUpiSuffixes = listOf(
    UpiHandleSuffixOption("@ybl", "PhonePe", "com.phonepe.app"),
    UpiHandleSuffixOption("@paytm", "Paytm", "net.one97.paytm"),
    UpiHandleSuffixOption("@upi", "BHIM / NPCI", "in.org.npci.upiapp"),
    UpiHandleSuffixOption("@okaxis", "GPay Axis", "com.google.android.apps.nbu.paisa.user"),
    UpiHandleSuffixOption("@okicici", "GPay ICICI", "com.google.android.apps.nbu.paisa.user"),
    UpiHandleSuffixOption("@okhdfcbank", "GPay HDFC", "com.google.android.apps.nbu.paisa.user"),
    UpiHandleSuffixOption("@oksbi", "GPay SBI", "com.google.android.apps.nbu.paisa.user"),
    UpiHandleSuffixOption("@ibl", "PhonePe ICICI", "com.phonepe.app"),
    UpiHandleSuffixOption("@apl", "Amazon Pay", "in.amazon.mShop.android.shopping")
)

data class UpiExpressAppTarget(
    val id: String,
    val label: String,
    val subtitle: String,
    val packageName: String?,
    val defaultPhoneSuffix: String,
    val accentBg: Color,
    val accentBorder: Color,
    val accentText: Color
)

private val ExpressUpiAppTargets = listOf(
    UpiExpressAppTarget(
        id = "phonepe",
        label = "PhonePe",
        subtitle = "Auto @ybl · Pre-fills ₹",
        packageName = "com.phonepe.app",
        defaultPhoneSuffix = "@ybl",
        accentBg = Color(0xFFF3E8FF),
        accentBorder = Color(0xFFD8B4FE),
        accentText = Color(0xFF581C87)
    ),
    UpiExpressAppTarget(
        id = "gpay",
        label = "Google Pay",
        subtitle = "Auto @okaxis · Copies #",
        packageName = "com.google.android.apps.nbu.paisa.user",
        defaultPhoneSuffix = "@okaxis",
        accentBg = Color(0xFFE0F2FE),
        accentBorder = Color(0xFFBAE6FD),
        accentText = Color(0xFF075985)
    ),
    UpiExpressAppTarget(
        id = "paytm",
        label = "Paytm UPI",
        subtitle = "Auto @paytm · Pre-fills ₹",
        packageName = "net.one97.paytm",
        defaultPhoneSuffix = "@paytm",
        accentBg = Color(0xFFE0E7FF),
        accentBorder = Color(0xFFC7D2FE),
        accentText = Color(0xFF1E3A8A)
    ),
    UpiExpressAppTarget(
        id = "bhim",
        label = "BHIM / Any",
        subtitle = "Universal @upi chooser",
        packageName = null,
        defaultPhoneSuffix = "@upi",
        accentBg = Color(0xFFD7E8B6),
        accentBorder = Color(0xFFB7D382),
        accentText = Color(0xFF2D4810)
    )
)

/**
 * Builds a clean NPCI P2P `upi://pay` URI without unsigned merchant (`mc`/`sign`/`tr`) flags
 * so Google Pay, PhonePe, Paytm, and BHIM accept the pre-filled amount (`am`) and open MPIN entry directly.
 */
fun buildCleanP2pUpiUri(
    payeeVpa: String,
    payeeName: String,
    amountDecimal: String,
    transactionNote: String
): Uri {
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", payeeVpa.trim())
        .appendQueryParameter("pn", payeeName.trim())
        .appendQueryParameter("am", amountDecimal.trim())
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", transactionNote.trim().take(48))
        .build()
}

/**
 * Parses the NPCI UPI `ActivityResult` response string (`Status=SUCCESS&txnId=...&responseCode=00`).
 */
private fun isUpiActivityResultSuccess(resultCode: Int, data: Intent?): Boolean {
    val rawResponse = data?.getStringExtra("response")
        ?: data?.dataString
        ?: ""
    val params = rawResponse
        .split("&")
        .mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                pair.substring(0, idx).trim().lowercase(Locale.US) to pair.substring(idx + 1).trim()
            } else null
        }
        .toMap()
    val status = (params["status"] ?: data?.getStringExtra("Status").orEmpty()).uppercase(Locale.US)
    val responseCode = params["responsecode"] ?: ""
    return status == "SUCCESS" || responseCode == "00"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiExpressPaymentSheet(
    transferModel: SettlementTransferUiModel,
    groupName: String,
    initialSavedUpiId: String,
    onSaveMemberUpi: (String) -> Unit,
    onMarkSettled: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Extract initial 10-digit phone or custom VPA prefix
    val initialPrefix = remember(initialSavedUpiId, transferModel.cleanPhone) {
        val rawBeforeAt = initialSavedUpiId.substringBefore("@").trim()
        val cleanPhone = cleanIndianTenDigitPhone(rawBeforeAt.ifBlank { transferModel.cleanPhone })
        if (cleanPhone.length == 10) cleanPhone else rawBeforeAt
    }
    val initialSuffix = remember(initialSavedUpiId) {
        if (initialSavedUpiId.contains("@")) {
            "@" + initialSavedUpiId.substringAfter("@").trim()
        } else {
            "@ybl"
        }
    }

    var phoneOrPrefixInput by remember(transferModel.toMemberId) { mutableStateOf(initialPrefix) }
    var selectedSuffix by remember(transferModel.toMemberId) { mutableStateOf(initialSuffix) }
    var returnedFromUpiApp by remember { mutableStateOf(false) }
    var lastLaunchedAppName by remember { mutableStateOf("UPI App") }

    // If user typed a full VPA with '@' in the input box, split it live
    val effectivePrefix = remember(phoneOrPrefixInput) {
        if (phoneOrPrefixInput.contains("@")) {
            phoneOrPrefixInput.substringBefore("@").trim()
        } else {
            val maybe10 = cleanIndianTenDigitPhone(phoneOrPrefixInput)
            if (maybe10.length == 10) maybe10 else phoneOrPrefixInput.trim()
        }
    }
    val effectiveSuffix = remember(phoneOrPrefixInput, selectedSuffix) {
        if (phoneOrPrefixInput.contains("@") && phoneOrPrefixInput.substringAfter("@").isNotBlank()) {
            "@" + phoneOrPrefixInput.substringAfter("@").trim()
        } else {
            selectedSuffix
        }
    }
    val clean10DigitPhone = remember(effectivePrefix) {
        val cleaned = cleanIndianTenDigitPhone(effectivePrefix)
        if (cleaned.length == 10) cleaned else ""
    }
    val resolvedVpa = remember(effectivePrefix, effectiveSuffix) {
        if (effectivePrefix.isNotBlank()) "$effectivePrefix$effectiveSuffix" else ""
    }

    // Contact Picker to pull 10-digit phone number in 1 tap right from inside the payment sheet
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) > 0 else false
                        if (contactId != null && hasPhone) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { pCursor ->
                                if (pCursor.moveToFirst()) {
                                    val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    val rawNum = if (numIdx >= 0) pCursor.getString(numIdx) else ""
                                    val tenDigit = cleanIndianTenDigitPhone(rawNum)
                                    if (tenDigit.isNotEmpty()) {
                                        phoneOrPrefixInput = tenDigit
                                        onSaveMemberUpi("$tenDigit$selectedSuffix")
                                        Toast.makeText(
                                            context,
                                            "Linked +91 $tenDigit for ${transferModel.toName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // ActivityResult launcher to receive automatic NPCI Status=SUCCESS callback after MPIN entry
    val upiResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (isUpiActivityResultSuccess(result.resultCode, result.data)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (resolvedVpa.isNotBlank()) {
                onSaveMemberUpi(resolvedVpa)
            }
            Toast.makeText(
                context,
                "Payment of ${transferModel.formattedDisplayAmount} to ${transferModel.toName} verified & settled!",
                Toast.LENGTH_LONG
            ).show()
            onMarkSettled()
            onDismiss()
        } else {
            // Show 1-tap post-return confirmation bar in case the PSP omitted the result extra
            returnedFromUpiApp = true
        }
    }

    fun launchUpiPayment(targetApp: UpiExpressAppTarget?) {
        val activePrefix = effectivePrefix.trim()
        if (activePrefix.isEmpty()) {
            Toast.makeText(
                context,
                "Please enter ${transferModel.toName}'s 10-digit phone number or tap Pick Contact first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // If the prefix is a 10-digit phone number and the user tapped a specific UPI app tile
        // (e.g., PhonePe -> @ybl, Paytm -> @paytm), auto-sync the suffix unless user typed an explicit '@'
        val finalSuffix = if (!phoneOrPrefixInput.contains("@") && clean10DigitPhone.length == 10 && targetApp != null) {
            // If current suffix is default @upi or matches another app's default, switch to target app's native phone suffix
            selectedSuffix = targetApp.defaultPhoneSuffix
            targetApp.defaultPhoneSuffix
        } else {
            effectiveSuffix
        }

        val finalVpa = "$activePrefix$finalSuffix"
        onSaveMemberUpi(finalVpa)

        // Safety Net: Copy the 10-digit phone number (or VPA) to clipboard automatically
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipPayload = clean10DigitPhone.ifBlank { finalVpa }
        clipboard?.setPrimaryClip(ClipData.newPlainText("Recipient UPI Phone", clipPayload))

        val cleanNote = "SplitMate · ${groupName.toSmartTitleCase()}"
        val upiUri = buildCleanP2pUpiUri(
            payeeVpa = finalVpa,
            payeeName = transferModel.toName,
            amountDecimal = transferModel.amount,
            transactionNote = cleanNote
        )

        lastLaunchedAppName = targetApp?.label ?: "UPI App"
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        try {
            if (targetApp?.packageName != null) {
                val targetedIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                    setPackage(targetApp.packageName)
                }
                val canResolveTargeted = targetedIntent.resolveActivity(context.packageManager) != null
                if (canResolveTargeted) {
                    Toast.makeText(
                        context,
                        "Opening ${targetApp.label} for ${transferModel.formattedDisplayAmount} ($clipPayload copied)",
                        Toast.LENGTH_SHORT
                    ).show()
                    upiResultLauncher.launch(targetedIntent)
                    return
                }
            }
            // Fallback to universal UPI chooser
            val genericIntent = Intent(Intent.ACTION_VIEW, upiUri)
            Toast.makeText(
                context,
                "Pre-filling ${transferModel.formattedDisplayAmount} for $finalVpa ($clipPayload copied)",
                Toast.LENGTH_SHORT
            ).show()
            upiResultLauncher.launch(Intent.createChooser(genericIntent, "Pay ${transferModel.formattedDisplayAmount} via UPI"))
        } catch (_: Exception) {
            returnedFromUpiApp = true
            Toast.makeText(
                context,
                "No UPI app found on this device. Copied $clipPayload to clipboard.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SplitMateTheme.ScreenBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TACTILE RECEIPT STUB HEADER (Avatar + Pre-Filled Exact Amount Badge)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SplitMateTheme.SurfaceWhite,
                border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarToken(
                            initials = transferModel.toSeed.ifBlank { transferModel.toName },
                            bg = SplitMateTheme.SageSurface,
                            textColor = SplitMateTheme.SageText,
                            size = 46
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PAYING TO",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = SplitMateTheme.TextSecondary
                            )
                            Text(
                                text = transferModel.toName,
                                fontFamily = FigtreeFontFamily,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = SplitMateTheme.PrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "From ${transferModel.fromName} · ${groupName.toSmartTitleCase()}",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SplitMateTheme.SageSurface,
                        border = BorderStroke(1.dp, SplitMateTheme.AccentSage)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "PRE-FILLED",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = SplitMateTheme.SageText
                            )
                            Text(
                                text = transferModel.formattedDisplayAmount,
                                fontFamily = FigtreeFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }
                }
            }

            // POST-RETURN AUTO-SETTLE CONFIRMATION BANNER
            AnimatedVisibility(
                visible = returnedFromUpiApp,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SplitMateTheme.SageSurface,
                    border = BorderStroke(1.5.dp, SplitMateTheme.SageText.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = SplitMateTheme.SageText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Returned from $lastLaunchedAppName · Did you enter your UPI PIN?",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (resolvedVpa.isNotBlank()) {
                                        onSaveMemberUpi(resolvedVpa)
                                    }
                                    onMarkSettled()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitMateTheme.PrimaryDark,
                                    contentColor = SplitMateTheme.ScreenBg
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Yes, Mark ${transferModel.formattedDisplayAmount} Paid",
                                    fontFamily = FigtreeFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. RECIPIENT 10-DIGIT PHONE NUMBER OR UPI HANDLE INPUT + 1-TAP CONTACT PICKER
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. RECIPIENT 10-DIGIT PHONE OR UPI ID",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = SplitMateTheme.TextSecondary
                    )
                    Surface(
                        onClick = { contactPickerLauncher.launch(null) },
                        shape = CircleShape,
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Contacts,
                                contentDescription = null,
                                tint = SplitMateTheme.PrimaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Pick from Contacts",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneOrPrefixInput,
                    onValueChange = { phoneOrPrefixInput = it },
                    placeholder = {
                        Text(
                            text = "e.g. 9876543210 or priya@okaxis",
                            fontFamily = FigtreeFontFamily,
                            fontSize = 14.sp,
                            color = SplitMateTheme.TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (clean10DigitPhone.length == 10) Icons.Rounded.PhoneAndroid else Icons.Rounded.AlternateEmail,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (clean10DigitPhone.length == 10) {
                            Surface(
                                shape = CircleShape,
                                color = SplitMateTheme.SageSurface
                            ) {
                                Text(
                                    text = "✓ 10-Digit",
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. 1-TAP UPI HANDLE SUFFIX CHIPS (Bridges 10-Digit Phone -> Exact NPCI VPA)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. UPI HANDLE SUFFIX (AUTO-SAVED FOR ${transferModel.toName.uppercase(Locale.US)})",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
                        color = SplitMateTheme.TextSecondary
                    )
                    if (resolvedVpa.isNotBlank()) {
                        Text(
                            text = resolvedVpa,
                            fontFamily = FigtreeFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.SageText
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CommonIndianUpiSuffixes) { option ->
                        val isSelected = effectiveSuffix.equals(option.suffix, ignoreCase = true)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedSuffix = option.suffix
                                if (phoneOrPrefixInput.contains("@")) {
                                    phoneOrPrefixInput = phoneOrPrefixInput.substringBefore("@")
                                }
                                if (effectivePrefix.isNotBlank()) {
                                    onSaveMemberUpi("$effectivePrefix${option.suffix}")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = option.suffix,
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "· ${option.appBadge}",
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg.copy(alpha = 0.8f) else SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. EXPRESS UPI APP DOCK (1-Tap Redirect -> Pre-Filled Amount -> Enter UPI PIN)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. TAP YOUR UPI APP (OPENS WITH ${transferModel.formattedDisplayAmount} PRE-FILLED)",
                    fontFamily = FigtreeFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    color = SplitMateTheme.TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpressUpiAppTargets.take(2).forEach { appTarget ->
                        Surface(
                            onClick = { launchUpiPayment(appTarget) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (SplitMateTheme.isDark) SplitMateTheme.SurfaceWhite else appTarget.accentBg,
                            border = BorderStroke(1.dp, if (SplitMateTheme.isDark) SplitMateTheme.BorderLight else appTarget.accentBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (SplitMateTheme.isDark) SplitMateTheme.PrimaryDark else appTarget.accentText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = appTarget.label,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = if (SplitMateTheme.isDark) SplitMateTheme.PrimaryDark else appTarget.accentText
                                    )
                                    Text(
                                        text = appTarget.subtitle,
                                        fontFamily = FigtreeFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpressUpiAppTargets.drop(2).forEach { appTarget ->
                        Surface(
                            onClick = { launchUpiPayment(appTarget) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (SplitMateTheme.isDark) SplitMateTheme.SurfaceWhite else appTarget.accentBg,
                            border = BorderStroke(1.dp, if (SplitMateTheme.isDark) SplitMateTheme.BorderLight else appTarget.accentBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (SplitMateTheme.isDark) SplitMateTheme.PrimaryDark else appTarget.accentText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = appTarget.label,
                                        fontFamily = FigtreeFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = if (SplitMateTheme.isDark) SplitMateTheme.PrimaryDark else appTarget.accentText
                                    )
                                    Text(
                                        text = appTarget.subtitle,
                                        fontFamily = FigtreeFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SplitMateTheme.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. PRIMARY UNIVERSAL CTA BUTTON + MANUAL MARK PAID OPTION
            Button(
                onClick = { launchUpiPayment(null) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitMateTheme.PrimaryDark,
                    contentColor = SplitMateTheme.ScreenBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay ${transferModel.formattedDisplayAmount} via UPI App →",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already paid in cash or another app? ",
                    fontFamily = FigtreeFontFamily,
                    fontSize = 12.sp,
                    color = SplitMateTheme.TextSecondary
                )
                Text(
                    text = "✓ Mark as Paid",
                    fontFamily = FigtreeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SplitMateTheme.SageText,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMarkSettled()
                        onDismiss()
                    }
                )
            }
        }
    }
}
