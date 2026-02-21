package com.boardgamegeek.ui.collectiondetails

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.getSyncStatuses
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.PlayStats
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.model.CollectionItem.Companion.filterBaseGames
import com.boardgamegeek.model.CollectionItem.Companion.filterOwned
import com.boardgamegeek.model.CollectionItem.Companion.filterPlayed
import com.boardgamegeek.model.CollectionItem.Companion.filterPublishedGames
import com.boardgamegeek.model.CollectionItem.Companion.filterRated
import com.boardgamegeek.model.CollectionItem.Companion.filterUncommented
import com.boardgamegeek.model.CollectionItem.Companion.filterUnplayed
import com.boardgamegeek.model.CollectionItem.Companion.filterUnrated
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.StatsHelper.Companion.calculateCorrelationCoefficient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class CollectionDetailsViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {

    data class CollectionAcquireStats(
        val incomingCount: Int,
        val futureGrowthRate: Double,
    )

    data class CollectionAnalyzeStats(
        val averagePersonalRating: Double,
        val averageAverageRating: Double,
        val correlationCoefficient: Double,
    )

    enum class PlayerCountType {
        All,
        Supports,
        GoodWith,
        BestWith,
    }

    private val prefs: SharedPreferences by lazy { getApplication<Application>().preferences() }

    private val _errorMessageFlow = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessageFlow.asStateFlow()

    private val _loggedPlayResultFlow = MutableStateFlow<PlayUploadResult?>(null)
    val loggedPlayResultFlow: StateFlow<PlayUploadResult?> = _loggedPlayResultFlow.asStateFlow()

    val syncCollectionStatuses: StateFlow<Set<CollectionStatus>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == PREFERENCES_KEY_SYNC_STATUSES) {
                trySend(sharedPrefs.getSyncStatuses())
            }
        }
        trySend(prefs.getSyncStatuses())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.stateInWhileSubscribed(viewModelScope, emptySet())

    val isRefreshingFlow: StateFlow<Boolean> =
        WorkManager.getInstance(getApplication<Application>()).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            .asFlow()
            .map { list -> list.any { workInfo -> !workInfo.state.isFinished } }
            .stateInWhileSubscribed(viewModelScope, false)

    private val allItems: StateFlow<List<CollectionItem>> =
        gameCollectionRepository.loadAllAsFlow()
            .catch { e ->
                _errorMessageFlow.value = e.localizedMessage.ifEmpty { "Error loading collection" }
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    private val playStats: StateFlow<PlayStats?> =
        flow<PlayStats?> {
            emit(playRepository.calculatePlayStats())
        }.catch { e ->
            _errorMessageFlow.value = e.localizedMessage.ifEmpty { "Error loading play stats" }
            emit(null)
        }.stateInWhileSubscribed(viewModelScope, null)

    private val allGames: StateFlow<List<CollectionItem>> = allItems
        .map { list ->
            list
                .groupingBy { it.gameId }
                .fold<CollectionItem, Int, CollectionItem?>(null) { acc, item ->
                    acc?.copy(
                        own = acc.own || item.own,
                        previouslyOwned = acc.previouslyOwned || item.previouslyOwned,
                        preOrdered = acc.preOrdered || item.preOrdered,
                        forTrade = acc.forTrade || item.forTrade,
                        wantToPlay = acc.wantToPlay || item.wantToPlay,
                        wantToBuy = acc.wantToBuy || item.wantToBuy,
                        wantInTrade = acc.wantInTrade || item.wantInTrade,
                        wishList = acc.wishList || item.wishList,
                        wishListPriority = minOf(acc.wishListPriority, item.wishListPriority),
                        rating = maxOf(acc.rating, item.rating),
                    ) ?: item
                }
                .values
                .filterNotNull()
                .toList()
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    private val baseItems: StateFlow<List<CollectionItem>> = allItems
        .map { list ->
            list.filter { item -> item.subtype in listOf(Game.Subtype.BoardGame, null) }
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    private val _playerCount = MutableStateFlow<Int?>(null)
    private val _playerCountType = MutableStateFlow(PlayerCountType.All)
    val playerCountType: StateFlow<PlayerCountType> = _playerCountType.asStateFlow()

    private val itemsFilteredByPlayerCount: StateFlow<List<CollectionItem>> = combine(
        allItems,
        _playerCount,
        _playerCountType,
    ) { items, playerCount, playerCountType ->
        items.filterByPlayerCount(playerCount, playerCountType)
    }.stateInWhileSubscribed(viewModelScope, emptyList())

    // BROWSE

    val friendless: StateFlow<Int> = playStats
        .map { it?.friendless ?: 0 }
        .stateInWhileSubscribed(viewModelScope, 0)

    val recentlyViewedItems: StateFlow<List<CollectionItem>> = allItems
        .map {
            it.filter { item -> item.lastViewedDate > 0L }
                .sortedByDescending { item -> item.lastViewedDate }
                .take(ITEM_LIMIT)
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val friendlessFavoriteItems: StateFlow<List<CollectionItem>> = allItems
        .map { list -> list.sortedByDescending { it.friendlessFave }.take(ITEM_LIMIT) }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val underratedItems: StateFlow<List<CollectionItem>> = allItems
        .map {
            it.filter { item -> item.rating > 0.0 }
                .sortedByDescending { item -> item.zScore }
                .take(ITEM_LIMIT)
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    // OWN

    val utilization: StateFlow<Double> = playStats
        .map { it?.utilization ?: 0.0 }
        .stateInWhileSubscribed(viewModelScope, 0.0)

    val own: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.own && (item.subtype in listOf(Game.Subtype.BoardGame, Game.Subtype.Unknown, null)) }
            filter
                .sortedWith(compareByDescending<CollectionItem> { item -> item.rating }.thenBy { item -> item.geekRating })
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val growthRate: StateFlow<Int> = allItems
        .map { items ->
            val itemsWithAcquisitionDate = items.filter { it.acquisitionDate > 0L }
            if (itemsWithAcquisitionDate.isEmpty()) return@map 0

            val startCalendar = Calendar.getInstance().apply {
                timeInMillis = itemsWithAcquisitionDate.minOfOrNull { it.acquisitionDate } ?: 0L
            }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearsAcquiring = (currentYear - startCalendar.get(Calendar.YEAR) + 1).coerceAtLeast(1)
            itemsWithAcquisitionDate.sumOf { it.quantity } / yearsAcquiring
        }
        .stateInWhileSubscribed(viewModelScope, 0)

    val expansions: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.own && item.subtype == Game.Subtype.BoardGameExpansion }
            filter
                .sortedWith(compareByDescending<CollectionItem> { item -> item.rating }.thenByDescending { item -> item.geekRating })
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val accessories: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.own && item.subtype == Game.Subtype.BoardGameAccessory }
            filter
                .sortedWith(compareByDescending<CollectionItem> { item -> item.rating }.thenByDescending { item -> item.geekRating })
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val recentlyAcquired: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map { list ->
            val filter = list.filter { it.own && !it.acquisitionDate.isOlderThan(365.days) }
            filter
                .sortedByDescending { it.acquisitionDate }
                .take(ITEM_LIMIT) to filter.sumOf { it.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    @Suppress("SpellCheckingInspection")
    val hawtItems: StateFlow<List<CollectionItem>> = allItems
        .map { list -> list.filter { it.own }.sortedByDescending { it.hawt }.take(ITEM_LIMIT) }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    // ACQUIRE

    val collectionAcquireStats: StateFlow<CollectionAcquireStats?> = allItems
        .map { items ->
            val incomingCount = items.filter { it.isIncoming }.sumOf { it.quantity }
            val ownCount = items.filter { it.own }.sumOf { it.quantity }
            val forTradeCount = items.filter { it.forTrade }.sumOf { it.quantity }
            CollectionAcquireStats(
                incomingCount = incomingCount,
                futureGrowthRate = if (ownCount > 0) (incomingCount - forTradeCount).toDouble() / ownCount else 0.0,
            )
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val preordered: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val (withDate, withoutDate) = it.filter { item -> item.preOrdered }.partition { item -> item.acquisitionDate > 0L }
            (withoutDate.sortedByDescending { item -> item.geekRating } + withDate.sortedBy { item -> item.acquisitionDate })
                .take(ITEM_LIMIT) to (withoutDate.sumOf { item -> item.quantity } + withDate.sumOf { item -> item.quantity })
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val wishlist: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map { list ->
            val filter = list.filter { item -> item.wishList && item.wishListPriority in 1..4 }
            filter
                .sortedWith(compareBy<CollectionItem> { item -> item.wishListPriority }.thenByDescending { item -> item.geekRating })
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val wantToBuy: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.wantToBuy }
            filter
                .sortedByDescending { item -> item.geekRating }
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val wantInTrade: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.wantInTrade }
            filter
                .sortedByDescending { item -> item.geekRating }
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val favoriteUnownedItems: StateFlow<List<CollectionItem>> = allGames
        .map {
            it.filter { item -> !item.own && item.rating > 0.0 && !item.isIncoming }
                .sortedByDescending { item -> item.rating }
                .take(ITEM_LIMIT)
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val playedButUnownedItems: StateFlow<List<CollectionItem>> = allGames
        .map { list ->
            list.filter { item -> !item.own && item.numberOfPlays > 0 && !item.isIncoming }
                .filterPublishedGames()
                .sortedByDescending { item -> item.numberOfPlays }
                .take(ITEM_LIMIT)
        }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    @Suppress("SpellCheckingInspection")
    val hawtUnownedItems: StateFlow<List<CollectionItem>> = allGames
        .map { list -> list.filter { item -> !item.own && !item.isIncoming }.sortedByDescending { item -> item.hawt }.take(ITEM_LIMIT) }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    // DIVEST

    private val forTradeItems: StateFlow<List<CollectionItem>> = allItems
        .map { list -> list.filter { item -> item.forTrade } }
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val forTrade: StateFlow<Pair<List<CollectionItem>, Int>?> = forTradeItems
        .map {
            it.sortedByDescending { item -> item.numberOfUsersWanting }
                .take(ITEM_LIMIT) to it.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val forTradeWithoutCondition: StateFlow<Pair<List<CollectionItem>, Int>?> = forTradeItems
        .map {
            val filter = it.sortedByDescending { item -> item.numberOfUsersWanting }.drop(ITEM_LIMIT).filter { item -> item.conditionText.isBlank() }
            val quantity = it.filter { item -> item.conditionText.isBlank() }.sumOf { item -> item.quantity }
            filter.sortedByDescending { item -> item.numberOfUsersWanting }.take(ITEM_LIMIT) to quantity
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val regretFactor: StateFlow<Int> = allItems
        .map { list -> list.filter { item -> item.previouslyOwned && item.isIncoming }.sumOf { item -> item.quantity } }
        .stateInWhileSubscribed(viewModelScope, 0)

    val previouslyOwned: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map {
            val filter = it.filter { item -> item.previouslyOwned }
            filter.sortedByDescending { item -> item.geekRating }
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val whyOwnItems: StateFlow<Pair<List<CollectionItem>, Int>?> = allItems
        .map { list ->
            val filter = list
                .filter { item -> item.own && !item.forTrade && item.friendlessWhyOwn() > 100.0 }
                .filterPublishedGames()
            filter.sortedByDescending { item -> item.friendlessWhyOwn() }
                .take(ITEM_LIMIT) to filter.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    // PLAY

    val wantToPlayItems: StateFlow<Pair<List<CollectionItem>, Int>?> = itemsFilteredByPlayerCount
        .map {
            val list = it.filter { item -> item.wantToPlay }
            list.sortedByDescending { item -> item.geekRating }.take(ITEM_LIMIT) to list.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val recentlyPlayedGames: StateFlow<Pair<List<CollectionItem>, Int>?> = itemsFilteredByPlayerCount
        .map {
            val list = it.asSequence()
                .filterBaseGames()
                .filter { item ->
                    item.lastPlayDate != null &&
                        item.lastPlayDate > LocalDateTime.now().atZone(ZoneId.systemDefault()).minusDays(30).toInstant().toEpochMilli()
                }
                .sortedByDescending { item -> item.lastPlayDate }
                .toList()
            list.take(ITEM_LIMIT) to list.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val friendlessShouldPlayGames: StateFlow<Pair<List<CollectionItem>, Int>?> = itemsFilteredByPlayerCount
        .map {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filter { item -> item.rating >= 8 && item.friendlessShouldPlay > 1270 }
                .toList()
            list.sortedByDescending { item -> item.friendlessShouldPlay }
                .take(ITEM_LIMIT) to list.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val shelfOfOpportunityItems: StateFlow<Pair<List<CollectionItem>, Int>?> = itemsFilteredByPlayerCount
        .map {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filterUnplayed()
                .toList()
            list.sortedByDescending { item -> item.geekRating }
                .take(ITEM_LIMIT) to list.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val shelfOfNewOpportunityItems: StateFlow<Pair<List<CollectionItem>, Int>?> = itemsFilteredByPlayerCount
        .map {
            val list = it.asSequence()
                .filterOwned()
                .filterBaseGames()
                .filterUnplayed()
                .filter { item -> item.acquisitionDate > 0L }
                .toList()
            list.sortedByDescending { item -> item.acquisitionDate }
                .take(ITEM_LIMIT) to list.sumOf { item -> item.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    // ANALYZE

    val collectionAnalyzeStats: StateFlow<CollectionAnalyzeStats?> = allItems
        .map { items ->
            CollectionAnalyzeStats(
                averagePersonalRating = items.filter { it.rating > 0.0 }.map { it.rating }.average(),
                averageAverageRating = items.filter { it.averageRating > 0.0 }.map { it.averageRating }.average(),
                correlationCoefficient = calculateCorrelationCoefficient(items.filter { it.rating > 0 && it.averageRating > 0 }.map { it.rating to it.averageRating }),
            )
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val ratableItems: StateFlow<Pair<List<CollectionItem>, Int>?> = baseItems
        .map { list ->
            val filter = list
                .filterUnrated()
                .filterPlayed()
                .filterPublishedGames()
            filter.sortedWith(compareBy({ -it.numberOfPlays }, { it.numberOfUsersRating }))
                .take(ITEM_LIMIT) to filter.sumOf { it.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    val commentableItems: StateFlow<Pair<List<CollectionItem>, Int>?> = baseItems
        .map { list ->
            val filter = list
                .filterUncommented()
                .filterRated()
                .filterPlayed()
                .filterPublishedGames()
            filter.sortedBy { it.numberOfUsersRating }
                .take(ITEM_LIMIT) to filter.sumOf { it.quantity }
        }
        .stateInWhileSubscribed(viewModelScope, null)

    // Related data

    val acquiredFrom: StateFlow<List<String>> = flow {
        emit(gameCollectionRepository.loadAcquiredFrom())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
    )

    // Actions

    fun refresh() {
        if (!isRefreshingFlow.value) {
            gameCollectionRepository.enqueueRefreshRequest(WORK_NAME)
        }
    }

    fun clearErrorMessage() {
        _errorMessageFlow.value = null
    }

    fun clearLoggedPlayResult() {
        _loggedPlayResultFlow.value = null
    }

    fun filterPlayerCount(playerCount: Int?) {
        _playerCount.value = playerCount
    }

    fun filterPlayerCountType(playerCountType: PlayerCountType) {
        _playerCountType.value = playerCountType
    }

    fun addStatus(internalId: Long, status: CollectionStatus) {
        viewModelScope.launch {
            allItems.value.find { it.internalId == internalId }?.let { originalItem ->
                val statuses = originalItem.statuses.copy().apply {
                    first.add(status)
                }
                gameCollectionRepository.updateStatus(internalId, statuses.first, statuses.second)
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun removeStatus(internalId: Long, status: CollectionStatus) {
        viewModelScope.launch {
            allItems.value.find { it.internalId == internalId }?.let { originalItem ->
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(status)
                }
                gameCollectionRepository.updateStatus(internalId, statuses.first, statuses.second)
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun markAsTraded(internalId: Long) {
        viewModelScope.launch {
            allItems.value.find { it.internalId == internalId }?.let { originalItem ->
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(CollectionStatus.ForTrade)
                    first.add(CollectionStatus.PreviouslyOwned)
                }
                gameCollectionRepository.updateStatus(internalId, statuses.first, statuses.second)
                gameCollectionRepository.updateCondition(internalId, "")
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun updateRating(internalId: Long, rating: Double) {
        viewModelScope.launch {
            val gameId = gameCollectionRepository.loadCollectionItem(internalId)?.gameId
            if (gameId != null && gameId != INVALID_ID) {
                gameCollectionRepository.updateRating(internalId, rating)
                gameCollectionRepository.enqueueUploadRequest(gameId)
            }
        }
    }

    fun updateComment(internalId: Long, comment: String) {
        viewModelScope.launch {
            val gameId = gameCollectionRepository.loadCollectionItem(internalId)?.gameId
            if (gameId != null && gameId != INVALID_ID) {
                gameCollectionRepository.updateComment(internalId, comment)
                gameCollectionRepository.enqueueUploadRequest(gameId)
            }
        }
    }

    fun updateCondition(internalId: Long, text: String) {
        viewModelScope.launch {
            allItems.value.find { it.internalId == internalId }?.let { originalItem ->
                gameCollectionRepository.updateCondition(internalId, text)
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                _errorMessageFlow.value = result.exceptionOrNull()?.localizedMessage.orEmpty()
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != INVALID_ID) {
                        _loggedPlayResultFlow.value = it
                    }
                }
            }
        }
    }

    fun markedAsAcquired(
        internalId: Long,
        priceCurrency: String?,
        pricePaid: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
    ) {
        allItems.value.find { it.internalId == internalId }?.let { originalItem ->
            viewModelScope.launch {
                val statuses = originalItem.statuses.copy().apply {
                    first.remove(CollectionStatus.WantInTrade)
                    first.remove(CollectionStatus.WantToBuy)
                    first.remove(CollectionStatus.Wishlist)
                    first.remove(CollectionStatus.Preordered)
                    first.add(CollectionStatus.Own)
                }
                gameCollectionRepository.updateStatus(internalId, statuses.first, statuses.second)

                val itemModified = priceCurrency != originalItem.pricePaidCurrency ||
                    pricePaid != originalItem.pricePaid ||
                    quantity != originalItem.quantity ||
                    acquisitionDate != originalItem.acquisitionDate ||
                    acquiredFrom != originalItem.acquiredFrom

                if (itemModified) {
                    gameCollectionRepository.updatePrivateInfo(
                        internalId,
                        priceCurrency,
                        pricePaid,
                        originalItem.currentValueCurrency,
                        originalItem.currentValue,
                        quantity,
                        acquisitionDate,
                        acquiredFrom,
                        originalItem.inventoryLocation,
                    )
                }
                gameCollectionRepository.enqueueUploadRequest(originalItem.gameId)
            }
        }
    }

    private fun List<CollectionItem>.filterByPlayerCount(playerCount: Int?, playerCountType: PlayerCountType): List<CollectionItem> {
        return playerCount?.let { count ->
            when (playerCountType) {
                PlayerCountType.All -> this
                PlayerCountType.Supports -> filter { item -> count in item.minPlayerCount..item.maxPlayerCount }
                PlayerCountType.GoodWith -> filter { item -> item.recommendedPlayerCounts?.contains(count) == true }
                PlayerCountType.BestWith -> filter { item -> item.bestPlayerCounts?.contains(count) == true }
            }
        } ?: this
    }

    companion object {
        const val ITEM_LIMIT = 30
        const val UNPUBLISHED_PROTOTYPE_ID = 18291
        const val WORK_NAME = "CollectionViewModel"
    }
}
