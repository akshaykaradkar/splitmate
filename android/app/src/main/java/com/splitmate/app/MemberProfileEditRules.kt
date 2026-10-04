package com.splitmate.app

import com.splitmate.app.data.PhoneIdentityValidator
import com.splitmate.app.ui.AvatarGender
import com.splitmate.app.ui.AvatarSeedCodec

/**
 * v2.3.5 (issue #7): pure, Android-free rules behind the "Edit Group Members" dialog.
 *
 * - Gender pills use the exact ids that [AvatarSeedCodec] stores (`Male` / `Female` / `Neutral`),
 *   so a saved member re-opens with the right pill selected (RC2). Legacy `Masculine` / `Feminine`
 *   tokens still normalise through [AvatarGender.fromId].
 * - [resolveUpiId] only derives `<phone>@upi` when the member has no real VPA. A real VPA such as
 *   `rohan@okaxis` is never overwritten by a phone edit.
 * - [resolveMemberAvatarSeed] keeps the existing seed key / style / colour preset and only swaps the
 *   gender (and the seed key when the name changes), so existing seeds like
 *   `Gaurii|Female|open-peeps|PastelWall` stay compatible.
 */
object MemberProfileEditRules {

    /** Compact M / F / N pills: (stored gender id, short code). */
    val GENDER_PILLS: List<Pair<String, String>> = listOf(
        AvatarGender.MALE.id to "M",
        AvatarGender.FEMALE.id to "F",
        AvatarGender.NEUTRAL.id to "N"
    )

    /** Full-width segmented control: (stored gender id, visible label). */
    val GENDER_SEGMENTS: List<Pair<String, String>> = AvatarGender.values().map { it.id to it.label }

    private val DERIVED_PHONE_UPI = Regex("^\\+?\\d{10,12}@upi$", RegexOption.IGNORE_CASE)

    fun normalizeGenderId(raw: String?): String = AvatarGender.fromId(raw).id

    fun isGenderSelected(draftGenderId: String?, pillGenderId: String): Boolean =
        normalizeGenderId(draftGenderId) == normalizeGenderId(pillGenderId)

    /** Gender id the dialog should pre-select for a stored member. */
    fun initialGenderIdOf(avatarSeed: String, name: String): String =
        AvatarSeedCodec.parse(avatarSeed.ifBlank { name }).gender.id

    /** True for an auto-derived `<phone>@upi` handle (not a VPA the user typed). */
    fun isDerivedPhoneUpi(upiId: String?): Boolean =
        DERIVED_PHONE_UPI.matches(upiId?.trim().orEmpty())

    /** True when [upiId] is a real, user-provided VPA that must be preserved. */
    fun isRealVpa(upiId: String?): Boolean {
        val clean = upiId?.trim().orEmpty()
        return clean.contains('@') && !isDerivedPhoneUpi(clean)
    }

    /** Value shown in the editable "UPI ID" field: only real VPAs; derived handles show blank. */
    fun editableVpaOf(upiId: String?): String = if (isRealVpa(upiId)) upiId!!.trim() else ""

    /**
     * Resolves the `upiId` to persist for a member.
     *
     * @param existingUpiId the member's currently stored `upiId`.
     * @param phoneInput the 10-digit mobile field (a VPA typed here is still honoured, legacy behaviour).
     * @param vpaInput the "UPI ID" field when the user edited it; `null` means untouched.
     *   A non-null value without `@` (e.g. blank) means the user cleared the VPA.
     */
    fun resolveUpiId(existingUpiId: String, phoneInput: String, vpaInput: String? = null): String {
        val vpa = vpaInput?.trim()
        if (vpa != null && vpa.contains('@')) return vpa

        val rawPhone = phoneInput.trim()
        if (rawPhone.contains('@')) return rawPhone

        val phone10 = PhoneIdentityValidator.extractMemberPhone10(rawPhone, rawPhone)
        val derived = if (phone10.length == 10) "${phone10}@upi" else ""
        val existing = existingUpiId.trim()
        val userClearedVpa = vpaInput != null

        return if (userClearedVpa || !isRealVpa(existing)) derived else existing
    }

    /** Builds the canonical 4-token avatar seed for a member edit (style + preset preserved). */
    fun resolveMemberAvatarSeed(
        existingSeed: String,
        existingName: String,
        draftName: String,
        genderId: String
    ): String {
        val cleanName = draftName.trim().ifEmpty { existingName }
        val baseDesc = AvatarSeedCodec.parse(existingSeed.ifBlank { existingName })
        val resolvedSeedKey = if (cleanName == existingName.trim() && baseDesc.seedKey.isNotBlank()) {
            baseDesc.seedKey
        } else {
            cleanName
        }
        return AvatarSeedCodec.encode(
            seedKey = resolvedSeedKey,
            gender = AvatarGender.fromId(genderId),
            styleId = baseDesc.styleId,
            colorPresetId = baseDesc.colorPresetId
        )
    }
}
