package com.boardgamegeek.ui.buddy

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.PlayerColor
import com.boardgamegeek.model.User

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun BuddyScreen(
    buddy: User?,
    player: Player?,
    colors: List<PlayerColor>?,
    isRefreshing: Boolean,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
    onEditNickname: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenPlays: () -> Unit,
    onOpenColors: () -> Unit,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
    val showPlays = (player?.playCount ?: 0) > 0 || (player?.winCount ?: 0) > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pullRefresh(pullRefreshState)
    ) {
        if (player == null && buddy == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    if (buddy != null) {
                        BuddyHeader(buddy = buddy)
                    }
                }
                item {
                    NicknameCard(name = player?.name.orEmpty(), onEditNickname = onEditNickname)
                }
                item {
                    if (buddy != null) {
                        ActionCard(
                            icon = { Icon(Icons.Default.CollectionsBookmark, contentDescription = null) },
                            label = stringResource(R.string.title_collection),
                            onClick = onOpenCollection
                        )
                    }
                }
                item {
                    if (showPlays) {
                        val playCount = player?.playCount ?: 0
                        val winCount = player?.winCount ?: 0
                        val winPercent = if (playCount > 0) ((winCount.toDouble() / playCount.toDouble()) * 100.0).toInt() else 0
                        PlaysCard(
                            plays = context.resources.getQuantityString(R.plurals.winnable_plays_suffix, playCount, playCount),
                            wins = context.resources.getQuantityString(R.plurals.wins_suffix, winCount, winCount),
                            percentage = context.getString(R.string.percentage, winPercent),
                            onClick = onOpenPlays,
                        )
                    }
                }
                item {
                    ColorsCard(colors = colors.orEmpty(), onClick = onOpenColors)
                }
                item {
                    if (buddy != null) {
                        val updatedText = if (buddy.updatedTimestamp > 0L) {
                            stringResource(R.string.updated_prefix, buddy.updatedTimestamp.formatTimestamp(context))
                        } else {
                            stringResource(R.string.needs_updating)
                        }
                        Text(
                            text = updatedText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (canRefresh) {
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun BuddyHeader(buddy: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = buddy.avatarUrl,
            contentDescription = stringResource(R.string.avatar),
            placeholder = painterResource(R.drawable.person_image_empty),
            error = painterResource(R.drawable.person_image_empty),
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = buddy.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buddy.username,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NicknameCard(
    name: String,
    onEditNickname: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.nickname), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditNickname)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.title_edit_nickname),
                )
            }
            Text(
                text = stringResource(R.string.nickname_description),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PlaysCard(
    plays: String,
    wins: String,
    percentage: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Default.EventAvailable, contentDescription = stringResource(R.string.title_plays))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = plays, style = MaterialTheme.typography.bodyMedium)
                Text(text = wins, style = MaterialTheme.typography.bodyMedium)
                Text(text = percentage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ColorsCard(
    colors: List<PlayerColor>,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Default.Palette, contentDescription = stringResource(R.string.title_favorite_colors))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.title_favorite_colors),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colors.take(3).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(color.rgb))
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
