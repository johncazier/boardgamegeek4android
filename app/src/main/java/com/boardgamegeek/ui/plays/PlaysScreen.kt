package com.boardgamegeek.ui.plays

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_FORM
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_QUICK
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.logPlayPreference
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.logplay.LogPlayLauncher
import com.boardgamegeek.ui.play.PlayLauncher
import com.boardgamegeek.util.XmlApiMarkupConverter
import java.text.SimpleDateFormat
import java.util.Locale

private sealed interface PlayListRow {
    data class Header(val text: String) : PlayListRow
    data class Item(val play: Play) : PlayListRow
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel,
    @StringRes emptyStringResId: Int,
    showGameName: Boolean,
    gameId: Int,
    gameName: String,
    heroImageUrl: String,
    arePlayersCustomSorted: Boolean,
    @ColorInt iconColor: Int,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val plays by viewModel.plays.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val markupConverter = remember(context) { XmlApiMarkupConverter(context) }

    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var showSendConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(plays) {
        selectedIds = selectedIds.filter { id -> plays.any { it.internalId == id } }.toSet()
    }

    val selectedPlays = remember(plays, selectedIds) { plays.filter { selectedIds.contains(it.internalId) } }
    val allSelectedPending = selectedPlays.all { it.dirtyTimestamp > 0 }

    val emptyTextRes = when (filterType) {
        PlaysViewModel.FilterType.DIRTY -> R.string.empty_plays_draft
        PlaysViewModel.FilterType.PENDING -> R.string.empty_plays_pending
        PlaysViewModel.FilterType.ALL -> if (context.preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] == true) {
            emptyStringResId
        } else {
            R.string.empty_plays_sync_off
        }
    }

    val rows = remember(plays, sortType) { buildPlayRows(context, plays, sortType) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.padding(contentPadding),
        floatingActionButton = {
            if (gameId != INVALID_ID && selectedIds.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        when (context.preferences().logPlayPreference()) {
                            LOG_PLAY_TYPE_FORM -> LogPlayLauncher.logPlay(context, gameId, gameName, heroImageUrl, arePlayersCustomSorted)
                            LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName)
                            else -> LogPlayLauncher.logPlay(context, gameId, gameName, heroImageUrl, arePlayersCustomSorted)
                        }
                    },
                    containerColor = iconColor
                        .takeIf { it != Color.TRANSPARENT }
                        ?.let { androidx.compose.ui.graphics.Color(it) }
                        ?: MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.title_log_play))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.resources.getQuantityString(R.plurals.msg_plays_selected, selectedIds.size, selectedIds.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row {
                        IconButton(enabled = allSelectedPending, onClick = { showSendConfirm = true }) {
                            Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send))
                        }
                        IconButton(enabled = selectedIds.size == 1, onClick = {
                            selectedPlays.firstOrNull()?.let { play ->
                                LogPlayLauncher.editPlay(context, play.internalId, play.gameId, play.gameName, play.robustHeroImageUrl)
                                selectedIds = emptySet()
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }

            if (rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(emptyTextRes), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(rows) { row ->
                        when (row) {
                            is PlayListRow.Header -> {
                                Text(
                                    text = row.text,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            is PlayListRow.Item -> {
                                val isSelected = selectedIds.contains(row.play.internalId)
                                PlayRow(
                                    play = row.play,
                                    showGameName = showGameName,
                                    markupConverter = markupConverter,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (selectedIds.isEmpty()) {
                                            PlayLauncher.start(context, row.play.internalId)
                                        } else {
                                            selectedIds = selectedIds.toggle(row.play.internalId)
                                        }
                                    },
                                    onLongClick = {
                                        selectedIds = selectedIds.toggle(row.play.internalId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSendConfirm) {
        AlertDialog(
            onDismissRequest = { showSendConfirm = false },
            title = { Text(stringResource(R.string.send)) },
            text = { Text(context.resources.getQuantityString(R.plurals.are_you_sure_send_play, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.send(selectedPlays)
                    selectedIds = emptySet()
                    showSendConfirm = false
                }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = {
                TextButton(onClick = { showSendConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(context.resources.getQuantityString(R.plurals.are_you_sure_delete_play, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(selectedPlays)
                    selectedIds = emptySet()
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayRow(
    play: Play,
    showGameName: Boolean,
    markupConverter: XmlApiMarkupConverter,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val statusMessageId = when {
        play.deleteTimestamp > 0 -> R.string.sync_pending_delete
        play.updateTimestamp > 0 -> R.string.sync_pending_update
        play.dirtyTimestamp > 0 -> if (play.isSynced) R.string.sync_editing else R.string.sync_draft
        else -> 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (showGameName) play.gameName else play.dateForDisplay(context).toString(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = play.describe(context, showGameName).toString(),
                style = MaterialTheme.typography.bodyMedium
            )
            val comment = remember(play.comments) { markupConverter.strip(play.comments) }
            if (comment.isNotBlank()) {
                Text(text = comment, style = MaterialTheme.typography.bodySmall)
            }
            if (statusMessageId != 0) {
                Text(text = stringResource(statusMessageId), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun buildPlayRows(
    context: android.content.Context,
    plays: List<Play>,
    sortType: PlaysViewModel.SortType,
): List<PlayListRow> {
    val rows = mutableListOf<PlayListRow>()
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun headerFor(play: Play): String {
        return when (sortType) {
            PlaysViewModel.SortType.DATE -> {
                if (play.dateInMillis == Play.UNKNOWN_DATE) {
                    context.getString(R.string.text_unknown)
                } else {
                    dateFormat.format(play.dateInMillis)
                }
            }
            PlaysViewModel.SortType.LOCATION -> play.location.ifBlank { context.getString(R.string.no_location) }
            PlaysViewModel.SortType.GAME -> play.gameName
            PlaysViewModel.SortType.LENGTH -> {
                val minutes = play.length
                when {
                    minutes == 0 -> context.getString(R.string.no_length)
                    minutes >= 120 -> "${(minutes / 60)}+ ${context.getString(R.string.hours_abbr)}"
                    minutes >= 60 -> "${(minutes / 10 * 10)}+ ${context.getString(R.string.minutes_abbr)}"
                    minutes >= 30 -> "${(minutes / 5 * 5)}+ ${context.getString(R.string.minutes_abbr)}"
                    else -> "$minutes ${context.getString(R.string.minutes_abbr)}"
                }
            }
        }
    }

    var lastHeader: String? = null
    plays.forEach { play ->
        val header = headerFor(play)
        if (header != lastHeader) {
            rows += PlayListRow.Header(header)
            lastHeader = header
        }
        rows += PlayListRow.Item(play)
    }
    return rows
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return if (contains(id)) this - id else this + id
}
