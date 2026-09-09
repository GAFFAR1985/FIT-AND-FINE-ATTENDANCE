package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.EmeraldGreenPrimary
import kotlin.math.abs

@Composable
fun MemberAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    testTag: String = "member_avatar"
) {
    val initials = remember(name) {
        name.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    }

    val backgroundColor = remember(name) {
        val colors = listOf(
            Color(0xFF0284C7), // Sky 600
            Color(0xFF059669), // Emerald 600
            Color(0xFF7C3AED), // Violet 600
            Color(0xFFD97706), // Amber 600
            Color(0xFFE11D48), // Rose 600
            Color(0xFF4F46E5)  // Indigo 600
        )
        if (name.isBlank()) Color(0xFF64748B)
        else colors[abs(name.hashCode()) % colors.size]
    }

    val boxModifier = modifier
        .size(size)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .clip(shape)
        .testTag(testTag)

    if (!photoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Photo of $name",
            contentScale = ContentScale.Crop,
            modifier = boxModifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 3),
                        strokeWidth = 2.dp,
                        color = EmeraldGreenPrimary
                    )
                }
            },
            error = {
                InitialsPlaceholder(
                    name = name,
                    initials = initials,
                    backgroundColor = backgroundColor,
                    size = size,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        InitialsPlaceholder(
            name = name,
            initials = initials,
            backgroundColor = backgroundColor,
            size = size,
            modifier = boxModifier
        )
    }
}

@Composable
private fun InitialsPlaceholder(
    name: String,
    initials: String,
    backgroundColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotBlank()) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
