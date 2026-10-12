package com.splitmate.app

import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.data.PhoneIdentityValidator

/**
 * v2.4.0 D7: "member joined" moment. Pure, display-only: compares the members on this phone
 * before and after a refresh and returns the people who just became JOINED in a trip that
 * already existed here (they accepted an invite, or joined with the code on their own phone).
 *
 * Never reported:
 * - you (current user or your own phone number),
 * - name-only members without a phone (added on this phone; nobody "joined"),
 * - members of a trip that is new on this phone (you just joined or restored it),
 * - trips you have not joined yourself (open invite or declined),
 * - someone who was already JOINED under another row with the same phone (a row id change).
 */
data class MemberArrival(val groupId: String, val groupName: String, val memberName: String)

object MemberArrivals {

    private fun isJoined(m: GroupMemberEntity) = m.inviteStatus.equals("JOINED", ignoreCase = true)

    private fun phone10(m: GroupMemberEntity) = PhoneIdentityValidator.extractMemberPhone10(m.userPhone, m.upiId)

    fun detect(
        beforeGroupIds: Set<String>,
        beforeMembers: List<GroupMemberEntity>,
        afterMembers: List<GroupMemberEntity>,
        groupNames: Map<String, String>,
        myPhone10: String
    ): List<MemberArrival> {
        val beforeById = beforeMembers.associateBy { it.memberId }
        val joinedPhonesBefore: Set<Pair<String, String>> = beforeMembers
            .filter { isJoined(it) }
            .mapNotNull { m -> phone10(m).takeIf { it.length == 10 }?.let { m.groupId to it } }
            .toSet()
        // Review J4: only trips you have actually joined (not an open invite or a declined trip).
        val myJoinedGroupIds: Set<String> = afterMembers
            .filter { m -> isJoined(m) && (m.isCurrentUser || (myPhone10.length == 10 && phone10(m) == myPhone10)) }
            .map { it.groupId }
            .toSet()
        return afterMembers
            .asSequence()
            .filter { it.groupId in beforeGroupIds && it.groupId in myJoinedGroupIds }
            .filter { isJoined(it) && !it.isCurrentUser }
            .filter { m ->
                val p = phone10(m)
                p.length == 10 && p != myPhone10 && (m.groupId to p) !in joinedPhonesBefore
            }
            .filter { m -> beforeById[m.memberId]?.let { !isJoined(it) } ?: true }
            .map { MemberArrival(it.groupId, groupNames[it.groupId] ?: "your trip", it.name) }
            .distinctBy { it.groupId to it.memberName }
            .toList()
    }

    /** One short line per trip: "Priya joined Goa", "Priya and Sam joined Goa", "3 people joined Goa". */
    fun messages(arrivals: List<MemberArrival>): List<String> =
        arrivals.groupBy { it.groupId }.values.map { inTrip ->
            val trip = inTrip.first().groupName
            when (inTrip.size) {
                1 -> "${inTrip[0].memberName} joined $trip"
                2 -> "${inTrip[0].memberName} and ${inTrip[1].memberName} joined $trip"
                else -> "${inTrip.size} people joined $trip"
            }
        }
}
