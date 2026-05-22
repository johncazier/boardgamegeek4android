package com.boardgamegeek.ui.game

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.ColorInt
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_STATUSES
import com.boardgamegeek.extensions.asWishListPriority
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.getDarkColor
import com.boardgamegeek.extensions.getIconColor
import com.boardgamegeek.extensions.getPlayCountColors
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.stateInWhileSubscribed
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameAgePoll
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.GameExpansion
import com.boardgamegeek.model.GameFamily
import com.boardgamegeek.model.GameLanguagePoll
import com.boardgamegeek.model.GamePlayerPollResults
import com.boardgamegeek.model.GameSubtype
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.ImageRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.PublisherRepository
import com.boardgamegeek.ui.game.GameLauncher
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val gameCollectionRepository: GameCollectionRepository,
    private val imageRepository: ImageRepository,
    private val playRepository: PlayRepository,
    private val designerRepository: DesignerRepository,
    private val artistRepository: ArtistRepository,
    private val publisherRepository: PublisherRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { getApplication<Application>().preferences() }
    private val isGameRefreshing = AtomicBoolean(false)
    private val areItemsRefreshing = AtomicBoolean(false)
    private val areDesignerImagesRefreshing = AtomicBoolean(false)
    private val areArtistImagesRefreshing = AtomicBoolean(false)
    private val arePublisherImagesRefreshing = AtomicBoolean(false)
    private val forceItemsRefresh = AtomicBoolean(false)
    private val arePlaysRefreshing = AtomicBoolean(false)
    private val forcePlaysRefresh = AtomicBoolean(false)
    private val gameRefreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES)
    private val itemsRefreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)
    private val playsFullMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS)
    private val playsPartialMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES)

    private val _gameId = MutableStateFlow<Int?>(null)
    val gameId: StateFlow<Int?> = _gameId.asStateFlow()

    private val _gameIsRefreshing = MutableStateFlow(false)
    val gameIsRefreshing: StateFlow<Boolean> = _gameIsRefreshing.asStateFlow()

    private val _itemsAreRefreshing = MutableStateFlow(false)
    val itemsAreRefreshing: StateFlow<Boolean> = _itemsAreRefreshing.asStateFlow()

    private val _playsAreRefreshing = MutableStateFlow(false)
    val playsAreRefreshing: StateFlow<Boolean> = _playsAreRefreshing.asStateFlow()

    private val _producerType = MutableStateFlow(ProducerType.UNKNOWN)
    val producerType: StateFlow<ProducerType> = _producerType.asStateFlow()

    private val _errorMessage = MutableStateFlow<Event<String>?>(null)
    val errorMessage: StateFlow<Event<String>?> = _errorMessage.asStateFlow()

    private val _loggedPlayResult = MutableStateFlow<Event<PlayUploadResult>?>(null)
    val loggedPlayResult: StateFlow<Event<PlayUploadResult>?> = _loggedPlayResult.asStateFlow()

    val username: StateFlow<String?> = preferenceFlow<String>(AccountPreferences.KEY_USERNAME)
        .stateInWhileSubscribed(viewModelScope, prefs.all[AccountPreferences.KEY_USERNAME] as? String)

    val syncPlaysPreference: StateFlow<Boolean?> = preferenceFlow<Boolean>(PREFERENCES_KEY_SYNC_PLAYS)
        .stateInWhileSubscribed(viewModelScope, prefs.all[PREFERENCES_KEY_SYNC_PLAYS] as? Boolean)

    @Suppress("UNCHECKED_CAST")
    val syncCollectionPreference: StateFlow<Set<String>?> =
        preferenceFlow<Set<String>>(PREFERENCES_KEY_SYNC_STATUSES)
            .stateInWhileSubscribed(
                viewModelScope,
                prefs.all[PREFERENCES_KEY_SYNC_STATUSES] as? Set<String>
            )

    enum class ProducerType {
        UNKNOWN,
        DESIGNER,
        ARTIST,
        PUBLISHER,
        CATEGORY,
        MECHANIC,
        EXPANSION,
        BASE_GAME,
    }

    fun setId(gameId: Int) {
        if (_gameId.value != gameId) {
            viewModelScope.launch {
                gameRepository.updateLastViewed(gameId)
            }
            _gameId.value = gameId
        }
    }

    fun setProducerType(type: ProducerType) {
        if (_producerType.value != type) _producerType.value = type
    }

    val game: StateFlow<Game?> =
        gameId
            .filterNotNull()
            .flatMapLatest { id ->
                gameRepository.loadGameFlow(id)
                    .onEach { loadedGame ->
                        if (loadedGame == null || loadedGame.updated.isOlderThan(gameRefreshMinutes.minutes)) {
                            refreshGame()
                        }
                    }
                    .catch { e ->
                        Timber.w(e)
                        emitError(e)
                        emit(null)
                    }
            }
            .distinctUntilChanged()
            .stateInWhileSubscribed(viewModelScope, null)

    val subtypes: StateFlow<List<GameSubtype>> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let { gameRepository.getSubtypesFlow(it.id) } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val families: StateFlow<List<GameFamily>> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let { gameRepository.getFamiliesFlow(it.id) } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val languagePoll: StateFlow<GameLanguagePoll?> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let { gameRepository.getLanguagePollFlow(it.id) } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(null)
            }
            .stateInWhileSubscribed(viewModelScope, null)

    val agePoll: StateFlow<GameAgePoll?> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let { gameRepository.getAgePollFlow(it.id) } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(null)
            }
            .stateInWhileSubscribed(viewModelScope, null)

    val playerPoll: StateFlow<List<GamePlayerPollResults>> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let { gameRepository.getPlayerPollFlow(it.id) } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val designers: StateFlow<List<GameDetail>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getDesignersFlow(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val artists: StateFlow<List<GameDetail>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getArtistsFlow(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val publishers: StateFlow<List<GameDetail>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getPublishers(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val categories: StateFlow<List<GameDetail>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getCategoriesFlow(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val mechanics: StateFlow<List<GameDetail>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getMechanicsFlow(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val expansions: StateFlow<List<GameDetail>> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let {
                gameRepository.getExpansionsFlow(it.id)
                    .map { list ->
                        list.map { expansion ->
                            GameDetail(expansion.id, expansion.name, describeStatuses(expansion), expansion.thumbnailUrl)
                        }.distinctBy { it.id }
                    }
                    .flowOn(Dispatchers.Default)
            } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val baseGames: StateFlow<List<GameDetail>> =
        game.flatMapLatest { loadedGame ->
            loadedGame?.let {
                gameRepository.getBaseGamesFlow(it.id)
                    .map { list ->
                        list.map { baseGame ->
                            GameDetail(baseGame.id, baseGame.name, describeStatuses(baseGame), baseGame.thumbnailUrl)
                        }.distinctBy { it.id }
                    }
                    .flowOn(Dispatchers.Default)
            } ?: emptyFlow()
        }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    fun refreshDesignerImages(limit: Int = 10) {
        gameId.value?.let {
            if (areDesignerImagesRefreshing.compareAndSet(false, true)) {
                viewModelScope.launch {
                    designerRepository.refreshMissingImagesForGame(it, limit = limit)
                    areDesignerImagesRefreshing.set(false)
                }
            }
        }
    }

    fun refreshArtistImages(limit: Int = 10) {
        gameId.value?.let {
            if (areArtistImagesRefreshing.compareAndSet(false, true)) {
                viewModelScope.launch {
                    artistRepository.refreshMissingImagesForGame(it, limit = limit)
                    areArtistImagesRefreshing.set(false)
                }
            }
        }
    }

    fun refreshPublisherImages(limit: Int = 10) {
        gameId.value?.let {
            if (arePublisherImagesRefreshing.compareAndSet(false, true)) {
                viewModelScope.launch {
                    publisherRepository.refreshMissingThumbnailsForGame(it, limit = limit)
                    arePublisherImagesRefreshing.set(false)
                }
            }
        }
    }

    private fun describeStatuses(expansion: GameExpansion): String {
        val ctx = getApplication<BggApplication>()
        val statuses = mutableListOf<String>()
        if (expansion.own) statuses.add(ctx.getString(R.string.collection_status_own))
        if (expansion.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
        if (expansion.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
        if (expansion.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
        if (expansion.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
        if (expansion.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
        if (expansion.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
        if (expansion.wishList) statuses.add(expansion.wishListPriority.asWishListPriority(ctx))
        if (expansion.numberOfPlays > 0) statuses.add(ctx.getString(R.string.played))
        if (expansion.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
        if (expansion.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
        return statuses.formatList()
    }

    val producers: StateFlow<List<GameDetail>> =
        producerType.flatMapLatest { type ->
            when (type) {
                ProducerType.DESIGNER -> designers
                ProducerType.ARTIST -> artists
                ProducerType.PUBLISHER -> publishers
                ProducerType.CATEGORY -> categories
                ProducerType.MECHANIC -> mechanics
                ProducerType.EXPANSION -> expansions
                ProducerType.BASE_GAME -> baseGames
                else -> MutableStateFlow(emptyList())
            }
        }.stateInWhileSubscribed(viewModelScope, emptyList())

    val collectionItems: StateFlow<List<CollectionItem>> =
        gameId.filterNotNull()
            .flatMapLatest { id ->
                gameCollectionRepository.loadCollectionItemsForGameFlow(id)
            }
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val plays: StateFlow<List<Play>> =
        game
            .onEach { attemptRefreshPlays() }
            .flatMapLatest { loadedGame ->
                loadedGame?.let { playRepository.loadPlaysByGameFlow(it.id) } ?: emptyFlow()
            }
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    val playColors: StateFlow<List<String>> =
        gameId.filterNotNull()
            .flatMapLatest { id -> gameRepository.getPlayColorsFlow(id) }
            .distinctUntilChanged()
            .catch {
                Timber.w(it)
                emitError(it)
                emit(emptyList())
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    fun refreshGame() {
        gameId.value?.let {
            if (isGameRefreshing.compareAndSet(false, true)) {
                _gameIsRefreshing.value = true
                viewModelScope.launch {
                    val result = gameRepository.refreshGame(it)
                    if (result.isFailure) {
                        result.exceptionOrNull()?.let { exception -> emitError(exception) }
                    } else {
                        gameRepository.loadGame(it)?.let { newGame ->
                            if (newGame.doesHeroImageNeedUpdating()) {
                                gameRepository.refreshHeroImage(newGame)
                            }
                        }
                    }
                    _gameIsRefreshing.value = false
                    isGameRefreshing.set(false)
                }
            }
        }
    }

    fun refreshItems() {
        forceItemsRefresh.set(true)
        attemptRefreshItems()
    }

    private fun attemptRefreshItems(list: List<CollectionItem>? = collectionItems.value) {
        game.value?.let { loadedGame ->
            if (areItemsRefreshing.compareAndSet(false, true)) {
                _itemsAreRefreshing.value = true
                viewModelScope.launch {
                    if (list?.any { it.isDirty } == true) {
                        gameCollectionRepository.enqueueUploadRequest(loadedGame.id)
                    } else if (
                        list?.isEmpty() == true ||
                        (list != null && list.minOf { item -> item.syncTimestamp }.isOlderThan(itemsRefreshMinutes.minutes)) ||
                        forceItemsRefresh.compareAndSet(true, false)
                    ) {
                        Timber.d("Refreshing items for game $loadedGame")
                        gameCollectionRepository.refreshCollectionItems(loadedGame.id, loadedGame.subtype)?.let { emitError(it) }
                    } else {
                        Timber.d("NOT refreshing items for game $loadedGame")
                    }
                    _itemsAreRefreshing.value = false
                    areItemsRefreshing.set(false)
                }
            }
        }
    }

    fun refreshPlays() {
        forcePlaysRefresh.set(true)
        attemptRefreshPlays()
    }

    private fun attemptRefreshPlays() {
        game.value?.let { loadedGame ->
            if (arePlaysRefreshing.compareAndSet(false, true)) {
                _playsAreRefreshing.value = true
                viewModelScope.launch {
                    val lastUpdated = loadedGame.updatedPlays
                    when {
                        lastUpdated.isOlderThan(playsFullMinutes.minutes) -> {
                            playRepository.refreshPlaysForGame(loadedGame.id)?.let { emitError(it) }
                        }
                        lastUpdated.isOlderThan(playsPartialMinutes.minutes) -> {
                            playRepository.refreshPlaysForGame(loadedGame.id, 1)?.let { emitError(it) }
                        }
                        forcePlaysRefresh.compareAndSet(true, false) -> {
                            playRepository.refreshPlaysForGame(loadedGame.id, 1)?.let { emitError(it) }
                        }
                    }
                    _playsAreRefreshing.value = false
                    arePlaysRefreshing.set(false)
                }
            }
        }
    }

    fun reload() {
        refreshGame()
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { p ->
            game.value?.let { loadedGame ->
                viewModelScope.launch {
                    @ColorInt
                    val iconColor = p.getIconColor()
                    @ColorInt
                    val darkColor = p.getDarkColor()
                    val (winsColor, winnablePlaysColor, allPlaysColor) = p.getPlayCountColors(getApplication())
                    val modified = loadedGame.iconColor != iconColor ||
                        loadedGame.darkColor != darkColor ||
                        loadedGame.winsColor != winsColor ||
                        loadedGame.winnablePlaysColor != winnablePlaysColor ||
                        loadedGame.allPlaysColor != allPlaysColor
                    if (modified) {
                        gameRepository.updateGameColors(
                            gameId.value ?: BggContract.INVALID_ID,
                            iconColor,
                            darkColor,
                            winsColor,
                            winnablePlaysColor,
                            allPlaysColor,
                        )
                    }
                }
            }
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure) {
                result.exceptionOrNull()?.let { exception -> emitError(exception) }
            } else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID) {
                        _loggedPlayResult.value = Event(it)
                    }
                }
            }
        }
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int) {
        viewModelScope.launch {
            gameCollectionRepository.addCollectionItem(
                gameId.value ?: BggContract.INVALID_ID,
                statuses,
                wishListPriority
            )
        }
    }

    fun createShortcut() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<BggApplication>().applicationContext
            val gameId = gameId.value ?: BggContract.INVALID_ID
            val gameName = game.value?.name.orEmpty()
            val thumbnailUrl = game.value?.thumbnailUrl.orEmpty()
            val bitmap = imageRepository.fetchThumbnail(thumbnailUrl.ensureHttpsScheme())
            GameLauncher.createShortcutInfo(context, gameId, gameName, bitmap)?.let { info ->
                ShortcutManagerCompat.requestPinShortcut(context, info, null)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> preferenceFlow(preferenceKey: String): Flow<T?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, changedKey ->
            if (changedKey == preferenceKey) {
                trySend(sharedPreferences.all[preferenceKey] as? T)
            }
        }
        trySend(prefs.all[preferenceKey] as? T)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun emitError(message: String) {
        _errorMessage.update { Event(message) }
    }

    private fun emitError(exception: Throwable) {
        emitError(exception.localizedMessage ?: exception.message ?: exception.toString())
    }
}
