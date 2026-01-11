package com.boardgamegeek.ui.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.UNRATED
import com.boardgamegeek.sorter.CollectionSorter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // Required for PullToRefreshBox and stickyHeader
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
    val effectiveSort by viewModel.effectiveSort.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState() // Create the state for Material 3 version
    val context = LocalContext.current

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
                val sorter = effectiveSort?.first
                val groupedItems = remember(collectionItems, sorter) {
                     collectionItems.groupBy { item ->
                         sorter?.getHeaderText(item) ?: "-"
                     }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    groupedItems.forEach { (header, itemsInGroup) ->
                        stickyHeader(key = header) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                val isRatingSort = sorter?.getRatingText(itemsInGroup.firstOrNull() ?: CollectionItem())?.isNotEmpty() == true
                                Text(
                                    text = header,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = if (isRatingSort) TextAlign.Center else TextAlign.Start
                                )
                            }
                        }

                        items(
                            items = itemsInGroup,
                            key = { item -> item.internalId }
                        ) { item ->
                            CollectionItemRow(
                                item = item,
                                sorter = sorter,
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
        }

        if (isFiltering && collectionItems.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun CollectionItemRow(
    item: CollectionItem,
    sorter: CollectionSorter?,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onItemClick, onLongClick = onItemLongClick)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.collectionName,
            modifier = Modifier
                .size(56.dp)
                .padding(end = 8.dp),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
            error = painterResource(id = R.drawable.ic_launcher_foreground)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.collectionName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.menu_favorite),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp),
                        tint = MaterialTheme.colorScheme.primary // Or specific favorite color
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year
                item.yearPublished.asYear(context).takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Info / Rating / Timestamp
                val timestamp = sorter?.getTimestamp(item) ?: 0L
                val ratingText = sorter?.getRatingText(item).orEmpty()
                
                if (timestamp > 0L) {
                    Text(
                        text = timestamp.formatTimestamp(context).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                } else if (ratingText.isNotEmpty()) {
                    val rating = sorter?.getRating(item) ?: 0.0
                    val ratingColorInt = rating.toColor(BggColors.ratingColors)
                    
                    Box(modifier = Modifier
                        .width(48.dp)
                        .background(color = Color(ratingColorInt))
                    ) {
                        Text(
                            text = ratingText,
                            color = Color(ratingColorInt.getTextColor()),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val displayInfo = sorter?.getDisplayInfo(item)
                    if (!displayInfo.isNullOrEmpty()) {
                        Text(
                            text = displayInfo,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
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