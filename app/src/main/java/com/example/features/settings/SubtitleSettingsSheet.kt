package com.example.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    settings: SubtitleSettings,
    onSetLanguage: (String) -> Unit,
    onSetDelayMs: (Long) -> Unit,
    onSetFontSize: (Int) -> Unit,
    onSetFontStyle: (String) -> Unit,
    onSetTextColor: (String) -> Unit,
    onSetTextOpacity: (Float) -> Unit,
    onSetBackgroundColor: (String) -> Unit,
    onSetBackgroundOpacity: (Float) -> Unit,
    onSetOutlineEnabled: (Boolean) -> Unit,
    onSetOutlineColor: (String) -> Unit,
    onSetBottomMarginDp: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("subtitle_settings_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Subtitles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subtitle Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Visual Subtitle Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("subtitle_live_preview_card"),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = settings.bottomMarginDp.dp.coerceAtMost(32.dp)),
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
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Subtitle Preview • Sample Track Text (日本語)",
                            style = textStyle,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Delay Offset Controls
            Text(
                text = "Subtitle Delay Offset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onSetDelayMs(settings.delayMs - 250L) },
                    modifier = Modifier.testTag("sub_delay_minus_btn")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease delay")
                }

                val delaySec = settings.delayMs / 1000.0f
                Text(
                    text = String.format("%+.2fs (%d ms)", delaySec, settings.delayMs),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (settings.delayMs != 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { onSetDelayMs(settings.delayMs + 250L) },
                    modifier = Modifier.testTag("sub_delay_plus_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase delay")
                }

                TextButton(onClick = { onSetDelayMs(0L) }) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preferred Language Input
            Text(
                text = "Preferred Language Code",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            var langInput by remember(settings.preferredLanguage) { mutableStateOf(settings.preferredLanguage) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = langInput,
                    onValueChange = {
                        langInput = it
                        onSetLanguage(it)
                    },
                    label = { Text("Language Code (e.g. en, ja, es, fr, id)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sub_lang_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Font Size", fontWeight = FontWeight.SemiBold)
                Text(text = "${settings.fontSizeSp} sp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onSetFontSize(it.toInt()) },
                valueRange = 12f..36f,
                steps = 23,
                modifier = Modifier.testTag("sub_font_size_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Font Style Chips
            Text(text = "Font Style", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NORMAL", "BOLD", "ITALIC", "BOLD_ITALIC").forEach { style ->
                    FilterChip(
                        selected = settings.fontStyle == style,
                        onClick = { onSetFontStyle(style) },
                        label = { Text(style.replace("_", " ")) },
                        modifier = Modifier.testTag("sub_style_chip_$style")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Color & Opacity
            Text(text = "Text Color", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            ColorPresetsRow(
                selectedColorHex = settings.textColorHex,
                onSelectColor = onSetTextColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Text Opacity", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${(settings.textOpacity * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            Slider(
                value = settings.textOpacity,
                onValueChange = onSetTextOpacity,
                valueRange = 0.2f..1.0f,
                modifier = Modifier.testTag("sub_text_opacity_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Background Color & Opacity
            Text(text = "Background Color", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            ColorPresetsRow(
                selectedColorHex = settings.backgroundColorHex,
                onSelectColor = onSetBackgroundColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Background Opacity", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${(settings.backgroundOpacity * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            Slider(
                value = settings.backgroundOpacity,
                onValueChange = onSetBackgroundOpacity,
                valueRange = 0.0f..1.0f,
                modifier = Modifier.testTag("sub_bg_opacity_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Outline & Outline Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Text Outline / Shadow", fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = settings.outlineEnabled,
                    onCheckedChange = onSetOutlineEnabled,
                    modifier = Modifier.testTag("sub_outline_switch")
                )
            }
            if (settings.outlineEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                ColorPresetsRow(
                    selectedColorHex = settings.outlineColorHex,
                    onSelectColor = onSetOutlineColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bottom Margin", fontWeight = FontWeight.SemiBold)
                Text(text = "${settings.bottomMarginDp} dp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.bottomMarginDp.toFloat(),
                onValueChange = { onSetBottomMarginDp(it.toInt()) },
                valueRange = 8f..64f,
                steps = 27,
                modifier = Modifier.testTag("sub_bottom_margin_slider")
            )
        }
    }
}

@Composable
private fun ColorPresetsRow(
    selectedColorHex: String,
    onSelectColor: (String) -> Unit
) {
    val presets = listOf(
        "#FFFFFF" to "White",
        "#FFFF00" to "Yellow",
        "#00FFFF" to "Cyan",
        "#00FF00" to "Green",
        "#000000" to "Black",
        "#121212" to "Dark Gray",
        "#1F1B24" to "Purple Dark",
        "#FF3333" to "Red"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        items(presets) { (hex, name) ->
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = name,
                        tint = if (hex == "#FFFFFF" || hex == "#FFFF00" || hex == "#00FFFF") Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
