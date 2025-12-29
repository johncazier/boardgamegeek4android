package com.boardgamegeek.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.UNRATED
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class) // Required for PullToRefreshBox
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    paddingValues: PaddingValues,
    isCreatingShortcut: Boolean,
    changingGamePlayId: Long,
    onGameClick: (gameId: Int, gameName: String, thumbnailUrl: String?, heroImageUrl: String?) -> Unit,
) {
    val collectionItems by viewModel.itemsFlow.collectAsState()
    val isRefreshing by viewModel.isRefreshingFlow.collectAsState()
    val isFiltering by viewModel.isFilteringFlow.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState() // Create the state for Material 3 version

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing || isFiltering,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (collectionItems.isEmpty() && !isFiltering && !isRefreshing) {
                EmptyCollectionView(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(
                        items = collectionItems,
                        key = { item -> item.internalId }
                    ) { item ->
                        CollectionItemRow(
                            item = item,
                            onItemClick = {
                                when {
                                    isCreatingShortcut -> {
                                        // TODO: Handle shortcut creation for item
                                    }
                                    changingGamePlayId != com.boardgamegeek.provider.BggContract.INVALID_ID.toLong() -> {
                                        // TODO: Handle changing game for a play
                                    }
                                    else -> {
                                        onGameClick(item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                                    }
                                }
                            },
                            onItemLongClick = {
                                // TODO: Implement ActionMode initiation
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (isFiltering && collectionItems.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun CollectionItemRow(
    item: CollectionItem,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onItemClick, onLongClick = onItemLongClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.collectionName,
            modifier = Modifier
                .size(60.dp)
                .padding(end = 8.dp),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
            error = painterResource(id = R.drawable.ic_launcher_foreground)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(item.collectionName, style = MaterialTheme.typography.titleMedium)
            item.yearPublished.takeIf { it > 0 }?.let {
                Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val rating = item.rating

        if (rating != UNRATED) {
            val ratingColorInt = rating.toColor(BggColors.ratingColors)
            Box(modifier = Modifier
                .padding(start = 8.dp)
                .background(color = Color(ratingColorInt))
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", rating),
                    color = Color(ratingColorInt.getTextColor()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCollectionView(
    modifier: Modifier = Modifier,
    emptyText: String = stringResource(R.string.empty_collection)
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = { /* TODO: Maybe trigger a refresh or sync? */ }) {
            Text(stringResource(R.string.menu_refresh))
        }
    }
}