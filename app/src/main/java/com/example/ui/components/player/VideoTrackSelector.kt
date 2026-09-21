package com.example.ui.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.player.VideoTrack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoTrackSelector(
    availableTracks: List<VideoTrack>,
    selectedTrack: VideoTrack?,
    onSelectTrack: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_track_selector")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.HighQuality,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Media3 Video Stream Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val isAutoSelected = selectedTrack == null
                FilterChip(
                    selected = isAutoSelected,
                    onClick = { onSelectTrack(null) },
                    label = { Text("Auto (Adaptive Bitrate)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("video_track_auto")
                )

                availableTracks.forEach { track ->
                    val isSelected = selectedTrack?.id == track.id
                    val label = when {
                        track.width > 0 && track.height > 0 -> "${track.height}p (${track.width}x${track.height})"
                        track.label.isNotBlank() -> track.label
                        else -> "Stream Track ${track.id}"
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTrack(track.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label)
                                if (track.bitrate > 0) {
                                    val mbps = String.format("%.1f Mbps", track.bitrate / 1_000_000f)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("• $mbps", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("video_track_${track.id}")
                    )
                }
            }
        }
    }
}
