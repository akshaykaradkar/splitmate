package com.splitmate.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.security.MessageDigest
import java.security.SecureRandom

object PhoneIdentityValidator {
    fun normalizeIndianPhone10(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            digits.length == 10 -> digits
            digits.length > 10 -> digits.takeLast(10)
            else -> ""
        }
    }

    fun isValidIndianMobile10(phone10: String): Boolean {
        return phone10.length == 10 && phone10[0] in '6'..'9' && phone10.all { it.isDigit() }
    }
}

data class OtpDispatchResult(
    val phone10: String,
    val generatedOtpCode: String,
    val expiresAtEpochMs: Long,
    val notificationDispatched: Boolean
)

object PhoneOtpAuthManager {
    private val random = SecureRandom()
    
    // In-memory pending OTP
    private var pendingPhone10: String? = null
    private var pendingOtpHash: String? = null
    private var pendingExpiresAt: Long = 0L

    fun sendOtp(context: Context?, rawPhone: String): OtpDispatchResult? {
        val phone10 = PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        if (!PhoneIdentityValidator.isValidIndianMobile10(phone10)) return null

        val code = (100000 + random.nextInt(900000)).toString()
        val now = System.currentTimeMillis()
        val expiresAt = now + 5 * 60_000L

        val hash = sha256("$phone10:$code")

        pendingPhone10 = phone10
        pendingOtpHash = hash
        pendingExpiresAt = expiresAt

        if (context != null) {
            val prefs = context.getSharedPreferences("splitmate_otp_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("pending_phone10", phone10)
                .putString("pending_otp_hash", hash)
                .putLong("pending_expires_at", expiresAt)
                .apply()
        }

        var notifDispatched = false
        if (context != null) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "splitmate_otp_auth_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "OTP Verification",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("SplitMate OTP")
                    .setContentText("Your verification code is $code. Valid for 5 minutes.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(phone10.hashCode(), notification)
                notifDispatched = true
            } catch (e: SecurityException) {
                // Missing POST_NOTIFICATIONS on Android 13+
            }
        }

        return OtpDispatchResult(phone10, code, expiresAt, notifDispatched)
    }

    fun verifyOtp(rawPhone: String, enteredCode: String, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val phone10 = PhoneIdentityValidator.normalizeIndianPhone10(rawPhone)
        if (phone10.isEmpty()) return false

        // Check memory first, then prefs (we skip prefs check in this pure static manager for simplicity unless requested, 
        // wait, we did save it to prefs, we should probably just check memory since this is a singleton 
        // and if it's killed we can't get context here anyway. I'll just check memory.)
        if (phone10 != pendingPhone10) return false
        if (nowEpochMs > pendingExpiresAt) return false

        val expectedHash = pendingOtpHash ?: return false
        val actualHash = sha256("$phone10:$enteredCode")

        if (MessageDigest.isEqual(expectedHash.toByteArray(), actualHash.toByteArray())) {
            pendingPhone10 = null
            pendingOtpHash = null
            pendingExpiresAt = 0L
            return true
        }
        return false
    }

    fun hashPin(phone10: String, pin4: String): String {
        if (pin4.isBlank()) return ""
        return sha256("splitmate_pin_v1:$phone10:${pin4.trim()}")
    }

    fun verifyPin(phone10: String, enteredPin: String, storedPinHash: String): Boolean {
        val actualHash = hashPin(phone10, enteredPin)
        if (actualHash.isEmpty() || storedPinHash.isEmpty()) return false
        return MessageDigest.isEqual(storedPinHash.toByteArray(), actualHash.toByteArray())
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
