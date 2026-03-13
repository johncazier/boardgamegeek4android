package com.boardgamegeek.ui.sync

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.addSyncStatus
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getSyncStatusesOrDefault
import com.boardgamegeek.extensions.mapToEnum
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.removeSyncStatus
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getCompleteCollectionTimestampKey
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.work.CollectionUploadWorker
import com.boardgamegeek.work.SyncCollectionWorker
import com.boardgamegeek.work.SyncPlaysWorker
import com.boardgamegeek.work.SyncUsersWorker
import com.boardgamegeek.work.PlayUploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@HiltViewModel
class SyncViewModel @Inject constructor(
    application: Application,
    private val collectionRepository: GameCollectionRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }

    val syncCollectionStatuses: StateFlow<Set<CollectionStatus>> = preferenceFlow<Set<String>>(PREFERENCES_KEY_SYNC_STATUSES)
        .map { set ->
            set?.map { it.mapToEnum() }?.toSet() ?: prefs.getSyncStatusesOrDefault()
        }
        .stateInWhileSubscribed(viewModelScope, prefs.getSyncStatusesOrDefault())

    val collectionCompleteTimestamp: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val collectionPartialTimestamp: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_COLLECTION_PARTIAL, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val collectionCompleteCurrentTimestamp: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE_CURRENT, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val syncPlays: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(PREFERENCES_KEY_SYNC_PLAYS)
            .stateInWhileSubscribed(viewModelScope, prefs[PREFERENCES_KEY_SYNC_PLAYS, false])

    private val oldestSyncDate: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    private val newestSyncDate: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    val syncBuddies: StateFlow<Boolean?> =
        preferenceFlow<Boolean>(PREFERENCES_KEY_SYNC_BUDDIES)
            .stateInWhileSubscribed(viewModelScope, prefs[PREFERENCES_KEY_SYNC_BUDDIES, false])

    val buddySyncDate: StateFlow<Long?> =
        preferenceFlow(SyncPrefs.TIMESTAMP_BUDDIES, SyncPrefs.NAME)
            .stateInWhileSubscribed(viewModelScope, null)

    private val collectionWorkInfos = WorkManager.getInstance(getApplication())
        .getWorkInfosForUniqueWorkLiveData(SyncCollectionWorker.UNIQUE_WORK_NAME_AD_HOC)
        .asFlow()

    private val playWorkInfos = WorkManager.getInstance(getApplication())
        .getWorkInfosForUniqueWorkLiveData(SyncPlaysWorker.UNIQUE_WORK_NAME_AD_HOC)
        .asFlow()

    private val userWorkInfos = WorkManager.getInstance(getApplication())
        .getWorkInfosForUniqueWorkLiveData(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)
        .asFlow()

    val numberOfCollectionItemsToUpload: StateFlow<Int> =
        collectionRepository.loadItemsPendingUploadAsFlow()
            .distinctUntilChanged()
            .map { it.size }
            .stateInWhileSubscribed(viewModelScope, 0)

    val numberOfUnsyncedGames: StateFlow<Int> =
        gameRepository.loadAllAsFlow()
            .distinctUntilChanged()
            .map { list -> list.filter { it?.updated == 0L }.size }
            .stateInWhileSubscribed(viewModelScope, 0)

    private val allPlays: Flow<List<Play>> =
        playRepository.loadAllPlaysFlow().distinctUntilChanged()

    private val numberOfSyncPlays: Flow<Int> =
        allPlays.map { list -> list.count { it.isSynced } }

    val playSyncState: StateFlow<Triple<Long, Long, Int>> =
        combine(numberOfSyncPlays, oldestSyncDate, newestSyncDate) { count, oldest, newest ->
            Triple(oldest ?: Long.MAX_VALUE, newest ?: 0L, count)
        }.stateInWhileSubscribed(viewModelScope, Triple(Long.MAX_VALUE, 0L, 0))

    val numberOfPlaysToBeUpdated: StateFlow<Int> =
        allPlays.map { list -> list.count { it.updateTimestamp > 0L } }
            .stateInWhileSubscribed(viewModelScope, 0)

    val numberOfPlaysToBeDeleted: StateFlow<Int> =
        allPlays.map { list -> list.count { it.deleteTimestamp > 0L } }
            .stateInWhileSubscribed(viewModelScope, 0)

    private val users: Flow<List<User>> =
        userRepository.loadUsersFlow().distinctUntilChanged()

    val userSyncState: StateFlow<UserSyncState> =
        users.map { list ->
            val updatedOrNotLists = list.partition { it.updatedTimestamp == 0L }
            val numberOfUnupdatedUsers = updatedOrNotLists.first.size
            val oldestSyncedUser = updatedOrNotLists.second.minByOrNull { it.updatedTimestamp }
            UserSyncState(
                updatedOrNotLists.second.size,
                numberOfUnupdatedUsers,
                oldestSyncedUser?.updatedTimestamp,
            )
        }.stateInWhileSubscribed(viewModelScope, UserSyncState(0, 0, null))

    val collectionSyncProgress: StateFlow<CollectionSyncProgress> = collectionWorkInfos
        .map { list ->
            val workInfo = list.firstOrNull()
            if (workInfo?.state == WorkInfo.State.RUNNING) {
                val step = when (workInfo.progress.getInt(SyncCollectionWorker.PROGRESS_KEY_STEP, SyncCollectionWorker.PROGRESS_STEP_UNKNOWN)) {
                    SyncCollectionWorker.PROGRESS_STEP_COLLECTION_COMPLETE -> CollectionSyncProgressStep.CompleteCollection
                    SyncCollectionWorker.PROGRESS_STEP_COLLECTION_PARTIAL -> CollectionSyncProgressStep.PartialCollection
                    SyncCollectionWorker.PROGRESS_STEP_COLLECTION_STALE -> CollectionSyncProgressStep.StaleCollection
                    SyncCollectionWorker.PROGRESS_STEP_COLLECTION_DELETE -> CollectionSyncProgressStep.DeleteCollection
                    SyncCollectionWorker.PROGRESS_STEP_GAMES_REMOVE -> CollectionSyncProgressStep.RemoveGames
                    SyncCollectionWorker.PROGRESS_STEP_GAMES_STALE -> CollectionSyncProgressStep.StaleGames
                    SyncCollectionWorker.PROGRESS_STEP_GAMES_NEW -> CollectionSyncProgressStep.NewGames
                    else -> CollectionSyncProgressStep.NotSyncing
                }
                val subtype = when (workInfo.progress.getInt(SyncCollectionWorker.PROGRESS_KEY_SUBTYPE, SyncCollectionWorker.PROGRESS_SUBTYPE_NONE)) {
                    SyncCollectionWorker.PROGRESS_SUBTYPE_ALL -> CollectionSyncProgressSubtype.All
                    SyncCollectionWorker.PROGRESS_SUBTYPE_ACCESSORY -> CollectionSyncProgressSubtype.Accessory
                    else -> CollectionSyncProgressSubtype.None
                }
                val status = workInfo.progress.getString(SyncCollectionWorker.PROGRESS_KEY_STATUS)
                CollectionSyncProgress(step, subtype, status.mapToEnum())
            } else {
                CollectionSyncProgress()
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, CollectionSyncProgress())

    val playSyncProgress: StateFlow<PlaySyncProgress> = playWorkInfos
        .map { list ->
            val workInfo = list.firstOrNull()
            if (workInfo?.state == WorkInfo.State.RUNNING) {
                val step = when (workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_STEP, SyncPlaysWorker.PROGRESS_STEP_UNKNOWN)) {
                    SyncPlaysWorker.PROGRESS_STEP_NEW -> PlaySyncProgressStep.New
                    SyncPlaysWorker.PROGRESS_STEP_OLD -> PlaySyncProgressStep.Old
                    SyncPlaysWorker.PROGRESS_STEP_STATS -> PlaySyncProgressStep.Stats
                    else -> PlaySyncProgressStep.NotSyncing
                }
                val minDate = workInfo.progress.getLong(SyncPlaysWorker.PROGRESS_MIN_DATE, 0L)
                val maxDate = workInfo.progress.getLong(SyncPlaysWorker.PROGRESS_MAX_DATE, 0L)
                val page = workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_PAGE, 1)
                val action = when (workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_ACTION, SyncPlaysWorker.PROGRESS_ACTION_UNKNOWN)) {
                    SyncPlaysWorker.PROGRESS_ACTION_WAITING -> PlaySyncProgressAction.Waiting
                    SyncPlaysWorker.PROGRESS_ACTION_DOWNLOADING -> PlaySyncProgressAction.Downloading
                    SyncPlaysWorker.PROGRESS_ACTION_SAVING -> PlaySyncProgressAction.Saving
                    SyncPlaysWorker.PROGRESS_ACTION_DELETING -> PlaySyncProgressAction.Deleting
                    else -> PlaySyncProgressAction.None
                }
                PlaySyncProgress(step, minDate, maxDate, page, action)
            } else {
                PlaySyncProgress(PlaySyncProgressStep.NotSyncing)
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, PlaySyncProgress(PlaySyncProgressStep.NotSyncing))

    val userProgress: StateFlow<UserSyncProgress> = userWorkInfos
        .map { list ->
            val workInfo = list.firstOrNull()
            if (workInfo?.state == WorkInfo.State.RUNNING) {
                val progress = workInfo.progress
                val stepEnum = when (progress.getInt(SyncUsersWorker.PROGRESS_STEP, SyncUsersWorker.PROGRESS_STEP_UNKNOWN)) {
                    SyncUsersWorker.PROGRESS_STEP_BUDDY_LIST -> UserSyncProgressStep.BuddyList
                    SyncUsersWorker.PROGRESS_STEP_STALE_BUDDIES -> UserSyncProgressStep.StaleBuddies
                    SyncUsersWorker.PROGRESS_STEP_NEW_BUDDIES -> UserSyncProgressStep.NewBuddies
                    SyncUsersWorker.PROGRESS_STEP_STALE_PLAYERS -> UserSyncProgressStep.StalePlayers
                    SyncUsersWorker.PROGRESS_STEP_NEW_PLAYERS -> UserSyncProgressStep.NewPlayers
                    else -> UserSyncProgressStep.NotSyncing
                }
                UserSyncProgress(
                    stepEnum,
                    progress.getString(SyncUsersWorker.PROGRESS_USERNAME),
                    progress.getInt(SyncUsersWorker.PROGRESS_INDEX, 0),
                    progress.getInt(SyncUsersWorker.PROGRESS_TOTAL, 0),
                )
            } else {
                UserSyncProgress(UserSyncProgressStep.NotSyncing)
            }
        }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, UserSyncProgress(UserSyncProgressStep.NotSyncing))

    fun collectionStatusCompleteTimestamp(status: CollectionStatus): Flow<Long?> {
        return if (status == CollectionStatus.Unknown) {
            flowOf(null)
        } else {
            preferenceFlow(getCompleteCollectionTimestampKey(null, status), SyncPrefs.NAME)
        }
    }

    fun collectionStatusAccessoryCompleteTimestamp(status: CollectionStatus): Flow<Long?> {
        return preferenceFlow(getCompleteCollectionTimestampKey(Game.Subtype.BoardGameAccessory, status), SyncPrefs.NAME)
    }

    fun syncCollection(status: CollectionStatus = CollectionStatus.Unknown) {
        SyncCollectionWorker.requestSync(getApplication(), status)
    }

    fun cancelCollection() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncCollectionWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    fun uploadCollection() {
        CollectionUploadWorker.buildRequest(getApplication())
    }

    fun modifyCollectionStatus(status: CollectionStatus, add: Boolean) {
        if (add) prefs.addSyncStatus(status)
        else prefs.removeSyncStatus(status)
    }

    fun setSyncPlaysEnabled(enabled: Boolean) {
        prefs[PREFERENCES_KEY_SYNC_PLAYS] = enabled
    }

    fun syncPlays() {
        SyncPlaysWorker.requestSync(getApplication())
    }

    fun cancelPlays() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncPlaysWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    fun uploadPlays() {
        PlayUploadWorker.requestSync(getApplication())
    }

    fun setSyncBuddiesEnabled(enabled: Boolean) {
        prefs[PREFERENCES_KEY_SYNC_BUDDIES] = enabled
    }

    fun syncBuddies() {
        SyncUsersWorker.requestSync(getApplication())
    }

    fun cancelBuddies() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    data class CollectionSyncProgress(
        val step: CollectionSyncProgressStep = CollectionSyncProgressStep.NotSyncing,
        val subtype: CollectionSyncProgressSubtype = CollectionSyncProgressSubtype.None,
        val status: CollectionStatus = CollectionStatus.Unknown,
    )

    enum class CollectionSyncProgressStep {
        NotSyncing,
        CompleteCollection,
        PartialCollection,
        StaleCollection,
        DeleteCollection,
        RemoveGames,
        NewGames,
        StaleGames,
    }

    enum class CollectionSyncProgressSubtype {
        None,
        All,
        Accessory,
    }

    data class PlaySyncProgress(
        val step: PlaySyncProgressStep,
        val minDate: Long = 0L,
        val maxDate: Long = 0L,
        val page: Int = 1,
        val action: PlaySyncProgressAction = PlaySyncProgressAction.None,
    )

    enum class PlaySyncProgressStep {
        NotSyncing,
        New,
        Old,
        Stats,
    }

    enum class PlaySyncProgressAction {
        None,
        Waiting,
        Downloading,
        Saving,
        Deleting,
    }

    data class UserSyncState(val count: Int, val numberOfUnupdatedUsers: Int, val oldestUpdatedUserTimestamp: Long?)

    data class UserSyncProgress(val step: UserSyncProgressStep, val username: String? = null, val progress: Int = 0, val max: Int = 0)

    enum class UserSyncProgressStep {
        NotSyncing,
        BuddyList,
        StaleBuddies,
        NewBuddies,
        StalePlayers,
        NewPlayers,
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> preferenceFlow(preferenceKey: String, prefsName: String? = null): Flow<T?> = callbackFlow {
        val preferences = getApplication<Application>().preferences(prefsName)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
            if (changedKey == preferenceKey) {
                trySend(sharedPreferences.all[preferenceKey] as? T)
            }
        }
        trySend(preferences.all[preferenceKey] as? T)
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
