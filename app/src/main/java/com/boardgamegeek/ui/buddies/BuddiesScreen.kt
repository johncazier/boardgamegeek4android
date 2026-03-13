package com.boardgamegeek.ui.buddies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.model.User
import com.boardgamegeek.ui.BuddyActivity

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun BuddiesScreen(
    viewModel: BuddiesViewModel,
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val buddies by viewModel.buddies.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val syncEnabled by viewModel.syncEnabled.collectAsStateWithLifecycle()
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
        when (val items = buddies) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                if (items.isEmpty()) {
                    EmptyBuddies(
                        syncEnabled = syncEnabled,
                        onEnableSync = viewModel::enableSyncAndRefresh
                    )
                } else {
                    val grouped = remember(items, viewModel.sortType.value) {
                        items.groupBy { buddy -> viewModel.sectionHeader(buddy) }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        grouped.forEach { (header, groupItems) ->
                            item(key = "header-$header") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = header,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(groupItems, key = { it.username }) { buddy ->
                                BuddyRow(buddy = buddy)
                                HorizontalDivider()
                            }
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

    if (!errorMessage.isNullOrBlank()) {
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage.orEmpty())
            viewModel.clearError()
        }
    }
}

@Composable
private fun EmptyBuddies(
    syncEnabled: Boolean,
    onEnableSync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (syncEnabled) {
                stringResource(R.string.empty_buddies)
            } else {
                stringResource(R.string.empty_buddies_sync_off)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!syncEnabled) {
            Button(
                onClick = onEnableSync,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(text = stringResource(R.string.sync))
            }
        }
    }
}

@Composable
private fun BuddyRow(
    buddy: User,
) {
    val displayName = if (buddy.fullName.isBlank()) buddy.username else buddy.fullName
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { BuddyActivity.start(context, buddy.username, buddy.fullName) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = buddy.avatarUrl,
                contentDescription = stringResource(R.string.avatar),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.thumbnail_list_size))
                    .align(Alignment.CenterStart),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.person_image_empty),
                error = painterResource(R.drawable.person_image_empty)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimensionResource(R.dimen.thumbnail_list_size) + 12.dp)
            ) {
                Text(text = displayName, style = MaterialTheme.typography.titleMedium)
                if (buddy.fullName.isNotBlank()) {
                    Text(
                        text = buddy.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
