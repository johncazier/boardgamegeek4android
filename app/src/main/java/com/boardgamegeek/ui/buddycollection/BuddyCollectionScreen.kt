package com.boardgamegeek.ui.buddycollection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status

@Composable
fun BuddyCollectionScreen(
    resource: RefreshableResource<List<CollectionItem>>?,
    paddingValues: PaddingValues,
    onGameClick: (CollectionItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            resource == null || (resource.status == Status.REFRESHING && resource.data.isNullOrEmpty()) -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            resource.status == Status.ERROR && resource.data.isNullOrEmpty() -> {
                EmptyCollectionMessage(resource.message)
            }
            resource.data.isNullOrEmpty() -> {
                EmptyCollectionMessage(stringResource(R.string.empty_buddy_collection))
            }
            else -> {
                val grouped = resource.data.orEmpty().groupBy { it.sortName.firstChar().ifBlank { "-" } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    grouped.toSortedMap().forEach { (header, games) ->
                        item {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(items = games, key = { item -> item.collectionId }) { item ->
                            BuddyCollectionRow(item = item, onClick = { onGameClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuddyCollectionRow(
    item: CollectionItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = item.gameName,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.gameId.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyCollectionMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp)
        )
    }
}
