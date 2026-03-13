package com.boardgamegeek.ui.linkedcollection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.BggColors

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LinkedCollectionScreen(
    collection: List<CollectionItem>?,
    emptyMessage: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (CollectionItem) -> Unit,
    paddingValues: PaddingValues,
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullRefresh(pullRefreshState)
    ) {
        when (val items = collection) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(items, key = { it.gameId }) { item ->
                            LinkedCollectionRow(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LinkedCollectionRow(
    item: CollectionItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnailSize = dimensionResource(R.dimen.thumbnail_list_size)
    val ratingColor = item.rating.toColor(BggColors.ratingColors)
    val ratingText = item.rating.asPersonalRating(context)
    val yearText = item.yearPublished.asYear(context)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.gameId != BggContract.INVALID_ID, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.robustThumbnailUrl.ensureHttpsScheme(),
            contentDescription = stringResource(R.string.thumbnail),
            placeholder = painterResource(R.drawable.thumbnail_image_empty),
            error = painterResource(R.drawable.thumbnail_image_empty),
            modifier = Modifier.size(thumbnailSize),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.robustName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.isFavorite) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_favorite_24),
                        contentDescription = stringResource(R.string.menu_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (yearText.isNotBlank()) {
                    Text(
                        text = yearText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
                if (ratingText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(24.dp)
                            .padding(start = 8.dp)
                            .background(color = Color(ratingColor))
                    ) {
                        Text(
                            text = ratingText,
                            color = Color(ratingColor.getTextColor()),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
