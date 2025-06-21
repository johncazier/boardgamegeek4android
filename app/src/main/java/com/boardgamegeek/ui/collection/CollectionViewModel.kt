package com.boardgamegeek.ui.collection

import android.app.Application
import android.content.SharedPreferences
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.isStatusSetToSync
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Event wrapper for one-time events (like toasts or navigation)
data class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

@OptIn(ExperimentalCoroutinesApi::class) // For operators like mapLatest, flatMapLatest
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val viewRepository: CollectionViewRepository,
    private val playRepository: PlayRepository,
    private val gameCollectionRepository: GameCollectionRepository,
) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }

    val defaultViewIdFlow = MutableStateFlow(
        prefs.getInt(CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID, CollectionViewPrefs.DEFAULT_DEFAULT_ID)
    )

    private val collectionSorterFactory: CollectionSorterFactory by lazy { CollectionSorterFactory(application) }

    // Inputs from UI (e.g., user selects a sort type or adds a filter)
    val selectedViewId = MutableStateFlow(defaultViewIdFlow.value)
    val manualSortType = MutableStateFlow<Int?>(null) // User explicitly sets sort
    val addedFilters = MutableStateFlow<List<CollectionFilterer>>(emptyList())
    val removedFilterTypes = MutableStateFlow<List<Int>>(emptyList())

    // --- Core Logic Flows ---

    val views: StateFlow<List<CollectionView>> = viewRepository.loadViewsWithoutFiltersFlow()
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, emptyList())

    val selectedView: StateFlow<CollectionView?> = selectedViewId.flatMapLatest { viewId ->
        manualSortType.value = null // Reset manual sort when view changes
        addedFilters.value = emptyList() // Reset added filters
        removedFilterTypes.value = emptyList() // Reset removed filters
        if (viewId == CollectionViewPrefs.DEFAULT_DEFAULT_ID) {
            flowOf(viewRepository.defaultView)
        } else {
            viewRepository.loadViewFlow(viewId)
        }
    }
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, null)

    val selectedViewName: StateFlow<String> = selectedView
        .map { it?.name ?: "" }
        .stateInWhileSubscribed(viewModelScope, "")

    val effectiveSort: StateFlow<Pair<CollectionSorter, Boolean>?> = combine(
        selectedView,
        manualSortType // Directly use the MutableStateFlow here
    ) { view, currentManualSort ->
        val type = currentManualSort ?: view?.sortType ?: CollectionSorterFactory.TYPE_DEFAULT
        collectionSorterFactory.create(type)
    }.stateInWhileSubscribed(viewModelScope, null)

    val effectiveFilters: StateFlow<List<CollectionFilterer>> = combine(
        selectedView,
        addedFilters,       // Directly use the MutableStateFlow
        removedFilterTypes  // Directly use the MutableStateFlow
    ) { view, currentAdded, currentRemovedTypes ->
        val addedTypes = currentAdded.map { it.type }
        val viewFilters = view?.filters.orEmpty()
        viewFilters.filter { !addedTypes.contains(it.type) && !currentRemovedTypes.contains(it.type) } +
                currentAdded.filter { !currentRemovedTypes.contains(it.type) }
    }.stateInWhileSubscribed(viewModelScope, emptyList())

    private val allItemsFlow: StateFlow<List<CollectionItem>> = // Renamed to avoid confusion with `items`
        gameCollectionRepository.loadAllAsFlow()
            .distinctUntilChanged()
            .catch { e ->
                errorMessageEvents.emit(Event(e.localizedMessage.ifEmpty { "Error loading collection" }))
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val isFilteringFlow = MutableStateFlow(true)

    // Throttled items for UI, combines all pieces
    val itemsFlow: StateFlow<List<CollectionItem>> = combine(
        allItemsFlow,
        effectiveFilters,
        effectiveSort,
        selectedViewId // To re-trigger filtering for default view logic
    ) { itemList, filters, sortPair, currentViewId ->
        isFilteringFlow.value = true // Indicate filtering starts
        if (itemList.isEmpty() && filters.isEmpty() && sortPair == null) {
            isFilteringFlow.value = false
            return@combine emptyList()
        }

        var listSequence = itemList.asSequence()
        if (currentViewId == CollectionViewPrefs.DEFAULT_DEFAULT_ID && filters.none { it.type == CollectionFiltererFactory.TYPE_STATUS }) {
            listSequence = filterDefaultView(listSequence)
        }

        filters.forEach { f ->
            listSequence = listSequence.filter { f.filter(it) }
        }

        val sortedList = sortPair?.let {
            it.first.sort(listSequence.toList(), it.second)
        } ?: listSequence.toList()
        isFilteringFlow.value = false // Indicate filtering ends
        sortedList
    }
        .debounce(300)
        .distinctUntilChanged()
        .stateInWhileSubscribed(viewModelScope, emptyList())


    // --- Event Flows (using SharedFlow for one-time events) ---
    // Made these public MutableSharedFlows if you intend to emit from outside,
    // otherwise, keep them private and expose as SharedFlow.
    // For typical ViewModel patterns, private Mutable and public SharedFlow is common.
    val errorMessageEvents = MutableSharedFlow<Event<String>>(replay = 0)

    val toastMessageEvents = MutableSharedFlow<Event<String>>(replay = 0)

    val loggedPlayResultEvents = MutableSharedFlow<Event<PlayUploadResult>>(replay = 0)

    val isRefreshingFlow: StateFlow<Boolean> =
        WorkManager.getInstance(application).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            .asFlow()
            .map { list -> list.any { workInfo -> !workInfo.state.isFinished } }
            .stateInWhileSubscribed(viewModelScope, false)

    fun selectView(viewId: Int) {
        if (selectedViewId.value != viewId) {
            isFilteringFlow.value = true
            viewModelScope.launch { viewRepository.updateShortcuts(viewId) }
            selectedViewId.value = viewId // Directly set the MutableStateFlow
        }
    }

    fun setSort(sortType: Int) {
        val type = when (sortType) {
            CollectionSorterFactory.TYPE_UNKNOWN -> CollectionSorterFactory.TYPE_DEFAULT
            else -> sortType
        }
        if (manualSortType.value != type) {
            isFilteringFlow.value = true
            manualSortType.value = type // Directly set the MutableStateFlow
        }
    }

    fun reverseSort() {
        effectiveSort.value?.let { (sorter, isReversed) ->
            val currentType = sorter.getType(isReversed)
            collectionSorterFactory.reverse(currentType)?.let { reversedSortType ->
                setSort(reversedSortType)
            }
        }
    }

    fun addFilter(filter: CollectionFilterer) {
        viewModelScope.launch(Dispatchers.Default) {
            isFilteringFlow.value = true
            if (filter.isValid) {
                // Update removedFilterTypes
                removedFilterTypes.update { currentRemoved ->
                    currentRemoved.filterNot { it == filter.type }
                }

                // Update addedFilters
                addedFilters.update { currentAdded ->
                    val newList = currentAdded.filterNot { it.type == filter.type }.toMutableList()
                    newList.add(filter)
                    newList
                }

                firebaseAnalytics.logEvent(
                    "Filter",
                    bundleOf(
                        FirebaseAnalytics.Param.CONTENT_TYPE to "Collection",
                        "FilterBy" to filter.type.toString()
                    )
                )
            }
        }
    }

    fun removeFilter(type: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            isFilteringFlow.value = true

            // Update addedFilters
            addedFilters.update { currentAdded ->
                currentAdded.filterNot { it.type == type }
            }

            // Update removedFilterTypes
            removedFilterTypes.update { currentRemoved ->
                if (!currentRemoved.contains(type)) {
                    currentRemoved + type
                } else {
                    currentRemoved
                }
            }
        }
    }

    val acquiredFrom: StateFlow<List<String>> = flow {
        emit(gameCollectionRepository.loadAcquiredFrom())
    }.stateInWhileSubscribed(viewModelScope, emptyList())

    val inventoryLocation: StateFlow<List<String>> = flow {
        emit(gameCollectionRepository.loadInventoryLocation())
    }.stateInWhileSubscribed(viewModelScope, emptyList())


    private fun filterDefaultView(list: Sequence<CollectionItem>) = list.filter {
        (prefs.isStatusSetToSync(CollectionStatus.Own) && it.own) ||
                (prefs.isStatusSetToSync(CollectionStatus.PreviouslyOwned) && it.previouslyOwned) ||
                (prefs.isStatusSetToSync(CollectionStatus.ForTrade) && it.forTrade) ||
                (prefs.isStatusSetToSync(CollectionStatus.WantInTrade) && it.wantInTrade) ||
                (prefs.isStatusSetToSync(CollectionStatus.WantToPlay) && it.wantToPlay) ||
                (prefs.isStatusSetToSync(CollectionStatus.Wishlist) && it.wishList) ||
                (prefs.isStatusSetToSync(CollectionStatus.Preordered) && it.preOrdered) ||
                (prefs.isStatusSetToSync(CollectionStatus.Played) && it.numberOfPlays > 0) ||
                (prefs.isStatusSetToSync(CollectionStatus.Rated) && it.rating > 0.0) ||
                (prefs.isStatusSetToSync(CollectionStatus.Commented) && it.comment.isNotBlank()) ||
                (prefs.isStatusSetToSync(CollectionStatus.HasParts) && it.hasPartsList.isNotBlank()) ||
                (prefs.isStatusSetToSync(CollectionStatus.WantParts) && it.wantPartsList.isNotBlank())
    }

    fun refresh() {
        if (!isRefreshingFlow.value) {
            gameCollectionRepository.enqueueRefreshRequest(WORK_NAME)
        }
    }

    fun insert(name: String, isDefault: Boolean) {
        viewModelScope.launch {
            val view = constructView(0, name, isDefault)
            val viewId = viewRepository.insertView(view)
            logAction("Insert", name)
            toastMessageEvents.emit(Event(application.getString(R.string.msg_collection_view_updated, name)))
            selectView(viewId)
        }
    }

    fun update(name: String, isDefault: Boolean) {
        viewModelScope.launch {
            val currentViewId = selectedViewId.value
            if (currentViewId != BggContract.INVALID_ID) {
                val view = constructView(currentViewId, name, isDefault)
                viewRepository.updateView(view)
                logAction("Update", name)
                toastMessageEvents.emit(Event(application.getString(R.string.msg_collection_view_updated, name)))
            }
        }
    }

    private fun constructView(viewId: Int, name: String, isDefault: Boolean): CollectionView {
        return CollectionView(
            id = viewId,
            name = name,
            sortType = effectiveSort.value?.let { it.first.getType(it.second) } ?: CollectionSorterFactory.TYPE_DEFAULT,
            starred = isDefault,
            filters = effectiveFilters.value,
        )
    }

    fun deleteView(viewIdToDelete: Int, name: String) {
        if (viewIdToDelete <= 0) return
        viewModelScope.launch {
            if (viewRepository.deleteView(viewIdToDelete)) {
                logAction("Delete", name)
                toastMessageEvents.emit(Event(application.getString(R.string.msg_collection_view_deleted, name)))
                if (viewIdToDelete == selectedViewId.value) {
                    selectView(defaultViewIdFlow.value)
                }
            }
        }
    }

    private fun logAction(action: String, name: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
            param("Action", action)
            param("Name", name)
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                errorMessageEvents.emit(Event(result.exceptionOrNull()?.localizedMessage.orEmpty()))
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID) {
                        loggedPlayResultEvents.emit(Event(it))
                    }
                }
            }
        }
    }

    fun createShortcut() {
        viewModelScope.launch {
            val currentViewId = selectedViewId.value
            val currentViewName = selectedViewName.value
            if (currentViewId > 0 && currentViewName.isNotEmpty()) {
                viewRepository.createViewShortcut(
                    application.applicationContext,
                    currentViewId,
                    currentViewName
                )
                //mutableToastMessageEvents.emit(Event(application.getString(R.string.msg_shortcut_created_for_view, currentViewName)))
            } else {
                //mutableToastMessageEvents.emit(Event(application.getString(R.string.msg_shortcut_creation_failed_no_view)))
            }
        }
    }

    companion object {
        private const val WORK_NAME = "CollectionViewModel"
    }
}