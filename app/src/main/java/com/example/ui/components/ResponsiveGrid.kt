package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.Anime

@Composable
fun ResponsiveAnimeGrid(
    items: List<Anime>,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    minItemWidth: Dp = 120.dp,
    testTag: String = "responsive_anime_grid"
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val calculatedMin = if (maxWidth >= 840.dp) {
            150.dp
        } else if (maxWidth >= 600.dp) {
            135.dp
        } else {
            minItemWidth
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = calculatedMin),
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag)
        ) {
            items(
                items = items,
                key = { it.localId },
                contentType = { "anime_card" }
            ) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.localId) }
                )
            }
        }
    }
}
