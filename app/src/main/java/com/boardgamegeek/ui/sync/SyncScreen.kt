package com.boardgamegeek.ui.sync

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.widget.CollectionStatusSync

@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    paddingValues: PaddingValues,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.title_collection,
        R.string.title_plays,
        R.string.title_users,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, titleRes ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = stringResource(titleRes)) }
                )
            }
        }

        when (selectedTab) {
            0 -> SyncCollectionScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            1 -> SyncPlaysScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            2 -> SyncUsersScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SyncCollectionScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val syncCollectionStatuses by viewModel.syncCollectionStatuses.collectAsStateWithLifecycle()
    val collectionCompleteCurrentTimestamp by viewModel.collectionCompleteCurrentTimestamp.collectAsStateWithLifecycle()
    val collectionCompleteTimestamp by viewModel.collectionCompleteTimestamp.collectAsStateWithLifecycle()
    val collectionPartialTimestamp by viewModel.collectionPartialTimestamp.collectAsStateWithLifecycle()
    val numberOfUnsyncedGames by viewModel.numberOfUnsyncedGames.collectAsStateWithLifecycle()
    val numberOfCollectionItemsToUpload by viewModel.numberOfCollectionItemsToUpload.collectAsStateWithLifecycle()
    val collectionSyncProgress by viewModel.collectionSyncProgress.collectAsStateWithLifecycle()

    val isSyncing = collectionSyncProgress.step != SyncViewModel.CollectionSyncProgressStep.NotSyncing

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if ((collectionCompleteCurrentTimestamp ?: 0L) > 0L) {
            SyncStatusRow(
                label = stringResource(R.string.currently_syncing),
                value = (collectionCompleteCurrentTimestamp ?: 0L).asDateTime(context)
            )
        }
        if ((collectionCompleteTimestamp ?: 0L) > 0L) {
            SyncStatusRow(
                label = stringResource(R.string.complete),
                value = (collectionCompleteTimestamp ?: 0L).asDateTime(context)
            )
        }
        if ((collectionPartialTimestamp ?: 0L) > 0L) {
            SyncStatusRow(
                label = stringResource(R.string.partial),
                value = (collectionPartialTimestamp ?: 0L).asDateTime(context)
            )
        }

        Text(
            text = context.getQuantityText(R.plurals.games_pending_download, numberOfUnsyncedGames, numberOfUnsyncedGames).toString(),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { viewModel.cancelCollection() },
                enabled = isSyncing
            ) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { viewModel.syncCollection() },
                enabled = !isSyncing
            ) {
                Text(stringResource(R.string.sync))
            }
        }

        val syncMessage = collectionSyncMessage(context, collectionSyncProgress)
        if (!syncMessage.isNullOrBlank()) {
            Text(
                text = syncMessage,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (isSyncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = context.getQuantityText(R.plurals.items_pending_upload, numberOfCollectionItemsToUpload, numberOfCollectionItemsToUpload).toString(),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Button(
            onClick = { viewModel.uploadCollection() },
            enabled = numberOfCollectionItemsToUpload > 0,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.upload))
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(R.string.title_statuses),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        val statusEntries = listOf(
            CollectionStatus.Own to R.string.collection_status_own,
            CollectionStatus.PreviouslyOwned to R.string.collection_status_prev_owned,
            CollectionStatus.ForTrade to R.string.collection_status_for_trade,
            CollectionStatus.WantInTrade to R.string.collection_status_want_in_trade,
            CollectionStatus.WantToBuy to R.string.collection_status_want_to_buy,
            CollectionStatus.WantToPlay to R.string.collection_status_want_to_play,
            CollectionStatus.Preordered to R.string.collection_status_preordered,
            CollectionStatus.Wishlist to R.string.collection_status_wishlist,
            CollectionStatus.Played to R.string.played,
            CollectionStatus.Rated to R.string.collection_status_rated,
            CollectionStatus.Commented to R.string.collection_status_commented,
            CollectionStatus.HasParts to R.string.collection_status_has_parts,
            CollectionStatus.WantParts to R.string.collection_status_want_parts,
        )

        statusEntries.forEachIndexed { index, (status, labelRes) ->
            val defaultTimestamp by viewModel.collectionStatusCompleteTimestamp(status)
                .collectAsStateWithLifecycle(initialValue = null)
            val accessoryTimestamp by viewModel.collectionStatusAccessoryCompleteTimestamp(status)
                .collectAsStateWithLifecycle(initialValue = null)

            CollectionStatusRow(
                labelRes = labelRes,
                isChecked = syncCollectionStatuses.contains(status),
                defaultTimestamp = defaultTimestamp,
                accessoryTimestamp = accessoryTimestamp,
                inProgress = isSyncing && collectionSyncProgress.status == status,
                isOtherInProgress = isSyncing && collectionSyncProgress.status != status,
                onSyncClick = { viewModel.syncCollection(status) },
                onEnabledChange = { enabled -> viewModel.modifyCollectionStatus(status, enabled) }
            )

            if (index != statusEntries.lastIndex) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun SyncPlaysScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val syncPlays by viewModel.syncPlays.collectAsStateWithLifecycle()
    val playSyncState by viewModel.playSyncState.collectAsStateWithLifecycle()
    val playSyncProgress by viewModel.playSyncProgress.collectAsStateWithLifecycle()
    val numberOfPlaysToBeUpdated by viewModel.numberOfPlaysToBeUpdated.collectAsStateWithLifecycle()
    val numberOfPlaysToBeDeleted by viewModel.numberOfPlaysToBeDeleted.collectAsStateWithLifecycle()

    val enabled = syncPlays == true
    val isSyncing = playSyncProgress.step != SyncViewModel.PlaySyncProgressStep.NotSyncing

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = enabled,
                onCheckedChange = { viewModel.setSyncPlaysEnabled(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.sync_plays))
        }

        if (enabled) {
            val (oldest, newest, count) = playSyncState
            Text(
                text = context.getQuantityText(
                    when {
                        oldest == Long.MAX_VALUE && newest <= 0L -> R.plurals.plays_sync_status_none
                        oldest <= 0L -> R.plurals.plays_sync_status_new
                        newest <= 0L -> R.plurals.plays_sync_status_old
                        else -> R.plurals.plays_sync_status_range
                    },
                    count,
                    count,
                    oldest.asDate(context),
                    newest.asDate(context),
                ).toString(),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelPlays() },
                    enabled = isSyncing
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.syncPlays() },
                    enabled = !isSyncing
                ) {
                    Text(stringResource(R.string.sync_plays))
                }
            }
        }

        if (isSyncing) {
            val stepText = playsStepText(context, playSyncProgress)
            if (!stepText.isNullOrBlank()) {
                Text(text = stepText, modifier = Modifier.padding(vertical = 4.dp))
            }
            val rangeText = playsRangeText(context, playSyncProgress)
            if (!rangeText.isNullOrBlank()) {
                Text(text = rangeText, modifier = Modifier.padding(vertical = 4.dp))
            }
            val actionText = playsActionText(context, playSyncProgress)
            if (!actionText.isNullOrBlank()) {
                Text(text = actionText, modifier = Modifier.padding(vertical = 4.dp))
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        if (numberOfPlaysToBeUpdated > 0) {
            Text(
                text = context.getQuantityText(R.plurals.plays_pending_update, numberOfPlaysToBeUpdated, numberOfPlaysToBeUpdated).toString(),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        if (numberOfPlaysToBeDeleted > 0) {
            Text(
                text = context.getQuantityText(R.plurals.plays_pending_deletion, numberOfPlaysToBeDeleted, numberOfPlaysToBeDeleted).toString(),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = { viewModel.uploadPlays() },
            enabled = (numberOfPlaysToBeUpdated + numberOfPlaysToBeDeleted) > 0,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.upload_plays))
        }
    }
}

@Composable
private fun SyncUsersScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val syncBuddies by viewModel.syncBuddies.collectAsStateWithLifecycle()
    val buddySyncDate by viewModel.buddySyncDate.collectAsStateWithLifecycle()
    val userSyncState by viewModel.userSyncState.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()

    val enabled = syncBuddies == true
    val isSyncing = userProgress.step != SyncViewModel.UserSyncProgressStep.NotSyncing

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = enabled,
                onCheckedChange = { viewModel.setSyncBuddiesEnabled(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.sync))
        }

        if (enabled) {
            val buddiesText = when {
                (buddySyncDate ?: 0L) <= 0L -> stringResource(R.string.sync_buddies_date_zero)
                else -> stringResource(R.string.sync_buddies_date, buddySyncDate.asDateTime(context))
            }
            Text(text = buddiesText, modifier = Modifier.padding(bottom = 12.dp))

            Text(
                text = context.getQuantityText(
                    R.plurals.users_synced_total,
                    userSyncState.count,
                    userSyncState.count,
                    userSyncState.oldestUpdatedUserTimestamp.asDateTime(context),
                ).toString(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (userSyncState.numberOfUnupdatedUsers > 0) {
                Text(
                    text = context.getQuantityText(
                        R.plurals.users_unupdated_total,
                        userSyncState.numberOfUnupdatedUsers,
                        userSyncState.numberOfUnupdatedUsers
                    ).toString(),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { viewModel.cancelBuddies() },
                enabled = isSyncing
            ) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { viewModel.syncBuddies() },
                enabled = !isSyncing && enabled
            ) {
                Text(stringResource(R.string.sync_users))
            }
        }

        if (isSyncing) {
            val stepText = usersStepText(context, userProgress.step)
            if (!stepText.isNullOrBlank()) {
                Text(text = stepText, modifier = Modifier.padding(bottom = 8.dp))
            }
            val username = userProgress.username
            if (!username.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.sync_notification_user, username),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (userProgress.max > 0) {
                LinearProgressIndicator(
                    progress = userProgress.progress.toFloat() / userProgress.max.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CollectionStatusRow(
    labelRes: Int,
    isChecked: Boolean,
    defaultTimestamp: Long?,
    accessoryTimestamp: Long?,
    inProgress: Boolean,
    isOtherInProgress: Boolean,
    onSyncClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            CollectionStatusSync(ctx).apply {
                findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.statusSwitch).text = ctx.getString(labelRes)
            }
        },
        update = { view ->
            view.check(isChecked)
            view.setDefaultTimestamp(defaultTimestamp)
            view.setAccessoryTimestamp(accessoryTimestamp)
            view.onSyncClick(onSyncClick)
            view.setEnableListener(onEnabledChange)
            view.setProgress(inProgress, isOtherInProgress)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SyncStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = value)
    }
}

private fun Long.asDate(context: android.content.Context): String {
    return formatDateTime(
        context,
        flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

private fun Long.asDateTime(context: android.content.Context): String {
    return formatDateTime(
        context,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
    ).toString()
}

private fun Long?.asDateTime(context: android.content.Context): String {
    return this?.formatDateTime(
        context,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )?.toString() ?: context.getString(R.string.never)
}

private fun collectionSyncMessage(
    context: android.content.Context,
    progress: SyncViewModel.CollectionSyncProgress
): String? {
    if (progress.step == SyncViewModel.CollectionSyncProgressStep.NotSyncing) return null
    val subtype = context.getString(
        when (progress.subtype) {
            SyncViewModel.CollectionSyncProgressSubtype.None -> R.string.items
            SyncViewModel.CollectionSyncProgressSubtype.All -> R.string.games_expansions
            SyncViewModel.CollectionSyncProgressSubtype.Accessory -> R.string.accessories
        }
    )
    val statusDescription = context.getString(
        when (progress.status) {
            CollectionStatus.Own -> R.string.collection_status_own
            CollectionStatus.PreviouslyOwned -> R.string.collection_status_prev_owned
            CollectionStatus.Preordered -> R.string.collection_status_preordered
            CollectionStatus.ForTrade -> R.string.collection_status_for_trade
            CollectionStatus.WantInTrade -> R.string.collection_status_want_in_trade
            CollectionStatus.WantToBuy -> R.string.collection_status_want_to_buy
            CollectionStatus.WantToPlay -> R.string.collection_status_want_to_play
            CollectionStatus.Wishlist -> R.string.collection_status_wishlist
            CollectionStatus.Played -> R.string.collection_status_played
            CollectionStatus.Rated -> R.string.collection_status_rated
            CollectionStatus.Commented -> R.string.collection_status_commented
            CollectionStatus.HasParts -> R.string.collection_status_has_parts
            CollectionStatus.WantParts -> R.string.collection_status_want_parts
            CollectionStatus.Unknown -> R.string.unknown
        }
    )
    return when (progress.step) {
        SyncViewModel.CollectionSyncProgressStep.CompleteCollection -> {
            if (statusDescription.isBlank()) {
                context.getString(R.string.sync_complete_collection, subtype)
            } else {
                context.getString(R.string.sync_complete_collection_status, statusDescription, subtype)
            }
        }
        SyncViewModel.CollectionSyncProgressStep.PartialCollection -> context.getString(R.string.sync_partial_collection, subtype)
        SyncViewModel.CollectionSyncProgressStep.StaleCollection -> context.getString(R.string.sync_stale_collection, subtype)
        SyncViewModel.CollectionSyncProgressStep.DeleteCollection -> context.getString(R.string.sync_delete_collection, subtype)
        SyncViewModel.CollectionSyncProgressStep.RemoveGames -> context.getString(R.string.sync_remove_games)
        SyncViewModel.CollectionSyncProgressStep.StaleGames -> context.getString(R.string.sync_stale_games)
        SyncViewModel.CollectionSyncProgressStep.NewGames -> context.getString(R.string.sync_new_games)
        else -> null
    }
}

private fun playsStepText(
    context: android.content.Context,
    progress: SyncViewModel.PlaySyncProgress
): String? {
    return when (progress.step) {
        SyncViewModel.PlaySyncProgressStep.NotSyncing -> null
        SyncViewModel.PlaySyncProgressStep.Old -> context.getString(R.string.sync_plays_step_old).appendPage(context, progress.page)
        SyncViewModel.PlaySyncProgressStep.New -> context.getString(R.string.sync_plays_step_new).appendPage(context, progress.page)
        SyncViewModel.PlaySyncProgressStep.Stats -> context.getString(R.string.sync_plays_step_stats)
    }
}

private fun playsRangeText(
    context: android.content.Context,
    progress: SyncViewModel.PlaySyncProgress
): String? {
    return when {
        progress.step == SyncViewModel.PlaySyncProgressStep.NotSyncing -> null
        progress.minDate == 0L && progress.maxDate == 0L -> context.getString(R.string.sync_notification_plays_all)
        progress.minDate == 0L -> context.getString(R.string.sync_notification_plays_old, progress.maxDate.asDate(context))
        progress.maxDate == 0L -> context.getString(R.string.sync_notification_plays_new, progress.minDate.asDate(context))
        else -> context.getString(R.string.sync_notification_plays_between, progress.minDate.asDate(context), progress.maxDate.asDate(context))
    }
}

private fun playsActionText(
    context: android.content.Context,
    progress: SyncViewModel.PlaySyncProgress
): String? {
    return when (progress.action) {
        SyncViewModel.PlaySyncProgressAction.None -> null
        SyncViewModel.PlaySyncProgressAction.Waiting -> context.getString(R.string.sync_plays_action_waiting)
        SyncViewModel.PlaySyncProgressAction.Downloading -> context.getString(R.string.sync_plays_action_downloading)
        SyncViewModel.PlaySyncProgressAction.Saving -> context.getString(R.string.sync_plays_action_saving)
        SyncViewModel.PlaySyncProgressAction.Deleting -> context.getString(R.string.sync_plays_action_deleting)
    }
}

private fun usersStepText(context: android.content.Context, step: SyncViewModel.UserSyncProgressStep): String? {
    return when (step) {
        SyncViewModel.UserSyncProgressStep.NotSyncing -> null
        SyncViewModel.UserSyncProgressStep.BuddyList -> context.getString(R.string.sync_user_step_list)
        SyncViewModel.UserSyncProgressStep.StaleBuddies -> context.getString(R.string.sync_user_step_stale_buddies)
        SyncViewModel.UserSyncProgressStep.NewBuddies -> context.getString(R.string.sync_user_step_unupdated_buddies)
        SyncViewModel.UserSyncProgressStep.StalePlayers -> context.getString(R.string.sync_user_step_stale_players)
        SyncViewModel.UserSyncProgressStep.NewPlayers -> context.getString(R.string.sync_user_step_unupdated_players)
    }
}

private fun String.appendPage(context: android.content.Context, page: Int): String {
    return if (page > 1) context.getString(R.string.sync_notification_page_suffix, this, page) else this
}
