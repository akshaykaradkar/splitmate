package com.splitmate.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.splitmate.app.R

/**
 * Dynamic DiceBear Open-Peeps SVG Avatar (`https://api.dicebear.com/9.x/open-peeps/svg?seed={seed}`)
 * decoded natively via Coil + SvgDecoder.Factory() and cached indefinitely to disk, with
 * local Vector Drawable (`R.drawable.ic_avatar_placeholder`) fallback.
 */
@Composable
fun DiceBearAvatar(
    seed: String,
    contentDescription: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val svgUrl = buildDiceBearOpenPeepsUrl(seed)

    val request = ImageRequest.Builder(context)
        .data(svgUrl)
        .decoderFactory(SvgDecoder.Factory())
        .diskCacheKey("dicebear_peep_$svgUrl")
        .memoryCacheKey("dicebear_peep_$svgUrl")
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
        error = painterResource(id = R.drawable.ic_avatar_placeholder),
        fallback = painterResource(id = R.drawable.ic_avatar_placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BuckwheatSageContainer, CircleShape)
            .border(1.dp, BuckwheatBorder, CircleShape)
    )
}

