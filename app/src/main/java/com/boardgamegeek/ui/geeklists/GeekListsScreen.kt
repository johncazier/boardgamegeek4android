package com.boardgamegeek.ui.geeklists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.boardgamegeek.R
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.ui.geeklist.GeekListActivity

@Composable
fun GeekListsScreen(
    viewModel: GeekListsViewModel,
    paddingValues: PaddingValues,
) {
    val lazyPagingItems = viewModel.geekLists.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id }
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null) {
                    GeekListRow(geekList = item)
                    HorizontalDivider()
                }
            }

            lazyPagingItems.loadState.apply {
                when {
                    refresh is LoadState.Loading -> {
                        item {
                            CircularProgressIndicator(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp))
                        }
                    }
                    append is LoadState.Loading -> {
                        item {
                            CircularProgressIndicator(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp))
                        }
                    }
                    refresh is LoadState.Error -> {
                        val e = lazyPagingItems.loadState.refresh as LoadState.Error
                        item {
                            Text(
                                text = e.error.localizedMessage ?: "",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    append is LoadState.Error -> {
                        val e = lazyPagingItems.loadState.append as LoadState.Error
                        item {
                            Text(
                                text = e.error.localizedMessage ?: "",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeekListRow(geekList: GeekList) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                GeekListActivity.start(context, geekList.id, geekList.title)
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = geekList.title)
            Text(text = geekList.username)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = pluralStringResource(id = R.plurals.num_items, geekList.numberOfItems, geekList.numberOfItems))
            Text(text = pluralStringResource(id = R.plurals.num_thumbs, geekList.numberOfThumbs, geekList.numberOfThumbs))
        }
    }
}
