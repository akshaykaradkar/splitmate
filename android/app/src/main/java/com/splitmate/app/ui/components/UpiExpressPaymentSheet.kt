package com.splitmate.app.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.splitmate.app.AvatarToken
import com.splitmate.app.R
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.SettlementTransferUiModel
import com.splitmate.app.ui.cleanIndianTenDigitPhone
import com.splitmate.app.ui.toSmartTitleCase
import java.util.Locale

private val QuickVpaBankSuffixes = listOf(
    "@okhdfcbank" to "GPay HDFC",
    "@okaxis" to "GPay Axis",
    "@okicici" to "GPay ICICI",
    "@oksbi" to "GPay SBI",
    "@ybl" to "PhonePe",
    "@paytm" to "Paytm",
    "@upi" to "BHIM / NPCI",
    "@ibl" to "PhonePe ICICI",
    "@apl" to "Amazon Pay"
)

/**
 * Posts a high-priority Heads-Up Notification (`SplitMate Payment Assist`) that stays visible at the top
 * of the phone while the user is inside Google Pay / PhonePe / Paytm, showing the exact Rupee amount
 * (`₹1,417.00`) and confirming that the friend's phone number / UPI ID is copied to clipboard.
 */
private fun postPaymentAssistHeadsUpNotification(
    context: Context,
    recipientName: String,
    formattedAmount: String,
    copiedIdentifier: String,
    appName: String
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return
        }
        val channelId = "splitmate_upi_payment_assist"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SplitMate UPI Payment Assist",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays exact settlement amount while paying in Google Pay / PhonePe / Paytm"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pay $recipientName $formattedAmount · SplitMate")
            .setContentText("Copied $copiedIdentifier! Paste in $appName search bar & type $formattedAmount")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "1. Tap the top Search / New Payment bar in $appName\n" +
                        "2. Paste '$copiedIdentifier' (already on your keyboard strip!)\n" +
                        "3. Enter $formattedAmount and your UPI PIN"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(9401, notification)
    } catch (_: Exception) {
    }
}

/**
 * Builds a clean NPCI P2P `upi://pay` URI (`mode=00`) for apps that support direct VPA intents
 * (BHIM, Paytm, PhonePe when a verified VPA like `gauri301998@okhdfcbank` or `9876543210@ybl` is provided).
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
        .appendQueryParameter("mode", "00")
        .build()
}

private fun isUpiActivityResultSuccess(data: Intent?): Boolean {
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Parse dual storage format "9876543210|gauri301998@okhdfcbank" or single phone/VPA
    val parsedInitial = remember(initialSavedUpiId, transferModel.cleanPhone) {
        val parts = initialSavedUpiId.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val extractedPhone = parts
            .map { cleanIndianTenDigitPhone(it.substringBefore("@")) }
            .firstOrNull { it.length == 10 }
            ?: transferModel.cleanPhone
        val extractedVpa = parts.firstOrNull { part ->
            part.contains("@") && !part.endsWith("@upi", ignoreCase = true)
        } ?: ""
        extractedPhone to extractedVpa
    }

    var phoneInput by remember(transferModel.toMemberId) { mutableStateOf(parsedInitial.first) }
    var exactVpaInput by remember(transferModel.toMemberId) { mutableStateOf(parsedInitial.second) }
    var hasLaunchedExternalApp by remember { mutableStateOf(false) }
    var returnedFromUpiApp by remember { mutableStateOf(false) }
    var lastLaunchedAppName by remember { mutableStateOf("Google Pay") }

    val clean10Phone = remember(phoneInput) {
        val cleaned = cleanIndianTenDigitPhone(phoneInput)
        if (cleaned.length == 10) cleaned else ""
    }
    val cleanExactVpa = remember(exactVpaInput) {
        val trimmed = exactVpaInput.trim()
        if (trimmed.contains("@") && trimmed.substringBefore("@").isNotBlank() && trimmed.substringAfter("@").isNotBlank()) {
            trimmed
        } else {
            ""
        }
    }

    // Combined persistence string so Room stores BOTH the 10-digit phone number AND the Gmail/custom VPA
    fun persistCombinedIdentity(newPhone: String = clean10Phone, newVpa: String = cleanExactVpa) {
        val combined = when {
            newPhone.length == 10 && newVpa.isNotBlank() -> "$newPhone|$newVpa"
            newVpa.isNotBlank() -> newVpa
            newPhone.length == 10 -> "$newPhone@upi"
            else -> ""
        }
        if (combined.isNotBlank()) {
            onSaveMemberUpi(combined)
        }
    }

    // Detect when the user returns from Google Pay / PhonePe / Paytm back to SplitMate (ON_RESUME)
    DisposableEffect(lifecycleOwner, hasLaunchedExternalApp) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLaunchedExternalApp) {
                returnedFromUpiApp = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request notification permission on Android 13+ so the floating Heads-Up Payment Assist banner shows while in GPay
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Contact Picker to pull 10-digit phone number in 1 tap
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
                                        phoneInput = tenDigit
                                        persistCombinedIdentity(newPhone = tenDigit)
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

    val upiResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (isUpiActivityResultSuccess(result.data)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            persistCombinedIdentity()
            Toast.makeText(
                context,
                "Payment of ${transferModel.formattedDisplayAmount} to ${transferModel.toName} settled!",
                Toast.LENGTH_LONG
            ).show()
            onMarkSettled()
            onDismiss()
        } else {
            returnedFromUpiApp = true
        }
    }

    /**
     * SMART NATIVE APP LAUNCH (100% Immune to Google Pay's "Cannot pay with this QR" & "Could not load banking name"):
     * 1. Copies Gauri's 10-digit phone number (`9876543210`) or exact Gmail UPI ID (`gauri301998@okhdfcbank`) to Clipboard.
     * 2. Posts a High-Priority Heads-Up Notification with the exact Rupee amount (`₹1,417.00`).
     * 3. Opens Google Pay / PhonePe / Paytm natively via `getLaunchIntentForPackage` so the user taps the top search bar,
     *    taps the pasted phone/VPA on their keyboard strip, and Google Pay's internal servers resolve her verified banking name!
     */
    fun launchSmartNativeUpiApp(packageName: String, appLabel: String, preferPhoneInSearch: Boolean = true) {
        val searchPayload = when {
            preferPhoneInSearch && clean10Phone.length == 10 -> clean10Phone
            cleanExactVpa.isNotBlank() -> cleanExactVpa
            clean10Phone.length == 10 -> clean10Phone
            else -> ""
        }
        if (searchPayload.isEmpty()) {
            Toast.makeText(
                context,
                "Enter ${transferModel.toName}'s 10-digit phone number (or UPI ID like gauri301998@okhdfcbank) first",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        persistCombinedIdentity()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("UPI Recipient", searchPayload))

        postPaymentAssistHeadsUpNotification(
            context = context,
            recipientName = transferModel.toName,
            formattedAmount = transferModel.formattedDisplayAmount,
            copiedIdentifier = searchPayload,
            appName = appLabel
        )

        lastLaunchedAppName = appLabel
        hasLaunchedExternalApp = true

        val nativeLaunchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (nativeLaunchIntent != null) {
            Toast.makeText(
                context,
                "Copied $searchPayload! Paste in $appLabel search bar to pay ${transferModel.formattedDisplayAmount} to ${transferModel.toName}",
                Toast.LENGTH_LONG
            ).show()
            context.startActivity(nativeLaunchIntent)
        } else {
            Toast.makeText(
                context,
                "$appLabel is not installed. Copied $searchPayload (${transferModel.formattedDisplayAmount}) to clipboard!",
                Toast.LENGTH_LONG
            ).show()
            returnedFromUpiApp = true
        }
    }

    /**
     * DIRECT NPCI `upi://pay` PRE-FILLED AMOUNT INTENT:
     * Uses the verified VPA (e.g. `gauri301998@okhdfcbank` or `9876543210@ybl` / `9876543210@paytm`)
     * for apps that accept P2P intent deep-links (BHIM, Paytm, PhonePe).
     */
    fun launchPreFilledUpiIntent(targetPackage: String?, appLabel: String, fallbackSuffix: String) {
        val targetVpa = when {
            cleanExactVpa.isNotBlank() -> cleanExactVpa
            clean10Phone.length == 10 -> "$clean10Phone$fallbackSuffix"
            else -> ""
        }
        if (targetVpa.isEmpty()) {
            Toast.makeText(
                context,
                "Enter ${transferModel.toName}'s UPI ID (e.g. gauri301998@okhdfcbank) or 10-digit phone number first",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        persistCombinedIdentity()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Always copy phone/VPA to clipboard as a backup
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipBackup = clean10Phone.ifBlank { targetVpa }
        clipboard?.setPrimaryClip(ClipData.newPlainText("UPI Recipient", clipBackup))

        postPaymentAssistHeadsUpNotification(
            context = context,
            recipientName = transferModel.toName,
            formattedAmount = transferModel.formattedDisplayAmount,
            copiedIdentifier = clipBackup,
            appName = appLabel
        )

        val cleanNote = "SplitMate · ${groupName.toSmartTitleCase()}"
        val upiUri = buildCleanP2pUpiUri(
            payeeVpa = targetVpa,
            payeeName = transferModel.toName,
            amountDecimal = transferModel.amount,
            transactionNote = cleanNote
        )

        lastLaunchedAppName = appLabel
        hasLaunchedExternalApp = true

        try {
            if (targetPackage != null) {
                val targetedIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                    setPackage(targetPackage)
                }
                if (targetedIntent.resolveActivity(context.packageManager) != null) {
                    Toast.makeText(
                        context,
                        "Opening $appLabel with ${transferModel.formattedDisplayAmount} pre-filled for $targetVpa",
                        Toast.LENGTH_SHORT
                    ).show()
                    upiResultLauncher.launch(targetedIntent)
                    return
                }
            }
            val genericIntent = Intent(Intent.ACTION_VIEW, upiUri)
            upiResultLauncher.launch(Intent.createChooser(genericIntent, "Pay ${transferModel.formattedDisplayAmount} via UPI"))
        } catch (_: Exception) {
            returnedFromUpiApp = true
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
            // 1. TACTILE RECEIPT STUB HEADER
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
                                text = "SETTLING WITH",
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
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Amount", transferModel.amount))
                            Toast.makeText(context, "Copied amount ₹${transferModel.amount}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = SplitMateTheme.SageSurface,
                        border = BorderStroke(1.dp, SplitMateTheme.AccentSage)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "EXACT SHARE",
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

            // POST-RETURN AUTO-SETTLE CONFIRMATION CARD (Triggers automatically on ON_RESUME after visiting GPay/PhonePe)
            AnimatedVisibility(
                visible = returnedFromUpiApp,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SplitMateTheme.SageSurface,
                    border = BorderStroke(1.5.dp, SplitMateTheme.SageText.copy(alpha = 0.45f)),
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
                                text = "Welcome back from $lastLaunchedAppName! Did your ${transferModel.formattedDisplayAmount} payment complete?",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                persistCombinedIdentity()
                                onMarkSettled()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitMateTheme.PrimaryDark,
                                contentColor = SplitMateTheme.ScreenBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✓ Yes, Mark ${transferModel.formattedDisplayAmount} to ${transferModel.toName} Paid",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 2. FIELD A: 10-DIGIT MOBILE NUMBER (For Google Pay / PhonePe Native Search Resolution)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. ${transferModel.toName.uppercase(Locale.US)}'S 10-DIGIT PHONE NUMBER",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
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
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        val cleaned = cleanIndianTenDigitPhone(it)
                        if (cleaned.length == 10) {
                            persistCombinedIdentity(newPhone = cleaned)
                        }
                    },
                    placeholder = {
                        Text(
                            text = "10-digit mobile (e.g. 9876543210)",
                            fontFamily = FigtreeFontFamily,
                            fontSize = 14.sp,
                            color = SplitMateTheme.TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (clean10Phone.length == 10) {
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Phone", clean10Phone))
                                    Toast.makeText(context, "Copied $clean10Phone", Toast.LENGTH_SHORT).show()
                                },
                                shape = CircleShape,
                                color = SplitMateTheme.SageSurface
                            ) {
                                Text(
                                    text = "📋 Copy #",
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SplitMateTheme.SageText,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. FIELD B: EXACT GMAIL / BANK UPI ID (e.g. gauri301998@okhdfcbank)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "2. EXACT UPI ID (OPTIONAL · e.g. gauri301998@okhdfcbank)",
                    fontFamily = FigtreeFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    color = SplitMateTheme.TextSecondary
                )

                OutlinedTextField(
                    value = exactVpaInput,
                    onValueChange = {
                        exactVpaInput = it
                        if (it.contains("@") && it.substringAfter("@").isNotBlank()) {
                            persistCombinedIdentity(newVpa = it.trim())
                        }
                    },
                    placeholder = {
                        Text(
                            text = "e.g. ${transferModel.toName.lowercase(Locale.US).replace(" ", "")}@okhdfcbank",
                            fontFamily = FigtreeFontFamily,
                            fontSize = 14.sp,
                            color = SplitMateTheme.TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AlternateEmail,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (cleanExactVpa.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = SplitMateTheme.SageSurface
                            ) {
                                Text(
                                    text = "✓ Saved",
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

                // Quick suffix helper chips to append @okhdfcbank, @okaxis, @ybl, etc.
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(QuickVpaBankSuffixes) { (suffix, label) ->
                        val isCurrentSuffix = exactVpaInput.endsWith(suffix, ignoreCase = true)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val basePrefix = exactVpaInput.substringBefore("@").trim()
                                    .ifBlank {
                                        if (suffix == "@ybl" || suffix == "@paytm" || suffix == "@upi" || suffix == "@ibl" || suffix == "@apl") {
                                            clean10Phone
                                        } else {
                                            transferModel.toName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                                        }
                                    }
                                val newVpa = "$basePrefix$suffix"
                                exactVpaInput = newVpa
                                persistCombinedIdentity(newVpa = newVpa)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentSuffix) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentSuffix) SplitMateTheme.PrimaryDark else SplitMateTheme.BorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = suffix,
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCurrentSuffix) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                                Text(
                                    text = "· $label",
                                    fontFamily = FigtreeFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrentSuffix) SplitMateTheme.ScreenBg.copy(alpha = 0.8f) else SplitMateTheme.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. GOOGLE PAY SMART NATIVE LAUNCH (Bypasses "Cannot pay with this QR" & resolves Gmail VPA from 10-Digit Phone!)
            Surface(
                onClick = {
                    launchSmartNativeUpiApp(
                        packageName = "com.google.android.apps.nbu.paisa.user",
                        appLabel = "Google Pay",
                        preferPhoneInSearch = clean10Phone.length == 10
                    )
                },
                shape = RoundedCornerShape(20.dp),
                color = if (SplitMateTheme.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.5.dp, if (SplitMateTheme.isDark) Color(0xFF38BDF8) else Color(0xFF3B82F6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Open Google Pay (Smart Phone/ID Search)",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp,
                                color = if (SplitMateTheme.isDark) Color(0xFFE0F2FE) else Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "Auto-copies ${clean10Phone.ifBlank { cleanExactVpa.ifBlank { "Phone / UPI ID" } }} + pins ${transferModel.formattedDisplayAmount} at top · Zero 'QR blocked' errors!",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.5.sp,
                                color = if (SplitMateTheme.isDark) Color(0xFFBAE6FD) else Color(0xFF1D4ED8)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ArrowOutward,
                        contentDescription = null,
                        tint = if (SplitMateTheme.isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 5. PHONEPE / PAYTM / BHIM DIRECT PRE-FILLED INTENT & NATIVE LAUNCH ROW
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OR PAY VIA PHONEPE / PAYTM / BHIM (${transferModel.formattedDisplayAmount} PRE-FILLED)",
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
                    Surface(
                        onClick = {
                            launchPreFilledUpiIntent(
                                targetPackage = "com.phonepe.app",
                                appLabel = "PhonePe",
                                fallbackSuffix = "@ybl"
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "PhonePe",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = cleanExactVpa.ifBlank { if (clean10Phone.length == 10) "$clean10Phone@ybl" else "Pre-fills ₹" },
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            launchPreFilledUpiIntent(
                                targetPackage = "net.one97.paytm",
                                appLabel = "Paytm",
                                fallbackSuffix = "@paytm"
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Paytm UPI",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = cleanExactVpa.ifBlank { if (clean10Phone.length == 10) "$clean10Phone@paytm" else "Pre-fills ₹" },
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            launchPreFilledUpiIntent(
                                targetPackage = null,
                                appLabel = "BHIM / UPI",
                                fallbackSuffix = "@upi"
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = SplitMateTheme.SurfaceWhite,
                        border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "BHIM / Any",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = cleanExactVpa.ifBlank { "Pre-filled URI" },
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. ONE-TAP SETTLEMENT BUTTON
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    persistCombinedIdentity()
                    onMarkSettled()
                    onDismiss()
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitMateTheme.PrimaryDark,
                    contentColor = SplitMateTheme.ScreenBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✓ Mark ${transferModel.formattedDisplayAmount} Paid to ${transferModel.toName}",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}
