package com.boardgamegeek.ui.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFactory
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalFoundationApi::class) // Required for stickyHeader
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
    val effectiveFilters by viewModel.effectiveFilters.collectAsState()

    val isLoading = collectionItems == null || isRefreshing || isFiltering

    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val validFilters = remember(effectiveFilters) { effectiveFilters.filter { it.isValid } }
    val sortType = effectiveSort?.first?.getType(effectiveSort?.second ?: false)
        ?: CollectionSorterFactory.TYPE_DEFAULT
    val showSortChip = sortType != CollectionSorterFactory.TYPE_DEFAULT

    val listState = rememberLazyListState()
    var pendingScrollToTop by remember { mutableStateOf(false) }

    LaunchedEffect(sortType, effectiveSort?.second) {
        pendingScrollToTop = true
    }

    LaunchedEffect(collectionItems, pendingScrollToTop) {
        if (pendingScrollToTop && !collectionItems.isNullOrEmpty()) {
            listState.scrollToItem(0)
            pendingScrollToTop = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (collectionItems.isNullOrEmpty()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EmptyCollectionView(
                    modifier = Modifier.fillMaxSize(),
                    onRefresh = { viewModel.refresh() }
                )
            }
        } else {
            val loadedItems = collectionItems.orEmpty()
            val sorter = effectiveSort?.first
            val groupedItems = remember(loadedItems, sorter) {
                loadedItems.groupBy { item ->
                    sorter?.getHeaderText(item) ?: "-"
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = if (showSortChip || validFilters.isNotEmpty()) 64.dp else 16.dp)
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

        if (showSortChip || validFilters.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                CollectionFilterChipsRow(
                    filters = validFilters,
                    sortLabel = effectiveSort?.first?.description.orEmpty(),
                    isSortReversed = effectiveSort?.second == true,
                    showSortChip = showSortChip,
                    onSortClick = { viewModel.reverseSort() },
                    onFilterClick = { filter ->
                        activity?.let { host ->
                            CollectionFilterDialogFactory()
                                .create(host, filter.type)
                                ?.createDialog(host, filter)
                        }
                    },
                    onFilterRemove = { type -> viewModel.removeFilter(type) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CollectionFilterChipsRow(
    filters: List<CollectionFilterer>,
    sortLabel: String,
    isSortReversed: Boolean,
    showSortChip: Boolean,
    onSortClick: () -> Unit,
    onFilterClick: (CollectionFilterer) -> Unit,
    onFilterRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showSortChip) {
            item(key = "sort_chip") {
                CollectionChip(
                    text = sortLabel,
                    leadingIcon = if (isSortReversed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    onClick = onSortClick,
                    trailingIcon = null,
                    onTrailingIconClick = null
                )
            }
        }
        items(filters, key = { it.type }) { filter ->
            CollectionChip(
                text = filter.chipText(),
                leadingPainter = if (filter.iconResourceId != CollectionFilterer.INVALID_ICON) {
                    painterResource(id = filter.iconResourceId)
                } else null,
                onClick = { onFilterClick(filter) },
                trailingIcon = Icons.Filled.Close,
                onTrailingIconClick = { onFilterRemove(filter.type) }
            )
        }
    }
}

@Composable
private fun CollectionChip(
    text: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    onTrailingIconClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                leadingIcon != null -> {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                leadingPainter != null -> {
                    Icon(
                        painter = leadingPainter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null && onTrailingIconClick != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = stringResource(R.string.menu_clear),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onTrailingIconClick)
                )
            }
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
    emptyText: String = stringResource(R.string.empty_collection),
    onRefresh: () -> Unit = {}
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
        Button(onClick = onRefresh) {
            Text(stringResource(R.string.menu_refresh))
        }
    }
}
