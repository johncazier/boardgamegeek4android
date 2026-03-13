package com.boardgamegeek.ui.playssummary

import android.text.format.DateUtils
import android.widget.ImageView
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.model.PlayerColor
import com.boardgamegeek.ui.BuddyActivity
import com.boardgamegeek.ui.playstats.PlayStatsActivity
import com.boardgamegeek.ui.locations.LocationsActivity
import com.boardgamegeek.ui.plays.LocationActivity
import com.boardgamegeek.ui.plays.PlaysActivity
import com.boardgamegeek.ui.play.PlayActivity
import com.boardgamegeek.ui.players.PlayersActivity
import com.boardgamegeek.ui.startActivity

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlaysSummaryScreen(
    viewModel: PlaysSummaryViewModel,
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRefreshing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncPlays by viewModel.syncPlays.collectAsStateWithLifecycle()
    val syncPlaysTimestamp by viewModel.syncPlaysTimestamp.collectAsStateWithLifecycle()
    val oldestSyncDate by viewModel.oldestSyncDate.collectAsStateWithLifecycle()
    val newestSyncDate by viewModel.newestSyncDate.collectAsStateWithLifecycle()
    val playsInProgress by viewModel.playsInProgress.collectAsStateWithLifecycle()
    val playsNotInProgress by viewModel.playsNotInProgress.collectAsStateWithLifecycle()
    val playCount by viewModel.playCount.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val hIndex by viewModel.hIndex.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val showSyncCard = syncPlays != true && (syncPlaysTimestamp ?: 0L) <= 0L
            if (showSyncCard) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.msg_play_sync),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(onClick = { viewModel.enableSyncing(false) }) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { viewModel.enableSyncing(true) }) {
                                Text(stringResource(R.string.sync))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.title_plays),
                actionText = playsMoreText(context, playCount),
                onAction = { context.startActivity<PlaysActivity>() }
            )

            if (playsInProgress.isNotEmpty() || playsNotInProgress.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (playsInProgress.isNotEmpty()) {
                            SectionSubtitle(text = stringResource(R.string.title_in_progress))
                            SummaryList(
                                items = playsInProgress,
                                itemTitle = { it.gameName },
                                itemSubtitle = { it.describe(context, true) },
                                onItemClick = { play -> PlayActivity.start(context, play.internalId) }
                            )
                            HorizontalDivider()
                        }
                        if (playsNotInProgress.isNotEmpty()) {
                            SectionSubtitle(text = stringResource(R.string.title_recent))
                            SummaryList(
                                items = playsNotInProgress,
                                itemTitle = { it.gameName },
                                itemSubtitle = { it.describe(context, true) },
                                onItemClick = { play -> PlayActivity.start(context, play.internalId) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.title_players),
                actionText = stringResource(R.string.more),
                onAction = { context.startActivity<PlayersActivity>() },
                actionVisible = players.isNotEmpty()
            )

            if (players.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SummaryList(
                        items = players,
                        itemTitle = { it.description },
                        itemSubtitle = { context.getQuantityText(R.plurals.plays_suffix, it.playCount, it.playCount).toString() },
                        onItemClick = { player -> BuddyActivity.start(context, player.username, player.name) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.title_locations),
                actionText = stringResource(R.string.more),
                onAction = { context.startActivity<LocationsActivity>() },
                actionVisible = locations.isNotEmpty()
            )

            if (locations.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SummaryList(
                        items = locations,
                        itemTitle = { it.name },
                        itemSubtitle = { context.getQuantityText(R.plurals.plays_suffix, it.playCount, it.playCount).toString() },
                        onItemClick = { location -> LocationActivity.start(context, location.name) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.title_favorite_colors),
                actionText = stringResource(R.string.edit),
                onAction = {
                    val resolvedUsername = username ?: context.preferences()[AccountPreferences.KEY_USERNAME, ""]
                    if (resolvedUsername.isNullOrBlank()) {
                        context.toast("Can't figure out your username.")
                    } else {
                        com.boardgamegeek.ui.PlayerColorsActivity.start(context, resolvedUsername, null)
                    }
                },
                actionVisible = true
            )

            if (colors.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.color_circle_diameter_small_margin))
                    ) {
                        colors.forEach { color ->
                            ColorCircle(color = color)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.title_play_stats),
                actionText = stringResource(R.string.more),
                onAction = { context.startActivity<PlayStatsActivity>() }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                val htmlText = stringResource(R.string.game_h_index_prefix, hIndex.description)
                Text(
                    text = AnnotatedString.fromHtml(htmlText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val syncStatusText = when {
                (oldestSyncDate ?: Long.MAX_VALUE) == Long.MAX_VALUE && (newestSyncDate ?: 0L) <= 0L ->
                    stringResource(R.string.plays_sync_status_none)
                (oldestSyncDate ?: 0L) <= 0L ->
                    stringResource(R.string.plays_sync_status_new, (newestSyncDate ?: 0L).asDate(context))
                (newestSyncDate ?: 0L) <= 0L ->
                    stringResource(R.string.plays_sync_status_old, (oldestSyncDate ?: Long.MAX_VALUE).asDate(context))
                else ->
                    stringResource(
                        R.string.plays_sync_status_range,
                        (oldestSyncDate ?: Long.MAX_VALUE).asDate(context),
                        (newestSyncDate ?: 0L).asDate(context)
                    )
            }
            Text(
                text = syncStatusText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit,
    actionVisible: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (actionVisible) {
            TextButton(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
private fun SectionSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun <T> SummaryList(
    items: List<T>,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    onItemClick: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            SummaryRow(
                title = itemTitle(item),
                subtitle = itemSubtitle(item),
                onClick = { onItemClick(item) }
            )
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SummaryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ColorCircle(color: PlayerColor) {
    val size = dimensionResource(R.dimen.color_circle_diameter_small)
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setColorViewValue(color.rgb)
            }
        },
        update = { view ->
            view.setColorViewValue(color.rgb)
        },
        modifier = Modifier.size(size)
    )
}

private fun playsMoreText(context: android.content.Context, playCount: Int): String {
    val moreCount = playCount - PlaysSummaryViewModel.ITEMS_TO_DISPLAY
    return if (moreCount > 0) {
        context.getString(R.string.more_suffix, moreCount)
    } else {
        context.getString(R.string.more)
    }
}

private fun Long.asDate(context: android.content.Context): String {
    return formatDateTime(
        context,
        flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL
    )
}
