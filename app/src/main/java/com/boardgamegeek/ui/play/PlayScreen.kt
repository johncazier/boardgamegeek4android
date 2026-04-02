package com.boardgamegeek.ui.play

import android.widget.Chronometer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.asMinutes
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asScore
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.startTimerWithSystemTime
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.ui.components.HtmlText
import com.boardgamegeek.ui.navigation.BuddyRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.play.PlayViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayScreen(
    viewModel: PlayViewModel,
    contentPadding: PaddingValues = PaddingValues(),
    onThumbnailClicked: (Play) -> Unit,
    onEndTimerClicked: (Play) -> Unit,
) {
    val context = LocalContext.current
    val play by viewModel.play.collectAsStateWithLifecycle()
    val relatedExpansionPlays by viewModel.relatedExpansionPlays.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingFlow.collectAsStateWithLifecycle()
    val markupConverter = remember(context) { XmlApiMarkupConverter(context) }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.errorMessageEvents.collect { message ->
            errorMessage = message
        }
    }

    LaunchedEffect(play) {
        if (play != null) {
            errorMessage = null
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = viewModel::refresh,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .pullRefresh(pullRefreshState)
    ) {
        when {
            play != null -> {
                PlayContent(
                    play = play!!,
                    relatedExpansionPlays = relatedExpansionPlays,
                    markupConverter = markupConverter,
                    onThumbnailClicked = onThumbnailClicked,
                    onEndTimerClicked = onEndTimerClicked,
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.empty_play),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
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
private fun PlayContent(
    play: Play,
    relatedExpansionPlays: List<Play>,
    markupConverter: XmlApiMarkupConverter,
    onThumbnailClicked: (Play) -> Unit,
    onEndTimerClicked: (Play) -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.image_header_height))
                    .clickable { onThumbnailClicked(play) },
            ) {
                AsyncImage(
                    model = play.robustHeroImageUrl,
                    contentDescription = play.gameName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                )
                Text(
                    text = play.gameName,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                LabeledValueRow(label = stringResource(R.string.on), value = play.dateForDisplay(context).toString())

                if (play.location.isNotBlank()) {
                    LabeledValueRow(label = stringResource(R.string.at), value = play.location)
                }

                when {
                    play.length > 0 -> {
                        LabeledValueRow(label = stringResource(R.string.for_), value = play.length.asMinutes(context))
                    }

                    play.hasStarted() -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.for_),
                                modifier = Modifier.weight(0.2f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            AndroidView(
                                modifier = Modifier.weight(0.8f),
                                factory = { Chronometer(it) },
                                update = { chronometer -> chronometer.startTimerWithSystemTime(play.startTime) },
                            )
                            IconButton(onClick = { onEndTimerClicked(play) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_outline_timer_off_24),
                                    contentDescription = stringResource(R.string.timer),
                                )
                            }
                        }
                    }
                }

                if (play.quantity != 1) {
                    Text(
                        text = pluralStringResource(R.plurals.times_suffix, play.quantity, play.quantity),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (play.incomplete) {
                    Text(
                        text = stringResource(R.string.incomplete),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (play.noWinStats) {
                    Text(
                        text = stringResource(R.string.noWinStats),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (play.comments.isNotBlank()) {
            item {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.comments),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
                HtmlText(
                    html = markupConverter.toHtml(play.comments),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (play.players.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                Text(
                    text = stringResource(R.string.title_players),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
            }

            items(play.sortedPlayers, key = { it.uiId }) { player ->
                PlayPlayerRow(player = player)
            }
        }

        if (relatedExpansionPlays.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                Text(
                    text = stringResource(R.string.expansions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    relatedExpansionPlays
                        .map { it.gameName }
                        .distinct()
                        .sorted()
                        .forEach { expansionName ->
                            Text(
                                text = expansionName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val pendingText = when {
                    play.deleteTimestamp > 0 -> context.getString(R.string.delete_pending_prefix, play.deleteTimestamp.formatTimestamp(context))
                    play.updateTimestamp > 0 -> context.getString(R.string.update_pending_prefix, play.updateTimestamp.formatTimestamp(context))
                    else -> null
                }
                if (!pendingText.isNullOrBlank()) {
                    FooterText(pendingText)
                }

                if (play.dirtyTimestamp > 0) {
                    val dirtyPrefix = if (play.isSynced) R.string.editing_prefix else R.string.draft_prefix
                    FooterText(context.getString(dirtyPrefix, play.dirtyTimestamp.formatTimestamp(context)))
                }

                if (play.playId > 0) {
                    FooterText(context.getString(R.string.play_id_prefix, play.playId.toString()))
                }

                if (play.syncTimestamp > 0) {
                    FooterText(context.getString(R.string.synced_prefix, play.syncTimestamp.formatTimestamp(context)))
                }
            }
        }
    }
}

@Composable
private fun LabeledValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.2f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.8f),
        )
    }
}

@Composable
private fun PlayPlayerRow(player: PlayPlayer) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val seatColor = remember(player.color) { player.color.asColorRgb() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = player.username.isNotBlank()) {
                navigator.navigate(BuddyRoute(player.username, player.name))
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(if (seatColor == android.graphics.Color.TRANSPARENT) Color.Transparent else Color(seatColor)),
            contentAlignment = Alignment.Center,
        ) {
            if (player.seat != PlayPlayer.SEAT_UNKNOWN) {
                Text(
                    text = player.startingPosition,
                    color = Color(seatColor.getTextColor()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val name = when {
                player.name.isBlank() && player.username.isBlank() -> {
                    if (player.seat == PlayPlayer.SEAT_UNKNOWN) {
                        stringResource(R.string.title_player)
                    } else {
                        stringResource(R.string.generic_player, player.seat)
                    }
                }
                player.name.isBlank() -> player.username
                else -> player.name
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (player.isWin) FontWeight.Bold else FontWeight.Normal,
            )

            if (player.name.isNotBlank() && player.username.isNotBlank()) {
                Text(
                    text = player.username,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (seatColor == android.graphics.Color.TRANSPARENT && player.color.isNotBlank()) {
                Text(text = player.color, style = MaterialTheme.typography.bodySmall)
            }

            if (player.seat == PlayPlayer.SEAT_UNKNOWN && player.startingPosition.isNotBlank()) {
                Text(text = player.startingPosition, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (player.rating != 0.0) {
            Text(
                text = player.rating.asPersonalRating(context),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (player.score.isNotBlank()) {
            val scoreText = player.numericScore?.asScore(context) ?: player.score
            Text(
                text = scoreText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (player.isWin) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 2.dp),
    )
}
