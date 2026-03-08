package com.boardgamegeek.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.model.Category

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    paddingValues: PaddingValues,
    onCategoryClick: (Category) -> Unit,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = viewModel::refresh,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullRefresh(pullRefreshState)
    ) {
        when (val items = categories) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_categories),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(
                            count = items.size,
                            key = { index -> items[index].id }
                        ) { index ->
                            val category = items[index]
                            CategoryRow(
                                category = category,
                                onClick = { onCategoryClick(category) }
                            )
                            HorizontalDivider()
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
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = pluralStringResource(R.plurals.games_suffix, category.itemCount, category.itemCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
