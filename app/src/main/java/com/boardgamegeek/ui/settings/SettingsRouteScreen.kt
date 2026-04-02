package com.boardgamegeek.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.livedata.observeAsState
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.KEY_ADVANCED_DATES
import com.boardgamegeek.extensions.KEY_HAPTIC_FEEDBACK
import com.boardgamegeek.extensions.KEY_SYNC_ERRORS
import com.boardgamegeek.extensions.KEY_SYNC_ONLY_CHARGING
import com.boardgamegeek.extensions.KEY_SYNC_ONLY_WIFI
import com.boardgamegeek.extensions.KEY_SYNC_UPLOADS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.cancelSync
import com.boardgamegeek.extensions.createStatusMap
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import com.boardgamegeek.ui.viewmodel.SettingsViewModel
import com.boardgamegeek.work.SyncCollectionWorker
import com.boardgamegeek.work.SyncPlaysWorker
import com.boardgamegeek.work.SyncUsersWorker
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.pref.clearPlaysTimestamps

@Composable
fun SettingsRouteScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    selfUserViewModel: SelfUserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val prefs = remember(context) { context.preferences() }
    val syncPrefs = remember(context) { SyncPrefs.getPrefs(context) }
    val username by selfUserViewModel.username.observeAsState()

    var statuses by rememberSaveable {
        mutableStateOf(prefs.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, emptySet()).orEmpty())
    }
    var syncPlays by rememberSaveable { mutableStateOf(prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true) }
    var syncBuddies by rememberSaveable { mutableStateOf(prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true) }
    var syncUploads by rememberSaveable { mutableStateOf(prefs[KEY_SYNC_UPLOADS, true] == true) }
    var syncErrors by rememberSaveable { mutableStateOf(prefs[KEY_SYNC_ERRORS, false] == true) }
    var syncOnlyWifi by rememberSaveable { mutableStateOf(prefs[KEY_SYNC_ONLY_WIFI, false] == true) }
    var syncOnlyCharging by rememberSaveable { mutableStateOf(prefs[KEY_SYNC_ONLY_CHARGING, false] == true) }
    var advancedDates by rememberSaveable { mutableStateOf(prefs[KEY_ADVANCED_DATES, false] == true) }
    var hapticFeedback by rememberSaveable { mutableStateOf(prefs[KEY_HAPTIC_FEEDBACK, true] != false) }

    var showStatusDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    val statusMap = remember(context) { context.createStatusMap() }

    AppScreen(
        topBarTitle = stringResource(R.string.title_settings),
        currentScreenRouteFromActivity = "settings",
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsSection(title = stringResource(R.string.pref_account)) {
                SettingValueRow(
                    title = if (username.isNullOrBlank()) {
                        stringResource(R.string.pref_sync_sign_in)
                    } else {
                        stringResource(R.string.pref_sync_sign_out)
                    },
                    value = username.orEmpty().ifBlank { stringResource(R.string.pref_account_summary) },
                    onClick = {
                        if (username.isNullOrBlank()) {
                            navigator.navigate(LoginRoute())
                        } else {
                            confirmAction = ConfirmAction.SignOut
                        }
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.pref_sync)) {
                SettingValueRow(
                    title = stringResource(R.string.pref_sync_statuses),
                    value = statuses.mapNotNull(statusMap::get).joinToString().ifBlank {
                        stringResource(R.string.pref_list_empty)
                    },
                    onClick = { showStatusDialog = true },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_plays),
                    checked = syncPlays,
                    onCheckedChange = {
                        syncPlays = it
                        prefs.edit { putBoolean(PREFERENCES_KEY_SYNC_PLAYS, it) }
                        syncPrefs.clearPlaysTimestamps()
                        SyncPlaysWorker.requestSync(context)
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_buddies),
                    checked = syncBuddies,
                    onCheckedChange = {
                        syncBuddies = it
                        prefs.edit { putBoolean(PREFERENCES_KEY_SYNC_BUDDIES, it) }
                        syncPrefs.clearBuddyListTimestamps()
                        SyncUsersWorker.requestSync(context)
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_upload),
                    checked = syncUploads,
                    onCheckedChange = {
                        syncUploads = it
                        prefs.edit { putBoolean(KEY_SYNC_UPLOADS, it) }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_errors),
                    checked = syncErrors,
                    onCheckedChange = {
                        syncErrors = it
                        prefs.edit { putBoolean(KEY_SYNC_ERRORS, it) }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_only_wifi),
                    checked = syncOnlyWifi,
                    onCheckedChange = {
                        syncOnlyWifi = it
                        prefs.edit { putBoolean(KEY_SYNC_ONLY_WIFI, it) }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_sync_only_charging),
                    checked = syncOnlyCharging,
                    onCheckedChange = {
                        syncOnlyCharging = it
                        prefs.edit { putBoolean(KEY_SYNC_ONLY_CHARGING, it) }
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.pref_data)) {
                SettingValueRow(
                    title = stringResource(R.string.pref_sync_clear),
                    value = stringResource(R.string.pref_sync_clear_info_message),
                    onClick = { confirmAction = ConfirmAction.ClearAllData },
                )
                SettingValueRow(
                    title = stringResource(R.string.pref_sync_reset_collection),
                    value = stringResource(R.string.pref_sync_reset_collection_info_message),
                    onClick = { confirmAction = ConfirmAction.ResetCollection },
                )
                SettingValueRow(
                    title = stringResource(R.string.pref_sync_re_sync_plays),
                    value = stringResource(R.string.pref_sync_re_sync_plays_info_message),
                    onClick = { confirmAction = ConfirmAction.ResetPlays },
                )
                SettingValueRow(
                    title = stringResource(R.string.pref_sync_reset_buddies),
                    value = stringResource(R.string.pref_sync_reset_buddies_info_message),
                    onClick = { confirmAction = ConfirmAction.ResetBuddies },
                )
            }

            SettingsSection(title = stringResource(R.string.pref_advanced)) {
                SettingSwitchRow(
                    title = stringResource(R.string.pref_advanced_forum_dates),
                    checked = advancedDates,
                    onCheckedChange = {
                        advancedDates = it
                        prefs.edit { putBoolean(KEY_ADVANCED_DATES, it) }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.pref_advanced_haptic_feedback),
                    checked = hapticFeedback,
                    onCheckedChange = {
                        hapticFeedback = it
                        prefs.edit { putBoolean(KEY_HAPTIC_FEEDBACK, it) }
                    },
                )
            }
        }
    }

    if (showStatusDialog) {
        val orderedStatuses = remember(statusMap) { statusMap.keys.toList() }
        val labels = remember(statusMap) { orderedStatuses.mapNotNull(statusMap::get) }
        var selection by remember { mutableStateOf(statuses) }
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text(stringResource(R.string.pref_sync_statuses)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    orderedStatuses.forEachIndexed { index, value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selection.contains(value),
                                onCheckedChange = { checked ->
                                    selection = if (checked) selection + value else selection - value
                                },
                            )
                            Text(text = labels[index])
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    statuses = selection
                    prefs.edit { putStringSet(PREFERENCES_KEY_SYNC_STATUSES, selection) }
                    syncPrefs[SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE_CURRENT] =
                        syncPrefs[SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE, 0L] ?: 0L
                    SyncCollectionWorker.requestSync(context)
                    showStatusDialog = false
                }) {
                    Text(stringResource(R.string.set))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.title(context)) },
            text = { Text(action.message(context)) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        ConfirmAction.SignOut -> {
                            context.cancelSync()
                            Authenticator.signOut(context)
                        }
                        ConfirmAction.ClearAllData -> settingsViewModel.clearAllData()
                        ConfirmAction.ResetCollection -> settingsViewModel.resetCollectionItems()
                        ConfirmAction.ResetPlays -> settingsViewModel.resetPlays()
                        ConfirmAction.ResetBuddies -> settingsViewModel.resetUsers()
                    }
                    confirmAction = null
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private enum class ConfirmAction {
    SignOut,
    ClearAllData,
    ResetCollection,
    ResetPlays,
    ResetBuddies,
}

private fun ConfirmAction.title(context: android.content.Context) = when (this) {
    ConfirmAction.SignOut -> context.getString(R.string.pref_sync_sign_out)
    ConfirmAction.ClearAllData -> context.getString(R.string.pref_sync_clear)
    ConfirmAction.ResetCollection -> context.getString(R.string.pref_sync_reset_collection)
    ConfirmAction.ResetPlays -> context.getString(R.string.pref_sync_re_sync_plays)
    ConfirmAction.ResetBuddies -> context.getString(R.string.pref_sync_reset_buddies)
}

private fun ConfirmAction.message(context: android.content.Context) = when (this) {
    ConfirmAction.SignOut -> context.getString(R.string.pref_sync_sign_out_are_you_sure)
    ConfirmAction.ClearAllData -> context.getString(R.string.pref_sync_clear_info_message)
    ConfirmAction.ResetCollection -> context.getString(R.string.pref_sync_reset_collection_info_message)
    ConfirmAction.ResetPlays -> context.getString(R.string.pref_sync_re_sync_plays_info_message)
    ConfirmAction.ResetBuddies -> context.getString(R.string.pref_sync_reset_buddies_info_message)
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = title)
            Spacer(modifier = Modifier.padding(top = 2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
