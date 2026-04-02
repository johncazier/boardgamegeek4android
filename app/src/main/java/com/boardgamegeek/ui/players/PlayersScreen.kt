package com.boardgamegeek.ui.players

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.buddy.BuddyLauncher

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayersScreen(
    viewModel: PlayersViewModel,
    paddingValues: PaddingValues,
    showFilter: Boolean,
    onShowFilterChange: (Boolean) -> Unit,
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when (val items = players) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                val hasFilter = filter.isNotBlank()
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_players),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                if (items.isNotEmpty() || hasFilter) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (showFilter) {
                            OutlinedTextField(
                                value = filter,
                                onValueChange = viewModel::filter,
                                label = { Text(stringResource(R.string.title_filter)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                singleLine = true
                            )
                        } else if (hasFilter) {
                            onShowFilterChange(true)
                        }
                        val grouped = remember(items, viewModel.sortType.value, filter) {
                            items.groupBy { player -> viewModel.sectionHeader(player) }
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
                                items(groupItems, key = { it.id }) { player ->
                                    PlayerRow(
                                        player = player,
                                        displayText = viewModel.getDisplayText(player),
                                        onClick = { BuddyLauncher.start(it, player.username, player.name) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    displayText: String,
    onClick: (android.content.Context) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(context) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = buildDisplayName(player),
            style = MaterialTheme.typography.titleMedium
        )
        if (!player.username.isNullOrBlank()) {
            Text(
                text = player.username.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildDisplayName(player: Player) = buildAnnotatedString {
    val fullName = player.userFullName
    val name = player.name
    if (fullName.isNullOrBlank()) {
        append(name)
    } else if (fullName.contains(name)) {
        val splits = fullName.split(name)
        splits.forEachIndexed { index, split ->
            if (index > 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(name)
                }
            }
            append(split)
        }
    } else {
        append(fullName)
        append(" (")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(name)
        }
        append(")")
    }
}
