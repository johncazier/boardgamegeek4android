package com.boardgamegeek.ui.playstats

import android.content.Context
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.showClickableAlertDialog
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.HIndex
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.ui.players.PlayersActivity
import java.text.DecimalFormat
import java.util.Locale

@Composable
fun PlayStatsScreen(
    viewModel: PlayStatsViewModel,
    paddingValues: PaddingValues,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val playStats by viewModel.playStats.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val includeIncomplete by viewModel.includeIncomplete.collectAsStateWithLifecycle()
    val includeExpansions by viewModel.includeExpansions.collectAsStateWithLifecycle()
    val includeAccessories by viewModel.includeAccessories.collectAsStateWithLifecycle()
    val syncStatuses by viewModel.syncCollectionStatuses.collectAsStateWithLifecycle()

    var showCollectionStatusDialog by remember { mutableStateOf(false) }
    var showIncludeSettingsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (playStats == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        val isOwnedSynced = syncStatuses.contains(CollectionStatus.Own)
        val isPlayedSynced = syncStatuses.contains(CollectionStatus.Played)
        val accuracyMessage = accuracyMessage(
            context = context,
            includeIncomplete = includeIncomplete == true,
            includeExpansions = includeExpansions == true,
            includeAccessories = includeAccessories == true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionTitle(title = stringResource(R.string.title_play_count))
            Card(modifier = Modifier.fillMaxWidth()) {
                val rows = remember(playStats, isPlayedSynced) {
                    buildPlayCountRows(context, playStats!!, isPlayedSynced)
                }
                StatList(rows = rows, onInfoClick = { titleRes, infoRes ->
                    context.showClickableAlertDialog(titleRes, infoRes)
                })
            }

            Spacer(modifier = Modifier.height(16.dp))

            HIndexSection(
                titleRes = R.string.play_stat_game_h_index,
                value = playStats!!.hIndex.description,
                onInfoClick = {
                    context.showClickableAlertDialog(
                        R.string.play_stat_game_h_index,
                        R.string.play_stat_game_h_index_info,
                        playStats!!.hIndex.h,
                        playStats!!.hIndex.n
                    )
                }
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                val rows = remember(playStats) { buildHIndexRows(playStats!!.hIndex, playStats!!.getHIndexGames()) }
                HIndexTable(rows = rows)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (playerStats != null) {
                HIndexSection(
                    titleRes = R.string.play_stat_player_h_index,
                    value = playerStats!!.hIndex.description,
                    onInfoClick = {
                        context.showClickableAlertDialog(
                            R.string.play_stat_player_h_index,
                            R.string.play_stat_player_h_index_info,
                            playerStats!!.hIndex.h,
                            playerStats!!.hIndex.n
                        )
                    }
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { PlayersActivity.startByPlayCount(context) }
                ) {
                    val rows = remember(playerStats) { buildHIndexRows(playerStats!!.hIndex, playerStats!!.hIndexPlayers) }
                    HIndexTable(rows = rows)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val advancedRows = remember(playStats) { buildAdvancedRows(context, playStats!!) }
            if (advancedRows.isNotEmpty()) {
                SectionTitle(title = stringResource(R.string.title_advanced))
                Card(modifier = Modifier.fillMaxWidth()) {
                    StatList(rows = advancedRows, onInfoClick = { titleRes, infoRes ->
                        context.showClickableAlertDialog(titleRes, infoRes)
                    })
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isOwnedSynced || !isPlayedSynced) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.play_stat_collection_status),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showCollectionStatusDialog = true }) {
                        Text(stringResource(R.string.modify))
                    }
                }
            }

            if (!accuracyMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = accuracyMessage,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showIncludeSettingsDialog = true }) {
                        Text(stringResource(R.string.edit))
                    }
                }
            }
        }

        if (showCollectionStatusDialog) {
            AlertDialog(
                onDismissRequest = { showCollectionStatusDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCollectionStatusDialog = false
                            viewModel.enableOwnedPlayedSync()
                        }
                    ) {
                        Text(stringResource(R.string.modify))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCollectionStatusDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                title = { Text(stringResource(R.string.title_modify_collection_status)) },
                text = { Text(stringResource(R.string.msg_modify_collection_status)) }
            )
        }

        if (showIncludeSettingsDialog) {
            IncludeSettingsDialog(
                initialIncomplete = includeIncomplete == true,
                initialExpansions = includeExpansions == true,
                initialAccessories = includeAccessories == true,
                onConfirm = { incomplete, expansions, accessories ->
                    viewModel.setIncludeSettings(incomplete, expansions, accessories)
                    showIncludeSettingsDialog = false
                },
                onDismiss = { showIncludeSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun HIndexSection(
    @StringRes titleRes: Int,
    value: String,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 8.dp)
        )
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.information),
            )
        }
    }
}

@Composable
private fun StatList(
    rows: List<StatRow>,
    onInfoClick: (Int, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            if (row.isDivider) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.dark_blue)
                )
            } else {
                StatRowView(row = row, onInfoClick = onInfoClick)
            }
        }
    }
}

@Composable
private fun StatRowView(
    row: StatRow,
    onInfoClick: (Int, Int) -> Unit,
) {
    val background = if (row.highlighted) colorResource(R.color.light_blue) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = row.value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
        if (row.infoRes != null && row.titleRes != null) {
            IconButton(
                onClick = { onInfoClick(row.titleRes, row.infoRes) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.information)
                )
            }
        }
    }
}

@Composable
private fun HIndexTable(rows: List<HIndexRowItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            when (row) {
                is HIndexRowItem.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = colorResource(R.color.dark_blue)
                    )
                }
                is HIndexRowItem.Row -> {
                    StatRowView(
                        row = StatRow(
                            label = row.label,
                            value = row.value.toString(),
                            highlighted = row.highlighted
                        ),
                        onInfoClick = { _, _ -> }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncludeSettingsDialog(
    initialIncomplete: Boolean,
    initialExpansions: Boolean,
    initialAccessories: Boolean,
    onConfirm: (Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var includeIncomplete by remember(initialIncomplete) { mutableStateOf(initialIncomplete) }
    var includeExpansions by remember(initialExpansions) { mutableStateOf(initialExpansions) }
    var includeAccessories by remember(initialAccessories) { mutableStateOf(initialAccessories) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_settings)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(includeIncomplete, includeExpansions, includeAccessories) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingToggle(
                    label = stringResource(R.string.incomplete_plays),
                    checked = includeIncomplete,
                    onCheckedChange = { includeIncomplete = it }
                )
                SettingToggle(
                    label = stringResource(R.string.expansions),
                    checked = includeExpansions,
                    onCheckedChange = { includeExpansions = it }
                )
                SettingToggle(
                    label = stringResource(R.string.accessories),
                    checked = includeAccessories,
                    onCheckedChange = { includeAccessories = it }
                )
            }
        }
    )
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private data class StatRow(
    val label: String,
    val value: String,
    @StringRes val infoRes: Int? = null,
    @StringRes val titleRes: Int? = null,
    val highlighted: Boolean = false,
    val isDivider: Boolean = false,
)

private sealed interface HIndexRowItem {
    data class Row(val label: String, val value: Int, val highlighted: Boolean = false) : HIndexRowItem
    data object Divider : HIndexRowItem
}

private fun buildPlayCountRows(context: Context, stats: PlayStats, isPlayedSynced: Boolean): List<StatRow> {
    val rows = mutableListOf<StatRow>()
    fun maybeAdd(@StringRes labelRes: Int, value: Int) {
        if (value > 0) {
            rows.add(
                StatRow(
                    label = context.getString(labelRes),
                    value = value.toString(),
                    titleRes = labelRes,
                )
            )
        }
    }
    maybeAdd(R.string.play_stat_play_count, stats.numberOfPlays)
    maybeAdd(R.string.play_stat_distinct_games, stats.numberOfPlayedGames)
    maybeAdd(R.string.play_stat_dollars, stats.numberOfDollars)
    maybeAdd(R.string.play_stat_half_dollars, stats.numberOfHalfDollars)
    maybeAdd(R.string.play_stat_quarters, stats.numberOfQuarters)
    maybeAdd(R.string.play_stat_dimes, stats.numberOfDimes)
    maybeAdd(R.string.play_stat_nickels, stats.numberOfNickels)

    if (isPlayedSynced) {
        rows.add(
            StatRow(
                label = context.getString(R.string.play_stat_top_100),
                value = "${stats.top100Count}%",
                titleRes = R.string.play_stat_top_100
            )
        )
    }
    return rows
}

private fun buildAdvancedRows(context: Context, stats: PlayStats): List<StatRow> {
    val rows = mutableListOf<StatRow>()
    if (stats.gIndex.isValid()) {
        rows.add(
            StatRow(
                label = context.getString(R.string.g_index),
                value = stats.gIndex.description,
                infoRes = R.string.play_stat_game_g_index_info,
                titleRes = R.string.g_index
            )
        )
    }
    if (stats.friendless != PlayStats.INVALID_FRIENDLESS) {
        rows.add(
            StatRow(
                label = context.getString(R.string.play_stat_friendless),
                value = stats.friendless.toString(),
                infoRes = R.string.play_stat_friendless_info,
                titleRes = R.string.play_stat_friendless
            )
        )
    }
    if (stats.utilization != PlayStats.INVALID_UTILIZATION) {
        rows.add(
            StatRow(
                label = context.getString(R.string.play_stat_utilization),
                value = stats.utilization.asPercentage(),
                infoRes = R.string.play_stat_utilization_info,
                titleRes = R.string.play_stat_utilization
            )
        )
    }
    if (stats.cfm != PlayStats.INVALID_CFM) {
        rows.add(
            StatRow(
                label = context.getString(R.string.play_stat_cfm),
                value = DOUBLE_FORMAT.format(stats.cfm),
                infoRes = R.string.play_stat_cfm_info,
                titleRes = R.string.play_stat_cfm
            )
        )
    }
    return rows
}

private fun buildHIndexRows(hIndex: HIndex, entries: List<Pair<String, Int>>?): List<HIndexRowItem> {
    if (entries.isNullOrEmpty()) return emptyList()
    val rankedEntries = entries.filter { pair -> pair.first.isNotBlank() && pair.second > 0 }
        .mapIndexed { index, pair -> "${pair.first} (#${index + 1})" to pair.second }

    val nextHighestHIndex = entries.findLast { it.second > hIndex.h }?.second ?: (hIndex.h + 1)
    val nextLowestHIndex = entries.find { it.second < hIndex.h }?.second ?: (hIndex.h - 1)

    val rows = mutableListOf<HIndexRowItem>()
    val prefix = rankedEntries.filter { it.second == nextHighestHIndex && it.first.isNotBlank() }
    prefix.forEach { rows.add(HIndexRowItem.Row(it.first, it.second)) }

    val list = rankedEntries.filter { it.second == hIndex.h && it.first.isNotBlank() }
    if (list.isEmpty()) {
        rows.add(HIndexRowItem.Divider)
    } else {
        list.forEach { rows.add(HIndexRowItem.Row(it.first, it.second, highlighted = true)) }
    }

    val suffix = rankedEntries.filter { it.second == nextLowestHIndex }
    suffix.forEach { rows.add(HIndexRowItem.Row(it.first, it.second)) }
    return rows
}

private fun accuracyMessage(
    context: Context,
    includeIncomplete: Boolean,
    includeExpansions: Boolean,
    includeAccessories: Boolean,
): String? {
    val messages = ArrayList<String>(3)
    if (!includeIncomplete) {
        messages.add(context.getString(R.string.incomplete_plays).lowercase(Locale.getDefault()))
    }
    if (!includeExpansions) {
        messages.add(context.getString(R.string.expansions).lowercase(Locale.getDefault()))
    }
    if (!includeAccessories) {
        messages.add(context.getString(R.string.accessories).lowercase(Locale.getDefault()))
    }
    return if (messages.isEmpty()) {
        null
    } else {
        context.getString(
            R.string.play_stat_accuracy,
            messages.formatList(context.getString(R.string.or).lowercase(Locale.getDefault()))
        )
    }
}

private val DOUBLE_FORMAT = DecimalFormat("0.00")
