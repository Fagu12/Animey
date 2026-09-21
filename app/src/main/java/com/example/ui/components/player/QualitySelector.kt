package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.QualityOption

@Composable
fun QualitySelector(
    availableQualities: List<QualityOption>,
    selectedQuality: String?,
    onSelectQuality: (String) -> Unit,
    onSetPreferredQuality: (quality: String, isGlobal: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("quality_selector")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Video Stream Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notice badge: Provider/Server and Quality Separation
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Provider/Server selection and Quality selection are kept strictly separated.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quality Options List (Auto, 1080p, 720p, 480p, 360p)
            val allStandardQualities = listOf("Auto", "1080p", "720p", "480p", "360p")
            val optionMap = availableQualities.associateBy { it.quality.lowercase() }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allStandardQualities.forEach { qKey ->
                    val opt = optionMap[qKey.lowercase()]
                    val label = when (qKey) {
                        "Auto" -> "Auto (Automatic Best Quality)"
                        "1080p" -> "1080p (Full HD)"
                        "720p" -> "720p (HD)"
                        "480p" -> "480p (Standard Definition)"
                        "360p" -> "360p (Data Saver)"
                        else -> qKey
                    }
                    val isSelected = selectedQuality.equals(qKey, ignoreCase = true)
                    val isAvailable = opt?.isAvailable ?: true
                    val isPreferred = opt?.isPreferred ?: false

                    QualityOptionRow(
                        qualityKey = qKey,
                        label = label,
                        isSelected = isSelected,
                        isAvailable = isAvailable,
                        isPreferred = isPreferred,
                        onSelect = { onSelectQuality(qKey) },
                        onTogglePreferred = { onSetPreferredQuality(qKey, false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityOptionRow(
    qualityKey: String,
    label: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    isPreferred: Boolean,
    onSelect: () -> Unit,
    onTogglePreferred: () -> Unit
) {
    Card(
        onClick = onSelect,
        enabled = isAvailable,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                isSelected -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quality_row_$qualityKey")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onTertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAvailable) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )

                    if (!isAvailable) {
                        Text(
                            text = "Not directly hosted on current server (switches server if picked)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else if (isPreferred) {
                        Text(
                            text = "Preferred Quality",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePreferred,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("pref_star_$qualityKey")
                ) {
                    Icon(
                        imageVector = if (isPreferred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Set Preferred Quality",
                        tint = if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
