package com.splitmate.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.splitmate.app.SplitMateTheme

// ==============================================================================
// 1. MATERIAL 3 EXPRESSIVE LUMINOUS TRANSIT TOKENS
// ==============================================================================
object AnimatedTransitDeckTokens {
    // Mode 1: Train PNR Pass Tokens (Fresh Botanical Sage & Lustrous Forest)
    val SageForestStart = Color(0xFF32571F)        // Luminous Olive Forest
    val SageForestEnd = Color(0xFF223E13)          // Shadowed Botanical Forest
    val SagePillBg = Color(0xFFEAF5DC)             // Airy Sage Action Surface
    val SagePillText = Color(0xFF254212)           // High-contrast Deep Forest Text
    val SageBorder = Color(0xFFC3E29C)
    val SageSignalGreen = Color(0xFF4CAF50)

    // Mode 2: Flight E-Ticket Pass Tokens (Warmed Periwinkle-Indigo & Lavender Mist)
    val AviationNavyStart = Color(0xFF2B2768)      // Warm Periwinkle-Indigo Dusk
    val AviationNavyEnd = Color(0xFF1B1849)        // Deep Periwinkle Midnight
    val SkyBluePillBg = Color(0xFFEEF2FF)          // Stitch Canonical Periwinkle Mist Surface (#EEF2FF)
    val SkyBluePillText = Color(0xFF2B2768)        // Deep Periwinkle Indigo Typography
    val SkyBlueBorder = Color(0xFFC7D2FE)          // Soft Periwinkle Hairline Border
    val SkySignalBlue = Color(0xFF818CF8)          // Luminous Periwinkle Signal

    // Shared Specular & Rim Tokens
    val SpecularRim = Color(0x38FFFFFF)            // 22% Specular Hairline
    val TranslucentHeader = Color(0x2EFFFFFF)      // 18% Header Glass Inset
    val DashedLine = Color(0x38FFFFFF)             // Perforation Dashed Line

    // Expressive Radii
    val RadiusCardCorner = 26.dp
    val RadiusPill = RoundedCornerShape(999.dp)
}

enum class ActiveTravelPassMode {
    TRAIN,
    FLIGHT
}

// ==============================================================================
// 2. LUXURY BOARDING PASS SHAPE (Left & Right 14dp Semicircular Inward Cutouts)
// ==============================================================================
class AnimatedTicketNotchShape(
    private val cornerRadius: Float,
    private val notchRadius: Float,
    private val notchYPercent: Float = 0.52f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val notchY = h * notchYPercent

            // Top-left corner
            moveTo(0f, cornerRadius)
            arcTo(
                rect = Rect(0f, 0f, 2 * cornerRadius, 2 * cornerRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top edge to top-right
            lineTo(w - cornerRadius, 0f)
            arcTo(
                rect = Rect(w - 2 * cornerRadius, 0f, w, 2 * cornerRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right edge down to notch
            lineTo(w, notchY - notchRadius)
            // Right semicircle cutout (bites inward)
            arcTo(
                rect = Rect(w - notchRadius, notchY - notchRadius, w + notchRadius, notchY + notchRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Right edge to bottom-right
            lineTo(w, h - cornerRadius)
            arcTo(
                rect = Rect(w - 2 * cornerRadius, h - 2 * cornerRadius, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom edge to bottom-left
            lineTo(cornerRadius, h)
            arcTo(
                rect = Rect(0f, h - 2 * cornerRadius, 2 * cornerRadius, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left edge up to notch
            lineTo(0f, notchY + notchRadius)
            // Left semicircle cutout (bites inward)
            arcTo(
                rect = Rect(-notchRadius, notchY - notchRadius, notchRadius, notchY + notchRadius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Close to top-left
            lineTo(0f, cornerRadius)
            close()
        }
        return Outline.Generic(path)
    }
}

// ==============================================================================
// 3. MASTER COMPONENT: AnimatedTransitDeckHeroCard
//    (Self-contained component with stacked spring-physics shuffle + kinetic train & airplane graphics)
// ==============================================================================
@Composable
fun AnimatedTransitDeckHeroCard(
    modifier: Modifier = Modifier,
    initialPassMode: ActiveTravelPassMode = ActiveTravelPassMode.TRAIN,
    trainCountLogged: Int = 0,
    flightCountActive: Int = 0,
    onEnterTrainPnrClick: () -> Unit = {},
    onUploadFlightPdfClick: () -> Unit = {}
) {
    var activePass by remember { mutableStateOf(initialPassMode) }
    val haptic = LocalHapticFeedback.current

    // Luxury cardstock spring physics (LowBouncy 0.74f avoids wobble while preserving tactile snap)
    val springSpecFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val springSpecDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Continuous deck progress (0f = Train Foreground, 1f = Flight Foreground)
    // Z-index flips strictly at the midpoint (deckProgress >= 0.5f) when cards are at peak separation!
    val isTrainActive = activePass == ActiveTravelPassMode.TRAIN
    val isFlightActive = activePass == ActiveTravelPassMode.FLIGHT
    val deckProgress by animateFloatAsState(
        targetValue = if (isFlightActive) 1f else 0f,
        animationSpec = springSpecFloat,
        label = "DeckShuffleProgress"
    )
    val isFlightInFrontZ = deckProgress >= 0.5f
    // Parabolic separation arc (0f at rest, 1f at midpoint crossing) so cards peel over each other without clipping
    val crossingArc = 4f * deckProgress * (1f - deckProgress)

    val trainScale by animateFloatAsState(
        targetValue = if (isTrainActive) 1.0f else 0.94f,
        animationSpec = springSpecFloat,
        label = "TrainScale"
    )
    val baseTrainOffsetY by animateDpAsState(
        targetValue = if (isTrainActive) 0.dp else (-34).dp,
        animationSpec = springSpecDp,
        label = "TrainOffsetY"
    )
    val trainOffsetY = baseTrainOffsetY + (if (!isFlightInFrontZ) (-8).dp else 6.dp) * crossingArc
    val trainAlpha by animateFloatAsState(
        targetValue = if (isTrainActive) 1.0f else 0.84f,
        animationSpec = tween(durationMillis = 260),
        label = "TrainAlpha"
    )
    val trainElevation by animateDpAsState(
        targetValue = if (isTrainActive) 14.dp else 4.dp,
        animationSpec = springSpecDp,
        label = "TrainElevation"
    )

    val flightScale by animateFloatAsState(
        targetValue = if (isFlightActive) 1.0f else 0.94f,
        animationSpec = springSpecFloat,
        label = "FlightScale"
    )
    val baseFlightOffsetY by animateDpAsState(
        targetValue = if (isFlightActive) 0.dp else (-34).dp,
        animationSpec = springSpecDp,
        label = "FlightOffsetY"
    )
    val flightOffsetY = baseFlightOffsetY + (if (isFlightInFrontZ) (-8).dp else 6.dp) * crossingArc
    val flightAlpha by animateFloatAsState(
        targetValue = if (isFlightActive) 1.0f else 0.84f,
        animationSpec = tween(durationMillis = 260),
        label = "FlightAlpha"
    )
    val flightElevation by animateDpAsState(
        targetValue = if (isFlightActive) 14.dp else 4.dp,
        animationSpec = springSpecDp,
        label = "FlightElevation"
    )

    ProvideTextStyle(
        value = LocalTextStyle.current.copy(fontFamily = SplitMateTheme.FontRounded)
    ) {
        // Master container: 242dp envelope with 34dp upper headroom to prevent overlap with top buttons
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(242.dp)
                .padding(top = 34.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // ----------------------------------------------------------------------
            // CARD 1: FLIGHT PASS (Warmed Periwinkle-Indigo)
            // ----------------------------------------------------------------------
            FlightPassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .offset(y = flightOffsetY)
                    .scale(flightScale)
                    .zIndex(if (isFlightInFrontZ) 2f else 1f),
                elevation = flightElevation,
                alpha = flightAlpha,
                isForeground = isFlightActive,
                flightCountActive = flightCountActive,
                onCardHeaderTap = {
                    if (!isFlightActive) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activePass = ActiveTravelPassMode.FLIGHT
                    }
                },
                onPrimaryCtaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUploadFlightPdfClick()
                },
                onSwitchToTrainClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activePass = ActiveTravelPassMode.TRAIN
                }
            )

            // ----------------------------------------------------------------------
            // CARD 2: TRAIN PASS (Fresh Botanical Sage Green)
            // ----------------------------------------------------------------------
            TrainPassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .offset(y = trainOffsetY)
                    .scale(trainScale)
                    .zIndex(if (!isFlightInFrontZ) 2f else 1f),
                elevation = trainElevation,
                alpha = trainAlpha,
                isForeground = isTrainActive,
                trainCountLogged = trainCountLogged,
                onCardHeaderTap = {
                    if (!isTrainActive) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activePass = ActiveTravelPassMode.TRAIN
                    }
                },
                onPrimaryCtaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnterTrainPnrClick()
                },
                onSwitchToFlightClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activePass = ActiveTravelPassMode.FLIGHT
                }
            )
        }
    }
}

// ==============================================================================
// 4. SUB-CARD 1: TrainPassCard with Kinetic Express Rail Animation
// ==============================================================================
@Composable
private fun TrainPassCard(
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp,
    alpha: Float,
    isForeground: Boolean,
    trainCountLogged: Int,
    onCardHeaderTap: () -> Unit,
    onPrimaryCtaClick: () -> Unit,
    onSwitchToFlightClick: () -> Unit
) {
    val ticketShape = AnimatedTicketNotchShape(
        cornerRadius = 26f * 3f,
        notchRadius = 14f * 3f,
        notchYPercent = 0.52f
    )

    // Ambient loop for train gliding across the tracks (5.5s periodic cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "TrainTransitLoop")
    val trainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TrainGlideProgress"
    )

    val trainVectorPainter = rememberVectorPainter(Icons.Rounded.Train)

    // Signal light beacon pulse
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TrainBeaconPulse"
    )

    Surface(
        onClick = { if (!isForeground) onCardHeaderTap() },
        shape = ticketShape,
        color = Color.Transparent,
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = ticketShape,
                spotColor = Color(0x44223E13),
                ambientColor = Color(0x2614270B)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ticketShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AnimatedTransitDeckTokens.SageForestStart.copy(alpha = alpha),
                            AnimatedTransitDeckTokens.SageForestEnd.copy(alpha = alpha)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = ticketShape
                )
                .border(
                    width = 1.dp,
                    color = AnimatedTransitDeckTokens.SpecularRim,
                    shape = ticketShape
                )
                // Kinetic Vector Rail Canvas (Twin Curving Tracks + Coupled Coaches + Locomotive Icon + Headlight Cone)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val trackColor = Color.White.copy(alpha = 0.20f * alpha)
                    val sleeperColor = Color.White.copy(alpha = 0.12f * alpha)

                    val p0 = Offset(w * 0.55f, h)
                    val p1 = Offset(w * 0.68f, h * 0.70f)
                    val p2 = Offset(w * 0.75f, h * 0.36f)
                    val p3 = Offset(w * 1.05f, h * 0.06f)

                    // Track 1
                    val rail1 = Path().apply {
                        moveTo(w * 0.52f, h)
                        cubicTo(w * 0.65f, h * 0.70f, w * 0.72f, h * 0.36f, w * 1.02f, h * 0.06f)
                    }
                    // Track 2
                    val rail2 = Path().apply {
                        moveTo(w * 0.58f, h)
                        cubicTo(w * 0.71f, h * 0.70f, w * 0.78f, h * 0.36f, w * 1.08f, h * 0.06f)
                    }

                    drawPath(rail1, color = trackColor, style = Stroke(width = 2.dp.toPx()))
                    drawPath(rail2, color = trackColor, style = Stroke(width = 2.dp.toPx()))

                    // Cross sleepers (wooden/steel railway ties)
                    val sleeperDash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 10.dp.toPx()), 0f)
                    drawPath(rail1, color = sleeperColor, style = Stroke(width = 15.dp.toPx(), pathEffect = sleeperDash))

                    // Station node beacon
                    val beaconCenter = Offset(w * 0.88f, h * 0.36f)
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SageSignalGreen.copy(alpha = 0.25f * beaconPulse * alpha),
                        radius = 18.dp.toPx(),
                        center = beaconCenter
                    )
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SagePillBg.copy(alpha = 0.85f * beaconPulse * alpha),
                        radius = 4.dp.toPx(),
                        center = beaconCenter
                    )

                    // Coupled Trailing Coach Cars following the Locomotive on the Rail
                    val t = trainProgress
                    listOf(0.15f, 0.08f).forEach { lag ->
                        val coachT = (t - lag)
                        if (coachT in 0.02f..0.98f) {
                            val coachPos = cubicBezierPoint(p0, p1, p2, p3, coachT)
                            val coachAngle = cubicBezierTangentAngle(p0, p1, p2, p3, coachT)
                            withTransform({
                                translate(coachPos.x, coachPos.y)
                                rotate(coachAngle, Offset.Zero)
                            }) {
                                drawRoundRect(
                                    color = AnimatedTransitDeckTokens.SagePillBg.copy(alpha = 0.55f * alpha),
                                    topLeft = Offset(-7.dp.toPx(), -4.dp.toPx()),
                                    size = Size(14.dp.toPx(), 8.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }
                    }

                    // Locomotive Position & Rhythmic Rail-Clack Micro-Bounce
                    val rawTrainPos = cubicBezierPoint(p0, p1, p2, p3, t)
                    val clackBounceY = sin(t * 38f) * 1.1f.dp.toPx()
                    val trainPos = Offset(rawTrainPos.x, rawTrainPos.y + clackBounceY)
                    val tangentAngle = cubicBezierTangentAngle(p0, p1, p2, p3, t)
                    val rad = Math.toRadians(tangentAngle.toDouble())

                    // Golden-Sage Locomotive Headlight Cone projecting along the track
                    val beamLength = 26.dp.toPx()
                    val beamSpread = 9.dp.toPx()
                    val noseX = trainPos.x + cos(rad).toFloat() * 10.dp.toPx()
                    val noseY = trainPos.y + sin(rad).toFloat() * 10.dp.toPx()
                    val farX = noseX + cos(rad).toFloat() * beamLength
                    val farY = noseY + sin(rad).toFloat() * beamLength
                    val perpX = -sin(rad).toFloat() * beamSpread
                    val perpY = cos(rad).toFloat() * beamSpread

                    val beamPath = Path().apply {
                        moveTo(noseX, noseY)
                        lineTo(farX + perpX, farY + perpY)
                        lineTo(farX - perpX, farY - perpY)
                        close()
                    }
                    drawPath(
                        path = beamPath,
                        color = Color(0xFFFFF59D).copy(alpha = 0.26f * alpha)
                    )

                    // Locomotive Medallion + Actual Train Vector Icon (Icons.Rounded.Train)
                    val medallionRadius = 11.dp.toPx()
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SageForestEnd.copy(alpha = 0.92f * alpha),
                        radius = medallionRadius + 2.dp.toPx(),
                        center = trainPos
                    )
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SagePillBg.copy(alpha = 0.98f * alpha),
                        radius = medallionRadius,
                        center = trainPos
                    )
                    val iconPx = 14.dp.toPx()
                    withTransform({
                        translate(trainPos.x - iconPx / 2f, trainPos.y - iconPx / 2f)
                    }) {
                        with(trainVectorPainter) {
                            draw(
                                size = Size(iconPx, iconPx),
                                alpha = alpha,
                                colorFilter = ColorFilter.tint(AnimatedTransitDeckTokens.SagePillText)
                            )
                        }
                    }
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP STATUS STRIP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!isForeground) onCardHeaderTap() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = Color.Black.copy(alpha = 0.28f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsRailway,
                                contentDescription = null,
                                tint = AnimatedTransitDeckTokens.SagePillBg,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "IRCTC RAIL PASS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (!isForeground) {
                        Surface(
                            shape = AnimatedTransitDeckTokens.RadiusPill,
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "Tap to Switch to Train ⤾",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(AnimatedTransitDeckTokens.SageSignalGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (trainCountLogged > 0) "$trainCountLogged PNRs Synced" else "Offline PNR Vault Ready",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }
                }

                // MIDDLE HEADINGS
                Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Text(
                        text = "Split Train Ticket",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Auto-fetches IRCTC fare & berths via 10-Digit PNR.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // BOTTOM ACTION ROW: Primary Enter PNR + Secondary Switch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPrimaryCtaClick,
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = AnimatedTransitDeckTokens.SagePillBg,
                        border = BorderStroke(0.5.dp, AnimatedTransitDeckTokens.SageBorder),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(38.dp)
                            .sizeIn(minWidth = 124.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Enter PNR",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AnimatedTransitDeckTokens.SagePillText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = AnimatedTransitDeckTokens.SagePillText,
                                modifier = Modifier.size(17.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = AnimatedTransitDeckTokens.SagePillBg,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onSwitchToFlightClick,
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlightTakeoff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Flight Pass ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 5. SUB-CARD 2: FlightPassCard with Aerodynamic Flight & Contrail Animation
// ==============================================================================
@Composable
private fun FlightPassCard(
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp,
    alpha: Float,
    isForeground: Boolean,
    flightCountActive: Int,
    onCardHeaderTap: () -> Unit,
    onPrimaryCtaClick: () -> Unit,
    onSwitchToTrainClick: () -> Unit
) {
    val ticketShape = AnimatedTicketNotchShape(
        cornerRadius = 26f * 3f,
        notchRadius = 14f * 3f,
        notchYPercent = 0.52f
    )

    // Ambient loop for airplane traversing contrail arc (6s periodic cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "FlightTransitLoop")
    val flightProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AirplaneGlideProgress"
    )

    val flightVectorPainter = rememberVectorPainter(Icons.Rounded.Flight)

    // Radar pulse wave expansion
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulseRadius"
    )

    Surface(
        onClick = { if (!isForeground) onCardHeaderTap() },
        shape = ticketShape,
        color = Color.Transparent,
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = ticketShape,
                spotColor = Color(0x44132743),
                ambientColor = Color(0x260C1B2E)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ticketShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AnimatedTransitDeckTokens.AviationNavyStart.copy(alpha = alpha),
                            AnimatedTransitDeckTokens.AviationNavyEnd.copy(alpha = alpha)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = ticketShape
                )
                .border(
                    width = 1.dp,
                    color = AnimatedTransitDeckTokens.SpecularRim,
                    shape = ticketShape
                )
                // Kinetic Vector Aeronautical Canvas (Contrail Path + Banking Jet Aircraft Icon + Radar Rings)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val contrailBaseColor = Color.White.copy(alpha = 0.16f * alpha)

                    val p0 = Offset(w * 0.44f, h * 0.96f)
                    val p1 = Offset(w * 0.60f, h * 0.65f)
                    val p2 = Offset(w * 0.76f, h * 0.58f)
                    val p3 = Offset(w * 0.98f, h * 0.14f)

                    // Parabolic contrail path
                    val contrailPath = Path().apply {
                        moveTo(p0.x, p0.y)
                        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                    }

                    // Main dashed contrail line
                    drawPath(
                        path = contrailPath,
                        color = contrailBaseColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )

                    // Radar node and expanding ring wave
                    val beaconCenter = Offset(w * 0.95f, h * 0.18f)
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SkySignalBlue.copy(alpha = (1f - radarPulse) * 0.28f * alpha),
                        radius = (36 * radarPulse).dp.toPx(),
                        center = beaconCenter,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SkyBluePillBg.copy(alpha = 0.75f * alpha),
                        radius = 4.dp.toPx(),
                        center = beaconCenter
                    )

                    // Gliding Jet Aircraft Position & Tangent Angle
                    val t = flightProgress
                    val planePos = cubicBezierPoint(p0 = p0, p1 = p1, p2 = p2, p3 = p3, t = t)
                    val tangentDeg = cubicBezierTangentAngle(p0 = p0, p1 = p1, p2 = p2, p3 = p3, t = t)
                    // Icons.Rounded.Flight nose points UP (-90 deg), so +90 aligns the nose along the tangent
                    val planeRotation = tangentDeg + 90f

                    // Twin Wingtip Jet Contrail Wakes trailing behind the plane
                    val wakePos1 = cubicBezierPoint(p0, p1, p2, p3, (t - 0.09f).coerceIn(0f, 1f))
                    val wakePos2 = cubicBezierPoint(p0, p1, p2, p3, (t - 0.17f).coerceIn(0f, 1f))
                    if (t > 0.05f) {
                        drawLine(
                            color = AnimatedTransitDeckTokens.SkyBluePillBg.copy(alpha = 0.55f * alpha),
                            start = planePos,
                            end = wakePos1,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f * alpha),
                            start = wakePos1,
                            end = wakePos2,
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Luminous Periwinkle Radar Halo + Rotated Jet Vector Icon (Icons.Rounded.Flight)
                    drawCircle(
                        color = AnimatedTransitDeckTokens.SkySignalBlue.copy(alpha = 0.22f * alpha),
                        radius = 13.dp.toPx(),
                        center = planePos
                    )
                    val jetSizePx = 21.dp.toPx()
                    withTransform({
                        translate(planePos.x, planePos.y)
                        rotate(planeRotation, Offset.Zero)
                        translate(-jetSizePx / 2f, -jetSizePx / 2f)
                    }) {
                        with(flightVectorPainter) {
                            draw(
                                size = Size(jetSizePx, jetSizePx),
                                alpha = alpha,
                                colorFilter = ColorFilter.tint(AnimatedTransitDeckTokens.SkyBluePillBg)
                            )
                        }
                    }
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP STATUS STRIP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!isForeground) onCardHeaderTap() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = Color.Black.copy(alpha = 0.28f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlightTakeoff,
                                contentDescription = null,
                                tint = AnimatedTransitDeckTokens.SkyBluePillBg,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AIRLINE E-TICKET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (!isForeground) {
                        Surface(
                            shape = AnimatedTransitDeckTokens.RadiusPill,
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "Tap to Switch to Flight ⤾",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(AnimatedTransitDeckTokens.SkySignalBlue)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (flightCountActive > 0) "$flightCountActive Flights Logged" else "PDF AI Parser Ready",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }
                }

                // MIDDLE HEADINGS
                Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Text(
                        text = "Split Flight E-Ticket",
                        fontFamily = SplitMateTheme.FontDisplay,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Upload airline PDF to parse seats & airfare.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // BOTTOM ACTION ROW: Primary Upload PDF + Secondary Switch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPrimaryCtaClick,
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = AnimatedTransitDeckTokens.SkyBluePillBg,
                        border = BorderStroke(0.5.dp, AnimatedTransitDeckTokens.SkyBlueBorder),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .height(38.dp)
                            .sizeIn(minWidth = 124.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Upload PDF",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AnimatedTransitDeckTokens.SkyBluePillText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = AnimatedTransitDeckTokens.SkyBluePillText,
                                modifier = Modifier.size(17.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = AnimatedTransitDeckTokens.SkyBluePillBg,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onSwitchToTrainClick,
                        shape = AnimatedTransitDeckTokens.RadiusPill,
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsRailway,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Train Pass ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 6. MATHEMATICAL HELPER: Cubic Bezier Interpolation for Smooth Kinetic Glides
// ==============================================================================
private fun cubicBezierPoint(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float
): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t

    val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
    return Offset(x, y)
}

private fun cubicBezierTangentAngle(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    t: Float
): Float {
    val u = 1f - t
    val dx = 3f * u * u * (p1.x - p0.x) + 6f * u * t * (p2.x - p1.x) + 3f * t * t * (p3.x - p2.x)
    val dy = 3f * u * u * (p1.y - p0.y) + 6f * u * t * (p2.y - p1.y) + 3f * t * t * (p3.y - p2.y)
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
}
