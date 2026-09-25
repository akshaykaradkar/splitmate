package com.splitmate.app

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.splitmate.app.data.ExpenseEntity
import com.splitmate.app.data.GroupMemberEntity
import com.splitmate.app.ui.ParsedTravelTicket
import com.splitmate.app.ui.buildDiceBearOpenPeepsUrl
import com.splitmate.app.ui.extractInitialsFromNameOrSeed
import com.splitmate.app.ui.extractTravelTicketFromTitle
import com.splitmate.app.ui.formatTravelExpenseTitle
import java.util.Locale

@Composable
fun EditLoggedExpenseDialog(
    expense: ExpenseEntity,
    groupMembers: List<GroupMemberEntity>,
    initialSplitMemberIds: Set<String> = groupMembers.map { it.memberId }.toSet(),
    onDismiss: () -> Unit,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    val existingTicket = remember(expense.title) { extractTravelTicketFromTitle(expense.title) }
    var editedTitle by remember(expense.title) {
        mutableStateOf(existingTicket?.cleanTitle ?: expense.title)
    }
    var editedAmountStr by remember(expense.totalAmountCents) {
        val rupees = expense.totalAmountCents / 100.0
        mutableStateOf(if (rupees % 1.0 == 0.0) rupees.toLong().toString() else String.format(Locale.US, "%.2f", rupees))
    }
    var selectedPayerId by remember(expense.payerId) {
        mutableStateOf(expense.payerId)
    }
    var selectedSplitMemberIds by remember(expense.expenseId, initialSplitMemberIds) {
        mutableStateOf(initialSplitMemberIds.ifEmpty { groupMembers.map { it.memberId }.toSet() })
    }
    var includeTravelTicket by remember(existingTicket) {
        mutableStateOf(existingTicket != null)
    }
    var pnrNumber by remember(existingTicket) { mutableStateOf(existingTicket?.pnr ?: "") }
    var trainOrFlightNo by remember(existingTicket) { mutableStateOf(existingTicket?.trainOrFlightNo ?: "") }
    var routeFromTo by remember(existingTicket) { mutableStateOf(existingTicket?.route ?: "") }
    var departureInfo by remember(existingTicket) { mutableStateOf(existingTicket?.departureInfo ?: "") }
    var coachAndSeats by remember(existingTicket) { mutableStateOf(existingTicket?.coachAndSeats ?: "") }

    val quickCategories = listOf(
        Triple("Train / PNR", Icons.Rounded.Train, true),
        Triple("Dinner & Food", Icons.Rounded.Restaurant, false),
        Triple("Cab & Auto", Icons.Rounded.LocalTaxi, false),
        Triple("Hotel & Stay", Icons.Rounded.Hotel, false),
        Triple("Groceries", Icons.Rounded.ShoppingCart, false),
        Triple("Drinks & Outing", Icons.Rounded.LocalBar, false)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SplitMateTheme.SurfaceWhite,
        shape = SplitMateTheme.RadiusCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = SplitMateTheme.SageText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Logged Expense",
                    fontFamily = SplitMateTheme.FontDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = SplitMateTheme.PrimaryDark
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quick Category Switch:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplitMateTheme.TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickCategories) { (cat, catIcon, isTrainCat) ->
                        val isSelected = editedTitle.equals(cat, ignoreCase = true)
                        Surface(
                            onClick = {
                                editedTitle = cat
                                if (isTrainCat) includeTravelTicket = true
                            },
                            shape = SplitMateTheme.RadiusBadge,
                            color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = cat,
                                    tint = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Expense Title / Category", fontSize = 12.sp) },
                    singleLine = true,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editedAmountStr,
                    onValueChange = { editedAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Total Amount (₹)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = SplitMateTheme.RadiusPanel,
                    modifier = Modifier.fillMaxWidth()
                )

                if (groupMembers.isNotEmpty()) {
                    Text(
                        text = "Paid By:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groupMembers, key = { it.memberId }) { mbr ->
                            val isSelected = mbr.memberId == selectedPayerId
                            Surface(
                                onClick = { selectedPayerId = mbr.memberId },
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (isSelected) SplitMateTheme.PrimaryDark else SplitMateTheme.SurfaceMuted
                            ) {
                                Text(
                                    text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SplitMateTheme.ScreenBg else SplitMateTheme.PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Split Equally Among (${selectedSplitMemberIds.size} of ${groupMembers.size} selected):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplitMateTheme.TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groupMembers, key = { "split_${it.memberId}" }) { mbr ->
                            val isIncluded = selectedSplitMemberIds.contains(mbr.memberId)
                            Surface(
                                onClick = {
                                    selectedSplitMemberIds = if (isIncluded && selectedSplitMemberIds.size > 1) {
                                        selectedSplitMemberIds - mbr.memberId
                                    } else {
                                        selectedSplitMemberIds + mbr.memberId
                                    }
                                },
                                shape = SplitMateTheme.RadiusBadge,
                                color = if (isIncluded) SplitMateTheme.SageSurface else SplitMateTheme.SurfaceMuted
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isIncluded) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = SplitMateTheme.SageText,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (mbr.isCurrentUser) "${mbr.name} (You)" else mbr.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncluded) SplitMateTheme.SageText else SplitMateTheme.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Train,
                            contentDescription = null,
                            tint = SplitMateTheme.PrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Attach / Edit Train PNR & Berths",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SplitMateTheme.PrimaryDark
                        )
                    }
                    Switch(
                        checked = includeTravelTicket,
                        onCheckedChange = { includeTravelTicket = it }
                    )
                }

                if (includeTravelTicket) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = pnrNumber,
                            onValueChange = { pnrNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("10-Digit PNR", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trainOrFlightNo,
                            onValueChange = { trainOrFlightNo = it },
                            label = { Text("Train #", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = routeFromTo,
                            onValueChange = { routeFromTo = it },
                            label = { Text("From → To", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = coachAndSeats,
                            onValueChange = { coachAndSeats = it },
                            label = { Text("Coach / Berths", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedRupees = editedAmountStr.toDoubleOrNull() ?: (expense.totalAmountCents / 100.0)
                    val finalTitle = if (includeTravelTicket && (pnrNumber.isNotBlank() || trainOrFlightNo.isNotBlank() || coachAndSeats.isNotBlank())) {
                        val parsedFrom = routeFromTo.substringBefore("→").substringBefore("-").trim()
                        val parsedTo = routeFromTo.substringAfter("→", routeFromTo.substringAfter("-", "")).trim()
                        val parsedTicket = ParsedTravelTicket(
                            pnr = pnrNumber.trim(),
                            trainOrFlightNo = trainOrFlightNo.trim(),
                            fromStation = parsedFrom,
                            toStation = parsedTo,
                            departureTime = departureInfo.trim(),
                            coachAndSeats = coachAndSeats.trim(),
                            cleanTitle = editedTitle.trim().ifBlank { "Train / PNR Ticket" }
                        )
                        formatTravelExpenseTitle(editedTitle.trim().ifBlank { "Train / PNR Ticket" }, parsedTicket)
                    } else {
                        editedTitle.trim().ifBlank { "Group Expense" }
                    }
                    onSave(finalTitle, parsedRupees, selectedPayerId, selectedSplitMemberIds.toList())
                },
                shape = SplitMateTheme.RadiusButton,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitMateTheme.PrimaryDark,
                    contentColor = SplitMateTheme.ScreenBg
                )
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = SplitMateTheme.TextSecondary)
            }
        }
    )
}

@Composable
fun AvatarToken(
    initials: String,
    bg: Color,
    textColor: Color,
    size: Int = 36
) {
    val context = LocalContext.current
    val diceBearSvgUrl = remember(initials) {
        buildDiceBearOpenPeepsUrl(initials)
    }
    val cleanInitials = remember(initials) {
        extractInitialsFromNameOrSeed(initials)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, SplitMateTheme.SurfaceWhite, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cleanInitials,
            fontFamily = SplitMateTheme.FontDisplay,
            fontSize = (size * 0.34f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(diceBearSvgUrl)
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
fun OverlappingAvatarStack(avatars: List<String>, remainingCount: Int = 0) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        avatars.forEachIndexed { index, seed ->
            Box(modifier = Modifier.offset(x = (-index * 8).dp)) {
                AvatarToken(
                    initials = seed,
                    bg = Color(0xFFE2E8F0),
                    textColor = SplitMateTheme.PrimaryDark,
                    size = 28
                )
            }
        }
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-avatars.size * 8).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SplitMateTheme.SurfaceMuted)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$remainingCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.PrimaryDark)
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color = SplitMateTheme.PrimaryDark,
    title: String,
    subtitle: String,
    amount: String,
    isPositive: Boolean
) {
    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SplitMateTheme.BorderLight, SplitMateTheme.RadiusCard)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SplitMateTheme.PrimaryDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontFamily = SplitMateTheme.FontRounded,
                        fontSize = 12.sp,
                        color = SplitMateTheme.TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = amount,
                fontFamily = SplitMateTheme.FontDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                color = if (isPositive) SplitMateTheme.SageText else SplitMateTheme.TerracottaText
            )
        }
    }
}

@Composable
fun ClaimItemRow(
    title: String,
    amount: String,
    claimedBy: List<String>,
    @Suppress("UNUSED_PARAMETER") unclaimed: List<String>
) {
    Card(
        shape = SplitMateTheme.RadiusCard,
        colors = CardDefaults.cardColors(containerColor = SplitMateTheme.SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SplitMateTheme.PrimaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Claimed by:", fontSize = 11.sp, color = SplitMateTheme.TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (claimedBy.isEmpty()) {
                        Text("Unclaimed (Payer holds)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SplitMateTheme.TerracottaText)
                    } else {
                        claimedBy.forEach { person ->
                            Box(modifier = Modifier.padding(end = 4.dp)) {
                                AvatarToken(
                                    initials = person,
                                    bg = SplitMateTheme.AccentSage,
                                    textColor = SplitMateTheme.PrimaryDark,
                                    size = 22
                                )
                            }
                        }
                    }
                }
            }

            Text(amount, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SplitMateTheme.PrimaryDark)
        }
    }
}
