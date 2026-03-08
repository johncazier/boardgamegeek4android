package com.boardgamegeek.ui.logplay

import android.app.Application
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.KEY_LAST_PLAY_LOCATION
import com.boardgamegeek.extensions.KEY_LAST_PLAY_TIME
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getLastPlayPlayers
import com.boardgamegeek.extensions.howManyHoursOld
import com.boardgamegeek.extensions.howManyMinutesOld
import com.boardgamegeek.extensions.isSameDay
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.model.GameExpansion
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.PlayerColorAssigner
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LogPlayViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private var originalPlay: Play? = null
    private val scoreFormat = DecimalFormat("0.#########")
    private var internalIdToDelete: Long = INVALID_ID.toLong()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _internalId = MutableStateFlow(INVALID_ID.toLong())
    val internalId: StateFlow<Long> = _internalId.asStateFlow()

    private val _canFinish = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val canFinish = _canFinish.asSharedFlow()

    private var playId: Int = INVALID_ID

    private val _game = MutableStateFlow<Pair<Int, String>?>(null)

    private val _dateInMillis = MutableStateFlow<Long?>(null)
    val dateInMillis: StateFlow<Long?> = _dateInMillis.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _length = MutableStateFlow(0)
    val length: StateFlow<Int> = _length.asStateFlow()

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    private val _incomplete = MutableStateFlow(false)
    val incomplete: StateFlow<Boolean> = _incomplete.asStateFlow()

    private val _doNotCountWinStats = MutableStateFlow(false)
    val doNotCountWinStats: StateFlow<Boolean> = _doNotCountWinStats.asStateFlow()

    private val _comments = MutableStateFlow("")
    val comments: StateFlow<String> = _comments.asStateFlow()

    private val _players = MutableStateFlow<List<PlayPlayer>>(emptyList())
    val players: StateFlow<List<PlayPlayer>> = _players.asStateFlow()

    private val _loggableExpansions = MutableStateFlow<List<GameExpansion>>(emptyList())
    val loggableExpansions: StateFlow<List<GameExpansion>> = _loggableExpansions.asStateFlow()

    private val _selectedExpansionIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedExpansionIds: StateFlow<Set<Int>> = _selectedExpansionIds.asStateFlow()

    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    private val _customPlayerSort = MutableStateFlow(false)
    val customPlayerSort: StateFlow<Boolean> = _customPlayerSort.asStateFlow()

    private var relatedExpansionPlays = emptyMap<Int, Play>()

    val colors: StateFlow<List<String>> = _game
        .filterNotNull()
        .mapLatest { game -> gameRepository.getPlayColors(game.first) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locations: StateFlow<List<com.boardgamegeek.model.Location>> = flow {
        emit(playRepository.loadLocations())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playersByLocation: StateFlow<List<Player>> = _location
        .mapLatest { location ->
            val allPlayers = playRepository.loadPlayersByLocation(location)
            allPlayers.filter { candidate -> _players.value.none { it.id == candidate.id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shouldCustomSort(sort: Boolean) {
        if (_customPlayerSort.value != sort) {
            _customPlayerSort.value = sort
            if (!sort) pickStartPlayer(0)
        }
    }

    fun loadPlay(
        internalId: Long,
        gameId: Int,
        gameName: String,
        isRequestingToEndPlay: Boolean,
        isRequestingRematch: Boolean,
        isChangingGame: Boolean,
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            _game.value = gameId to gameName
            val game = gameRepository.loadGame(gameId)
            val gameSupportsCustomSort = game?.customPlayerSort != false

            if (internalId == INVALID_ID.toLong()) {
                _internalId.value = INVALID_ID.toLong()
                _dateInMillis.value = today()
                val recentlyPlayed = (prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L).howManyHoursOld() < 1
                val prefsPlayers = prefs.getLastPlayPlayers()

                val initialPlayers = if (recentlyPlayed || prefsPlayers.isEmpty()) prefsPlayers else listOf(prefsPlayers.first())
                val initialPlayersWithSelf = includeCurrentUser(initialPlayers)

                _location.value = prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty()

                val seatedPlayers: List<PlayPlayer> = if (gameSupportsCustomSort) {
                    initialPlayersWithSelf.map { PlayPlayer(name = it.name, username = it.username) }
                } else {
                    assignSeats(initialPlayersWithSelf.map { PlayPlayer(name = it.name, username = it.username) })
                }

                _players.value = seatedPlayers
                originalPlay = buildPlay().copy()
                relatedExpansionPlays = emptyMap()
                _selectedExpansionIds.value = emptySet()
            } else {
                playRepository.loadPlay(internalId)?.let { play ->
                    if (originalPlay == null) originalPlay = play.copy()
                    internalIdToDelete = if (isChangingGame) internalId else INVALID_ID.toLong()
                    _internalId.value = if (isChangingGame || isRequestingRematch) INVALID_ID.toLong() else internalId
                    if (!isChangingGame && !isRequestingRematch) playId = play.playId
                    if (isRequestingRematch) {
                        _dateInMillis.value = today()
                        val rematchPlayers = if (gameSupportsCustomSort) {
                            play.players.map { player ->
                                player.copy(score = "", rating = 0.0, isWin = false, isNew = false, startingPosition = "")
                            }
                        } else {
                            play.sortedPlayers.map { player ->
                                player.copy(score = "", rating = 0.0, isWin = false, isNew = false)
                            }
                        }
                        _players.value = rematchPlayers
                    } else {
                        _dateInMillis.value = play.dateInMillis
                        _quantity.value = play.quantity
                        _incomplete.value = play.incomplete
                        _doNotCountWinStats.value = play.noWinStats
                        _comments.value = play.comments
                        _players.value = play.sortedPlayers
                    }
                    _location.value = play.location
                    val existingExpansionPlayIds = playRepository.loadRelatedExpansionPlays(play).associateBy { it.gameId }
                    if (isRequestingRematch || isChangingGame) {
                        relatedExpansionPlays = emptyMap()
                        _selectedExpansionIds.value = existingExpansionPlayIds.keys
                    } else {
                        relatedExpansionPlays = existingExpansionPlayIds
                        _selectedExpansionIds.value = relatedExpansionPlays.keys
                    }
                    when {
                        isRequestingToEndPlay -> {
                            _length.value = play.length + if (play.startTime > 0) play.startTime.howManyMinutesOld() else 0
                            _startTime.value = 0L
                        }

                        isRequestingRematch -> {
                            _length.value = 0
                            _startTime.value = 0L
                        }

                        else -> {
                            _length.value = play.length
                            _startTime.value = play.startTime
                        }
                    }
                    _customPlayerSort.value = gameSupportsCustomSort || play.arePlayersCustomSorted()
                }
            }

            _loggableExpansions.value = playRepository.loadLoggableExpansions(gameId, _selectedExpansionIds.value)

            _isLoading.value = false
        }
    }

    fun updateDate(dateInMillis: Long) {
        if (_dateInMillis.value != dateInMillis) {
            _dateInMillis.value = dateInMillis
        }
    }

    fun updateLocation(location: String) {
        if (_location.value != location) {
            _location.value = location
        }
    }

    fun updateLength(length: Int) {
        if (_length.value != length) {
            _length.value = length
        }
    }

    fun startTimer() {
        logTimer("Start")
        _startTime.value = System.currentTimeMillis()
        _length.value = 0
    }

    fun resumeTimer() {
        logTimer("Resume")
        val minutes = _length.value
        _startTime.value = System.currentTimeMillis() - minutes * DateUtils.MINUTE_IN_MILLIS
        _length.value = 0
    }

    fun endTimer() {
        logTimer("Off")
        val s = _startTime.value
        _length.value = if (s > 0) s.howManyMinutesOld() else 0
        _startTime.value = 0
    }

    private fun logTimer(state: String) {
        firebaseAnalytics.logEvent("LogPlayTimer") {
            param("State", state)
        }
    }

    fun updateQuantity(quantity: Int?) {
        quantity?.let {
            if (_quantity.value != it) {
                _quantity.value = it
            }
        }
    }

    fun updateIncomplete(isIncomplete: Boolean) {
        if (_incomplete.value != isIncomplete) {
            _incomplete.value = isIncomplete
        }
    }

    fun updateNoWinStats(doNotCountWinStats: Boolean) {
        if (_doNotCountWinStats.value != doNotCountWinStats) {
            _doNotCountWinStats.value = doNotCountWinStats
        }
    }

    fun updateComments(comments: String) {
        if (_comments.value != comments) {
            _comments.value = comments
        }
    }

    fun isDirty(): Boolean {
        return originalPlay?.let {
            !it.dateInMillis.isSameDay(_dateInMillis.value ?: 0L) ||
                it.location != _location.value ||
                it.length != _length.value ||
                it.startTime != _startTime.value ||
                it.quantity != _quantity.value ||
                it.incomplete != _incomplete.value ||
                it.noWinStats != _doNotCountWinStats.value ||
                it.comments != _comments.value ||
                it.players != _players.value ||
                relatedExpansionPlays.keys != _selectedExpansionIds.value
        } ?: false
    }

    fun toggleExpansion(expansionId: Int, isSelected: Boolean) {
        _selectedExpansionIds.value = _selectedExpansionIds.value.toMutableSet().apply {
            if (isSelected) add(expansionId) else remove(expansionId)
        }
    }

    fun addPlayers(players: List<Player>) {
        val newPlayers = _players.value.toMutableList()
        val initialPosition = _players.value.size
        newPlayers += players.mapIndexed { index, player ->
            if (!_customPlayerSort.value) {
                PlayPlayer(name = player.name, username = player.username, startingPosition = (initialPosition + 1 + index).toString())
            } else {
                PlayPlayer(name = player.name, username = player.username)
            }
        }
        if (!_customPlayerSort.value) newPlayers.sortBy { it.seat }
        _players.value = newPlayers
    }

    fun editPlayer(player: PlayPlayer = PlayPlayer(), position: Int? = null) {
        val currentPlayers = _players.value
        val updatedPlayers = currentPlayers.toMutableList()
        if (position in currentPlayers.indices) {
            position?.let { pos -> updatedPlayers[pos] = player }
        } else {
            updatedPlayers += player
        }
        if (!_customPlayerSort.value) updatedPlayers.sortBy { it.seat }
        _players.value = updatedPlayers
    }

    fun addPlayer(player: PlayPlayer = PlayPlayer()) {
        val currentPlayers = _players.value.toMutableList()
        _players.value = if (!_customPlayerSort.value) {
            assignSeats(currentPlayers + player)
        } else {
            currentPlayers + player
        }
    }

    fun removePlayer(player: PlayPlayer) {
        if (_players.value.isEmpty()) return
        val newPlayers = _players.value.toMutableList()
        newPlayers.remove(player)
        _players.value = if (!_customPlayerSort.value) assignSeats(newPlayers) else newPlayers
    }

    fun clearPositions() {
        modifyPlayers { it.copy(startingPosition = "") }
    }

    fun pickStartPlayer(playerIndex: Int) {
        val currentPlayers = _players.value
        if (currentPlayers.isNotEmpty()) {
            _players.value = assignSeats(currentPlayers, playerIndex)
        }
    }

    private val randomDelayMs = 300L

    fun randomizeStartPlayer() {
        val currentPlayers = _players.value
        if (currentPlayers.isEmpty()) return
        viewModelScope.launch {
            when (currentPlayers.size) {
                1 -> _players.value = assignSeats(currentPlayers)
                else -> {
                    val targetIndex = currentPlayers.indices.random() + currentPlayers.size + if (currentPlayers.size < 7) currentPlayers.size else 0
                    for (playerIndex in 0..targetIndex) {
                        _players.value = assignSeats(currentPlayers, playerIndex)
                        delay(randomDelayMs)
                    }
                }
            }
        }
    }

    fun randomizePlayerOrder() {
        val currentPlayers = _players.value
        if (currentPlayers.isEmpty()) return
        viewModelScope.launch {
            when (currentPlayers.size) {
                1 -> _players.value = assignSeats(currentPlayers)
                else -> {
                    for (i in 0..6) {
                        _players.value = assignSeats(currentPlayers.shuffled())
                        delay(randomDelayMs)
                    }
                }
            }
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        val currentPlayers = _players.value
        if (currentPlayers.isEmpty() || _customPlayerSort.value) return
        val newPlayers = currentPlayers.toMutableList()
        newPlayers.find { p -> p.seat == fromSeat }?.let { movingPlayer ->
            newPlayers.remove(movingPlayer)
            newPlayers.add(toSeat - 1, movingPlayer)
            _players.value = assignSeats(newPlayers)
        }
    }

    /**
     * Return the list of players, with seats assigned from 1 to N, starting at the specified index and re-sorted to begin at seat 1.
     */
    private fun assignSeats(players: List<PlayPlayer>, playerIndex: Int = 0): List<PlayPlayer> {
        if (players.isEmpty()) return emptyList()
        return players.mapIndexed { index, player ->
            player.copy(startingPosition = ((index - playerIndex).mod(players.size) + 1).toString())
        }.sortedBy { player -> player.seat }
    }

    fun assignColors(clearExisting: Boolean = false) {
        val currentPlayers = _players.value
        if (currentPlayers.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            val existingPlayers = if (clearExisting) currentPlayers.map { it.copy(color = "") } else currentPlayers
            val results = PlayerColorAssigner(
                _game.value?.first ?: INVALID_ID,
                existingPlayers,
                gameRepository,
                playRepository,
            ).execute()
            val newPlayers = mutableListOf<PlayPlayer>()
            existingPlayers.forEach { ppe ->
                val result = if (ppe.username.isEmpty()) {
                    results.find { it.type == PlayerColorAssigner.PlayerType.NON_USER && it.name == ppe.name }
                } else {
                    results.find { it.type == PlayerColorAssigner.PlayerType.USER && it.name == ppe.username }
                }
                newPlayers += if (result == null) ppe.copy() else ppe.copy(color = result.color)
            }
            _players.value = newPlayers
        }
    }

    fun addColorToPlayer(playerIndex: Int, color: String) {
        modifyPlayerAtIndex(playerIndex) { it.copy(color = color) }
    }

    fun addScoreToPlayer(playerIndex: Int, score: Double) {
        val currentPlayers = _players.value
        if (currentPlayers.isEmpty()) return
        val newPlayers = currentPlayers.mapIndexed { i, player ->
            if (i == playerIndex) player.copy(score = scoreFormat.format(score)) else player.copy()
        }
        val highScore = newPlayers.maxOfOrNull { player -> player.numericScore ?: -Double.MAX_VALUE }
        _players.value = newPlayers.map { ppe ->
            ppe.copy(isWin = (ppe.numericScore == highScore))
        }
    }

    fun addRatingToPlayer(playerIndex: Int, rating: Double) {
        modifyPlayerAtIndex(playerIndex) { it.copy(rating = rating) }
    }

    fun win(isWin: Boolean, playerIndex: Int) {
        modifyPlayerAtIndex(playerIndex) { it.copy(isWin = isWin) }
    }

    fun new(isNew: Boolean, playerIndex: Int) {
        modifyPlayerAtIndex(playerIndex) { it.copy(isNew = isNew) }
    }

    private fun modifyPlayers(indexFunction: (PlayPlayer) -> PlayPlayer) {
        _players.value = _players.value.map { player -> indexFunction(player) }
    }

    private fun modifyPlayerAtIndex(playerIndex: Int, indexFunction: (PlayPlayer) -> PlayPlayer) {
        _players.value = _players.value.mapIndexed { i, player ->
            if (i == playerIndex) indexFunction(player) else player.copy()
        }
    }

    fun logPlay() {
        viewModelScope.launch {
            val play = buildPlay(updateTimestamp = System.currentTimeMillis())
            val newInternalId = playRepository.upsert(play)
            val savedPlay = play.copy(internalId = newInternalId)
            playRepository.logPlay(savedPlay)
            syncExpansionPlays(savedPlay, markForUpload = true)
            if (internalIdToDelete != INVALID_ID.toLong()) {
                if (playRepository.markAsDeleted(internalIdToDelete)) {
                    playRepository.enqueueUploadRequest(internalIdToDelete)
                }
            }
            _internalId.value = newInternalId
            _canFinish.tryEmit(Unit)
        }
    }

    fun saveDraft(wantToFinish: Boolean = true) {
        viewModelScope.launch {
            val play = buildPlay(dirtyTimestamp = System.currentTimeMillis())
            val newInternalId = playRepository.upsert(play)
            syncExpansionPlays(play.copy(internalId = newInternalId), markForUpload = false)
            _internalId.value = newInternalId
            if (wantToFinish) _canFinish.tryEmit(Unit)
        }
    }

    fun deletePlay() {
        viewModelScope.launch {
            val play = buildPlay(deleteTimestamp = System.currentTimeMillis())
            relatedExpansionPlays.values.forEach { expansionPlay ->
                if (playRepository.markAsDeleted(expansionPlay.internalId)) {
                    playRepository.enqueueUploadRequest(expansionPlay.internalId)
                }
            }
            playRepository.enqueueUploadRequest(play.internalId)
            _canFinish.tryEmit(Unit)
        }
    }

    private fun buildPlay(
        dirtyTimestamp: Long = 0L,
        updateTimestamp: Long = 0L,
        deleteTimestamp: Long = 0L,
    ) = Play(
        internalId = _internalId.value,
        playId = playId,
        dateInMillis = _dateInMillis.value ?: today(),
        gameId = _game.value?.first ?: INVALID_ID,
        gameName = _game.value?.second.orEmpty(),
        location = _location.value,
        length = _length.value,
        startTime = _startTime.value,
        quantity = _quantity.value,
        incomplete = _incomplete.value,
        noWinStats = _doNotCountWinStats.value,
        comments = _comments.value,
        _players = _players.value,
        dirtyTimestamp = dirtyTimestamp,
        updateTimestamp = updateTimestamp,
        deleteTimestamp = deleteTimestamp,
    )

    private suspend fun syncExpansionPlays(play: Play, markForUpload: Boolean) {
        val timestamp = when {
            play.updateTimestamp > 0L -> play.updateTimestamp
            play.dirtyTimestamp > 0L -> play.dirtyTimestamp
            else -> System.currentTimeMillis()
        }
        val selectedExpansionIds = _selectedExpansionIds.value
        val refreshedExpansionPlays = mutableMapOf<Int, Play>()

        selectedExpansionIds.forEach { expansionId ->
            val existing = relatedExpansionPlays[expansionId]
            val expansionName = _loggableExpansions.value.find { it.id == expansionId }?.name ?: existing?.gameName.orEmpty()
            val expansionPlay = play.copy(
                internalId = existing?.internalId ?: INVALID_ID.toLong(),
                playId = existing?.playId ?: INVALID_ID,
                gameId = expansionId,
                gameName = expansionName,
                dirtyTimestamp = if (markForUpload) 0L else timestamp,
                updateTimestamp = if (markForUpload) timestamp else 0L,
                deleteTimestamp = 0L,
            )
            val newInternalId = playRepository.upsert(expansionPlay)
            val savedExpansionPlay = expansionPlay.copy(internalId = newInternalId)
            if (markForUpload) {
                playRepository.logPlay(savedExpansionPlay)
            }
            refreshedExpansionPlays[expansionId] = savedExpansionPlay
        }

        relatedExpansionPlays
            .filterKeys { it !in selectedExpansionIds }
            .values
            .forEach { removedPlay ->
                if (playRepository.markAsDeleted(removedPlay.internalId)) {
                    playRepository.enqueueUploadRequest(removedPlay.internalId)
                }
            }

        relatedExpansionPlays = refreshedExpansionPlays
        _loggableExpansions.value = playRepository.loadLoggableExpansions(play.gameId, selectedExpansionIds)
    }

    private fun today(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun includeCurrentUser(players: List<Player>): List<Player> {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""].orEmpty().trim()
        if (username.isBlank()) return players

        val exists = players.any {
            it.username.equals(username, ignoreCase = true) ||
                (it.username.isBlank() && it.name.equals(username, ignoreCase = true))
        }
        if (exists) return players

        val displayName = prefs[AccountPreferences.KEY_FULL_NAME, ""].orEmpty().trim().ifBlank { username }
        return listOf(Player(name = displayName, username = username)) + players
    }
}
