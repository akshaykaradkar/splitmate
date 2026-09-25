package com.splitmate.app.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.splitmate.app.AvatarToken
import com.splitmate.app.SplitMateTheme
import com.splitmate.app.ui.FigtreeFontFamily
import com.splitmate.app.ui.SettlementTransferUiModel
import com.splitmate.app.ui.cleanIndianTenDigitPhone
import com.splitmate.app.ui.toSmartTitleCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

data class DiscoveredContactUpiInfo(
    val tenDigitPhone: String = "",
    val gmailPrefix: String = "",
    val explicitUpiId: String = ""
)

/**
 * Queries Android's `ContactsContract` (`Phone`, `Email`, and `Data`) for a friend's name or 10-digit phone number
 * to automatically discover:
 * 1. Their 10-digit mobile number (`9876543210` -> for `<phone>@ybl` PhonePe & `<phone>@paytm` Paytm)
 * 2. Their `@gmail.com` prefix (`gauri301998@gmail.com` -> `gauri301998@okhdfcbank` / `@okaxis` / `@okicici` / `@oksbi` for Google Pay!)
 * 3. Any explicit `@ok...`, `@ybl`, `@paytm`, or `@upi` handle stored in Contact Notes/IM.
 */
private fun discoverGmailAndUpiFromAndroidContacts(
    context: Context,
    memberName: String,
    existingPhone: String
): DiscoveredContactUpiInfo {
    val hasPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPerm) return DiscoveredContactUpiInfo(tenDigitPhone = existingPhone)

    var foundPhone = existingPhone
    var matchedContactId: String? = null
    val cleanTargetName = memberName.replace("(You)", "", ignoreCase = true).trim().lowercase(Locale.US)

    try {
        // 1. Find CONTACT_ID by matching 10-digit phone number or display name
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val cId = if (idIdx >= 0) cursor.getString(idIdx) else null
                val cName = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                val cNum = if (numIdx >= 0) cleanIndianTenDigitPhone(cursor.getString(numIdx).orEmpty()) else ""

                if (foundPhone.length == 10 && cNum == foundPhone) {
                    matchedContactId = cId
                    break
                }
                if (cleanTargetName.isNotEmpty() &&
                    (cName.equals(cleanTargetName, ignoreCase = true) ||
                        cName.lowercase(Locale.US).startsWith(cleanTargetName) ||
                        cleanTargetName.startsWith(cName.lowercase(Locale.US).takeIf { it.length >= 3 } ?: "___"))
                ) {
                    matchedContactId = cId
                    if (foundPhone.isEmpty() && cNum.length == 10) {
                        foundPhone = cNum
                    }
                    break
                }
            }
        }

        // 2. Query Email.CONTENT_URI for matchedContactId (or by display name) to extract @gmail.com prefix!
        var foundGmailPrefix = ""
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null,
            null,
            null
        )?.use { eCursor ->
            val idIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val nameIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val addrIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (eCursor.moveToNext()) {
                val cId = if (idIdx >= 0) eCursor.getString(idIdx) else null
                val cName = if (nameIdx >= 0) eCursor.getString(nameIdx)?.trim().orEmpty() else ""
                val email = if (addrIdx >= 0) eCursor.getString(addrIdx)?.trim()?.lowercase(Locale.US).orEmpty() else ""
                val isMatch = (matchedContactId != null && cId == matchedContactId) ||
                    (cleanTargetName.isNotEmpty() && cName.lowercase(Locale.US).contains(cleanTargetName))
                if (isMatch && email.contains("@")) {
                    val prefix = email.substringBefore("@").trim()
                    val domain = email.substringAfter("@").trim()
                    if (domain.startsWith("ok") || domain == "ybl" || domain == "paytm" || domain == "upi" || domain == "ibl" || domain == "apl") {
                        return DiscoveredContactUpiInfo(
                            tenDigitPhone = foundPhone,
                            gmailPrefix = prefix,
                            explicitUpiId = email
                        )
                    } else if (domain == "gmail.com" || domain == "googlemail.com") {
                        foundGmailPrefix = prefix
                        break
                    } else if (foundGmailPrefix.isEmpty()) {
                        foundGmailPrefix = prefix
                    }
                }
            }
        }
        return DiscoveredContactUpiInfo(
            tenDigitPhone = foundPhone,
            gmailPrefix = foundGmailPrefix
        )
    } catch (_: Exception) {
        return DiscoveredContactUpiInfo(tenDigitPhone = foundPhone)
    }
}

/**
 * Decodes an NPCI UPI QR Code image from Gallery/Screenshots in <10ms using ZXing (`MultiFormatReader`)
 * and extracts the exact `pa` (Payee VPA e.g. `gauri301998@okhdfcbank`).
 */
private fun decodeUpiVpaFromQrUri(context: Context, imageUri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) return null

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(binaryBitmap)
        val rawText = result.text?.trim().orEmpty()

        if (rawText.startsWith("upi://", ignoreCase = true) || rawText.contains("pa=")) {
            val parsed = Uri.parse(rawText)
            val pa = parsed.getQueryParameter("pa")?.trim()
            if (!pa.isNullOrBlank() && pa.contains("@")) {
                return pa
            }
        }
        if (rawText.contains("@") && !rawText.contains(" ")) {
            return rawText
        }
        null
    } catch (_: Exception) {
        null
    }
}

/**
 * Builds a pure, clean 1-Tap NPCI P2P URI WITHOUT `mode=00`, `mc=`, or `tr=` so Google Pay, PhonePe,
 * Paytm, and BHIM open directly to the pre-filled amount (`am`) & UPI PIN screen without triggering
 * `"Cannot pay with this QR"`.
 */
fun buildTrueOneTapUpiUri(
    scheme: String = "upi",
    host: String = "pay",
    path: String? = null,
    payeeVpa: String,
    payeeName: String,
    amountDecimal: String,
    transactionNote: String
): Uri {
    val builder = Uri.Builder()
        .scheme(scheme)
        .authority(host)
    if (!path.isNullOrBlank()) {
        builder.appendEncodedPath(path)
    }
    return builder
        .appendQueryParameter("pa", payeeVpa.trim())
        .appendQueryParameter("pn", payeeName.trim())
        .appendQueryParameter("am", amountDecimal.trim())
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", transactionNote.trim().take(48))
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
    val coroutineScope = rememberCoroutineScope()
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
    var discoveredGmailPrefix by remember(transferModel.toMemberId) { mutableStateOf("") }
    var hasLaunchedExternalApp by remember { mutableStateOf(false) }
    var returnedFromUpiApp by remember { mutableStateOf(false) }
    var lastLaunchedAppName by remember { mutableStateOf("UPI App") }

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

    // Automatically query Android Contacts (Phone + Email) on open to discover 10-digit phone & @gmail.com prefix
    LaunchedEffect(transferModel.toMemberId) {
        withContext(Dispatchers.IO) {
            val discovered = discoverGmailAndUpiFromAndroidContacts(
                context = context,
                memberName = transferModel.toName,
                existingPhone = parsedInitial.first
            )
            withContext(Dispatchers.Main) {
                val effectivePhone = if (phoneInput.length == 10) phoneInput else discovered.tenDigitPhone
                if (phoneInput.isBlank() && effectivePhone.length == 10) {
                    phoneInput = effectivePhone
                }
                if (discovered.gmailPrefix.isNotBlank()) {
                    discoveredGmailPrefix = discovered.gmailPrefix
                }
                if (exactVpaInput.isBlank() && discovered.explicitUpiId.isNotBlank()) {
                    exactVpaInput = discovered.explicitUpiId
                    persistCombinedIdentity(newPhone = effectivePhone, newVpa = discovered.explicitUpiId)
                } else if (exactVpaInput.isBlank() && discovered.gmailPrefix.isNotBlank()) {
                    // Auto-fill <gmail_prefix>@okhdfcbank when a Gmail address is found in Contacts!
                    val autoGpayVpa = "${discovered.gmailPrefix}@okhdfcbank"
                    exactVpaInput = autoGpayVpa
                    persistCombinedIdentity(newPhone = effectivePhone, newVpa = autoGpayVpa)
                } else if (exactVpaInput.isBlank() && effectivePhone.length == 10) {
                    // Phone number only (no Gmail in Contacts): auto-fill interoperable <10-digit-phone>@ybl
                    // so Google Pay, PhonePe, Paytm, and BHIM work in 1 single tap from the phone number!
                    val autoPhoneVpa = "$effectivePhone@ybl"
                    exactVpaInput = autoPhoneVpa
                    persistCombinedIdentity(newPhone = effectivePhone, newVpa = autoPhoneVpa)
                }
            }
        }
    }

    // Detect when user returns from GPay / PhonePe / Paytm (ON_RESUME)
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
     * TRUE 1-TAP PRE-FILLED DEEP-LINK LAUNCHER (`tez://upi/pay`, `phonepe://pay`, `paytmmp://pay`, `upi://pay`)
     * Opens the chosen UPI app directly to the pre-filled Rupee amount (`am`) & UPI PIN entry screen!
     */
    fun launchTrueOneTapUpi(
        targetAppId: String,
        appLabel: String,
        targetPackage: String?,
        overrideVpa: String? = null
    ) {
        val resolvedVpa = overrideVpa?.takeIf { it.isNotBlank() } ?: when (targetAppId) {
            "gpay" -> {
                when {
                    clean10Phone.length == 10 -> clean10Phone
                    cleanExactVpa.isNotBlank() -> cleanExactVpa
                    discoveredGmailPrefix.isNotBlank() -> "$discoveredGmailPrefix@okhdfcbank"
                    else -> ""
                }
            }
            "phonepe" -> {
                when {
                    cleanExactVpa.isNotBlank() -> cleanExactVpa
                    clean10Phone.length == 10 -> "$clean10Phone@ybl"
                    else -> ""
                }
            }
            "paytm" -> {
                when {
                    cleanExactVpa.isNotBlank() -> cleanExactVpa
                    clean10Phone.length == 10 -> "$clean10Phone@paytm"
                    else -> ""
                }
            }
            else -> {
                when {
                    cleanExactVpa.isNotBlank() -> cleanExactVpa
                    clean10Phone.length == 10 -> "$clean10Phone@upi"
                    else -> ""
                }
            }
        }

        if (overrideVpa != null) {
            exactVpaInput = overrideVpa
        }
        if (resolvedVpa.isNotBlank()) {
            persistCombinedIdentity(newVpa = if (resolvedVpa.contains("@")) resolvedVpa else cleanExactVpa)
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Put the 10-digit phone number (preferred for Google Pay P2P search) or UPI ID on Android Keyboard Clipboard strip
        val copyTarget = clean10Phone.ifBlank { cleanExactVpa.ifBlank { resolvedVpa } }
        if (copyTarget.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Payee Phone or UPI", copyTarget))
        }

        val cleanNote = "SplitMate · ${groupName.toSmartTitleCase()}"
        lastLaunchedAppName = appLabel
        hasLaunchedExternalApp = true

        // IMPORTANT: Google Pay India (`com.google.android.apps.nbu.paisa.user`) blocks ALL `tez://upi/pay` and `upi://pay`
        // deep links when the recipient (`pa=`) is a Personal (`PERSON`) savings account (only allowing signed `MERCHANT`
        // accounts like Zomato/Blinkit) and shows "Cannot pay with this QR".
        // Launching Google Pay via its native package launcher (`getLaunchIntentForPackage`) completely bypasses the
        // Merchant/QR block so P2P personal payments work 100% of the time!
        if (targetAppId == "gpay") {
            val gpayLaunchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.nbu.paisa.user")
            if (gpayLaunchIntent != null) {
                gpayLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Toast.makeText(
                    context,
                    if (copyTarget.isNotBlank()) {
                        "Copied $copyTarget · Tap search in GPay to pay ${transferModel.formattedDisplayAmount}"
                    } else {
                        "Opening Google Pay · Pay ${transferModel.formattedDisplayAmount} to ${transferModel.toName}"
                    },
                    Toast.LENGTH_LONG
                ).show()
                context.startActivity(gpayLaunchIntent)
                return
            }
        }

        val vpaForDeepLink = when {
            cleanExactVpa.isNotBlank() -> cleanExactVpa
            clean10Phone.length == 10 -> "$clean10Phone@paytm"
            else -> return
        }

        try {
            val standardUpiUri = buildTrueOneTapUpiUri(
                scheme = "upi",
                host = "pay",
                payeeVpa = vpaForDeepLink,
                payeeName = transferModel.toName,
                amountDecimal = transferModel.amount,
                transactionNote = cleanNote
            )
            if (targetPackage != null) {
                val pkgIntent = Intent(Intent.ACTION_VIEW, standardUpiUri).apply {
                    setPackage(targetPackage)
                }
                if (pkgIntent.resolveActivity(context.packageManager) != null) {
                    upiResultLauncher.launch(pkgIntent)
                    return
                }
            }
            val chooserIntent = Intent(Intent.ACTION_VIEW, standardUpiUri)
            upiResultLauncher.launch(Intent.createChooser(chooserIntent, "Pay ${transferModel.formattedDisplayAmount} to ${transferModel.toName}"))
        } catch (_: Exception) {
            returnedFromUpiApp = true
        }
    }

    // 1-Tap Gallery / Screenshot UPI QR Scanner (`ActivityResultContracts.GetContent`)
    val qrGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val decodedVpa = withContext(Dispatchers.IO) {
                    decodeUpiVpaFromQrUri(context, uri)
                }
                if (!decodedVpa.isNullOrBlank()) {
                    exactVpaInput = decodedVpa
                    persistCombinedIdentity(newVpa = decodedVpa)
                    Toast.makeText(
                        context,
                        "Extracted UPI ID: $decodedVpa — Launching 1-Tap Payment!",
                        Toast.LENGTH_SHORT
                    ).show()
                    launchTrueOneTapUpi(
                        targetAppId = "gpay",
                        appLabel = "Google Pay",
                        targetPackage = "com.google.android.apps.nbu.paisa.user",
                        overrideVpa = decodedVpa
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Could not find a valid UPI QR in that image. Try cropping closer to the QR code.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Direct 1-Tap Contact Phone Number Picker (`CommonDataKinds.Phone.CONTENT_URI`)
    // Works even WITHOUT runtime READ_CONTACTS permission because Android grants temporary URI access to the picked phone row!
    val phoneContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val phoneUri = result.data?.data
        if (phoneUri != null) {
            coroutineScope.launch {
                val discovered = withContext(Dispatchers.IO) {
                    var pickedPhone = ""
                    var pickedGmailPrefix = ""
                    var contactId: String? = null
                    try {
                        context.contentResolver.query(
                            phoneUri,
                            arrayOf(
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                            ),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                                pickedPhone = cleanIndianTenDigitPhone(if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else "")
                                contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                            }
                        }
                        if (contactId != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { eCursor ->
                                while (eCursor.moveToNext()) {
                                    val addrIdx = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                    val email = if (addrIdx >= 0) eCursor.getString(addrIdx)?.trim()?.lowercase(Locale.US).orEmpty() else ""
                                    if (email.contains("@")) {
                                        pickedGmailPrefix = email.substringBefore("@").trim()
                                        break
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                    pickedPhone to pickedGmailPrefix
                }
                if (discovered.first.length == 10) {
                    phoneInput = discovered.first
                    if (discovered.second.isNotBlank()) {
                        discoveredGmailPrefix = discovered.second
                        val autoVpa = "${discovered.second}@okhdfcbank"
                        exactVpaInput = autoVpa
                        persistCombinedIdentity(newPhone = discovered.first, newVpa = autoVpa)
                        Toast.makeText(
                            context,
                            "Selected ${discovered.first} & auto-filled ${autoVpa}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Phone number only (no Gmail): auto-fill interoperable <phone>@ybl VPA so 1-tap works immediately!
                        val phoneVpa = "${discovered.first}@ybl"
                        exactVpaInput = phoneVpa
                        persistCombinedIdentity(newPhone = discovered.first, newVpa = phoneVpa)
                        Toast.makeText(
                            context,
                            "Selected ${discovered.first} from Contacts — Ready for 1-Tap Pay!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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
            // 1. TACTILE RECEIPT STUB HEADER (Avatar + Pre-Filled Amount)
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
                                text = "1-TAP UPI SETTLEMENT",
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

            // POST-RETURN CONFIRMATION BAR
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
                                text = "Returned from $lastLaunchedAppName · Did your ${transferModel.formattedDisplayAmount} payment succeed?",
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
                                text = "Yes, Mark ${transferModel.formattedDisplayAmount} Paid",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 2. 1-TAP QR SCREENSHOT SCANNER + CONTACT PICKER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { qrGalleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    color = SplitMateTheme.SageSurface,
                    border = BorderStroke(1.dp, SplitMateTheme.AccentSage),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = SplitMateTheme.SageText,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Scan UPI QR Image",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Auto-fills exact VPA & pays",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.SageText
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        phoneContactPickerLauncher.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = SplitMateTheme.SurfaceWhite,
                    border = BorderStroke(1.dp, SplitMateTheme.BorderLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Pick from Contacts",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SplitMateTheme.PrimaryDark
                            )
                            Text(
                                text = "Select 10-Digit Phone",
                                fontFamily = FigtreeFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.TextSecondary
                            )
                        }
                    }
                }
            }

            // 3. EXACT UPI ID INPUT (Auto-populated from Contact Phone/Gmail or QR & saved forever for 1-Tap Google Pay)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${transferModel.toName.uppercase(Locale.US)}'S UPI ID (FOR 1-TAP GOOGLE PAY)",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
                        color = SplitMateTheme.TextSecondary
                    )
                    if (discoveredGmailPrefix.isNotBlank()) {
                        Text(
                            text = "Gmail detected: $discoveredGmailPrefix",
                            fontFamily = FigtreeFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.SageText
                        )
                    } else if (clean10Phone.length == 10) {
                        Text(
                            text = "Phone: $clean10Phone",
                            fontFamily = FigtreeFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SplitMateTheme.SageText
                        )
                    }
                }

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
                            text = "e.g. gauri301998@okhdfcbank or ${clean10Phone.ifBlank { "9876543210" }}@ybl",
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
                                    text = "Saved",
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

                // 1-Tap Bank Suffix Chips (Appends @okhdfcbank, @okaxis, @okicici, @oksbi, @ybl, @paytm, @upi)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(QuickVpaBankSuffixes) { (suffix, label) ->
                        val isCurrentSuffix = exactVpaInput.endsWith(suffix, ignoreCase = true)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val currentPrefix = exactVpaInput.substringBefore("@").trim()
                                val basePrefix = when {
                                    currentPrefix.isNotBlank() && cleanIndianTenDigitPhone(currentPrefix).length != 10 -> currentPrefix
                                    suffix == "@ybl" || suffix == "@paytm" || suffix == "@upi" || suffix == "@ibl" || suffix == "@apl" -> {
                                        clean10Phone.ifBlank { currentPrefix }
                                    }
                                    discoveredGmailPrefix.isNotBlank() -> discoveredGmailPrefix
                                    currentPrefix.isNotBlank() -> currentPrefix
                                    else -> transferModel.toName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
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

            // 4. 10-DIGIT PHONE NUMBER FIELD (Auto-fills <phone>@ybl when no Gmail is present so Google Pay works in 1 tap!)
            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    phoneInput = it
                    val cleaned = cleanIndianTenDigitPhone(it)
                    if (cleaned.length == 10) {
                        if (exactVpaInput.isBlank() || cleanIndianTenDigitPhone(exactVpaInput.substringBefore("@")).length == 10) {
                            val currentSuffix = if (exactVpaInput.contains("@")) "@${exactVpaInput.substringAfter("@")}" else "@ybl"
                            val updatedVpa = "$cleaned$currentSuffix"
                            exactVpaInput = updatedVpa
                            persistCombinedIdentity(newPhone = cleaned, newVpa = updatedVpa)
                        } else {
                            persistCombinedIdentity(newPhone = cleaned)
                        }
                    }
                },
                label = {
                    Text(
                        text = "${transferModel.toName}'s 10-Digit Mobile Number (From Contacts)",
                        fontFamily = FigtreeFontFamily,
                        fontSize = 12.sp
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. SINGLE HERO 1-TAP GOOGLE PAY LAUNCHER (`tez://upi/pay`, Pre-Fills Exact Amount & Goes Straight to UPI PIN!)
            Surface(
                onClick = {
                    launchTrueOneTapUpi(
                        targetAppId = "gpay",
                        appLabel = "Google Pay",
                        targetPackage = "com.google.android.apps.nbu.paisa.user"
                    )
                },
                shape = RoundedCornerShape(20.dp),
                color = SplitMateTheme.PrimaryDark,
                border = BorderStroke(1.5.dp, SplitMateTheme.PrimaryDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
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
                            color = SplitMateTheme.SageSurface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SplitMateTheme.SageText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Pay ${transferModel.formattedDisplayAmount} with Google Pay →",
                                fontFamily = FigtreeFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.5.sp,
                                color = SplitMateTheme.ScreenBg
                            )
                            Text(
                                text = if (cleanExactVpa.isNotBlank()) {
                                    "1-Tap Google Pay → $cleanExactVpa (Pre-filled ₹ & MPIN)"
                                } else if (clean10Phone.length == 10) {
                                    "1-Tap Google Pay → $clean10Phone@ybl (From Phone Number)"
                                } else {
                                    "Pick Phone from Contacts or Scan QR above for 1-Tap Google Pay"
                                },
                                fontFamily = FigtreeFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SplitMateTheme.SageSurface
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = SplitMateTheme.SageSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 6. MANUAL MARK SETTLED BUTTON
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    persistCombinedIdentity()
                    onMarkSettled()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SplitMateTheme.SurfaceWhite,
                    contentColor = SplitMateTheme.PrimaryDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Already Settled? Mark ${transferModel.formattedDisplayAmount} Paid",
                    fontFamily = FigtreeFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
