package com.example.ui.components.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SubtitleSettings

@Composable
fun SubtitleOverlay(
    text: String,
    settings: SubtitleSettings,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible || text.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = settings.bottomMarginDp.dp)
            .testTag("subtitle_overlay_container"),
        contentAlignment = Alignment.BottomCenter
    ) {
        val textColor = parseHexColor(settings.textColorHex).copy(alpha = settings.textOpacity)
        val bgColor = parseHexColor(settings.backgroundColorHex).copy(alpha = settings.backgroundOpacity)
        val outlineColor = parseHexColor(settings.outlineColorHex)

        val textStyle = TextStyle(
            fontSize = settings.fontSizeSp.sp,
            fontWeight = if (settings.fontStyle.contains("BOLD")) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (settings.fontStyle.contains("ITALIC")) FontStyle.Italic else FontStyle.Normal,
            shadow = if (settings.outlineEnabled) Shadow(
                color = outlineColor,
                offset = Offset(2f, 2f),
                blurRadius = 3f
            ) else null
        )

        Surface(
            color = bgColor,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag("subtitle_text_surface")
        ) {
            Text(
                text = text,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("subtitle_text_content")
            )
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        Color(colorInt)
    } catch (_: Exception) {
        Color.White
    }
}
