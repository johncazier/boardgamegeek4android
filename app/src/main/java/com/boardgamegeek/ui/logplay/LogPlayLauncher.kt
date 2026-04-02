package com.boardgamegeek.ui.logplay

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogColorsBinding
import com.boardgamegeek.databinding.DialogNumberPadBinding
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.KEY_HAPTIC_FEEDBACK
import com.boardgamegeek.extensions.LOG_EDIT_PLAYER
import com.boardgamegeek.extensions.LOG_EDIT_PLAYER_PROMPTED
import com.boardgamegeek.extensions.TAG_PLAY_TIMER
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asScore
import com.boardgamegeek.extensions.cancelNotification
import com.boardgamegeek.extensions.childrenRecursiveSequence
import com.boardgamegeek.extensions.clearText
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.isToday
import com.boardgamegeek.extensions.launchPlayingNotification
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.extensions.showLogPlayComments
import com.boardgamegeek.extensions.showLogPlayIncomplete
import com.boardgamegeek.extensions.showLogPlayLength
import com.boardgamegeek.extensions.showLogPlayLocation
import com.boardgamegeek.extensions.showLogPlayNoWinStats
import com.boardgamegeek.extensions.showLogPlayPlayerList
import com.boardgamegeek.extensions.showLogPlayQuantity
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.extensions.fromLocalToUtc
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.logplayer.LogPlayerLauncher
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LocalRouteResultCoordinator
import com.boardgamegeek.ui.navigation.LogPlayRoute
import com.boardgamegeek.ui.navigation.LogPlayerRouteResult
import com.boardgamegeek.ui.navigation.findActivity
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Calendar
import java.util.UUID

object LogPlayLauncher {
    fun logPlay(
        context: Context,
        gameId: Int,
        gameName: String,
        heroImageUrl: String = "",
        customPlayerSort: Boolean = false,
    ) {
        context.startActivity(createIntent(context, INVALID_ID.toLong(), gameId, gameName, heroImageUrl, customPlayerSort))
    }

    fun editPlay(
        context: Context,
        internalId: Long,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
    ) {
        context.startActivity(createIntent(context, internalId, gameId, gameName, heroImageUrl, false))
    }

    fun endPlay(context: Context, internalId: Long, gameId: Int, gameName: String, heroImageUrl: String) {
        context.startActivity(
            createRouteIntent(
                context = context,
                route = LogPlayRoute(
                    internalId = internalId,
                    gameId = gameId,
                    gameName = gameName,
                    heroImageUrl = heroImageUrl,
                    isRequestingToEndPlay = true,
                ),
            ),
        )
    }

    fun rematch(
        context: Context,
        internalId: Long,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
        customPlayerSort: Boolean,
    ) {
        context.startActivity(createRematchIntent(context, internalId, gameId, gameName, heroImageUrl, customPlayerSort))
    }

    fun changeGame(
        context: Context,
        internalId: Long,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
    ) {
        context.startActivity(
            createRouteIntent(
                context = context,
                route = LogPlayRoute(
                    internalId = internalId,
                    gameId = gameId,
                    gameName = gameName,
                    heroImageUrl = heroImageUrl,
                    isChangingGame = true,
                ),
            ),
        )
    }

    fun createRematchIntent(
        context: Context,
        internalId: Long,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
        customPlayerSort: Boolean,
    ): Intent {
        return createRouteIntent(
            context = context,
            route = LogPlayRoute(
                internalId = internalId,
                gameId = gameId,
                gameName = gameName,
                heroImageUrl = heroImageUrl,
                customPlayerSort = customPlayerSort,
                isRequestingRematch = true,
            ),
        )
    }

    private fun createIntent(
        context: Context,
        internalId: Long,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
        customPlayerSort: Boolean,
    ): Intent {
        return createRouteIntent(
            context = context,
            route = LogPlayRoute(
                internalId = internalId,
                gameId = gameId,
                gameName = gameName,
                heroImageUrl = heroImageUrl,
                customPlayerSort = customPlayerSort,
            ),
        )
    }

    private fun createRouteIntent(
        context: Context,
        route: LogPlayRoute,
    ): Intent {
        return MainActivity.createIntent(
            context = context,
            route = route,
        )
    }
}

@Composable
fun LogPlayRouteScreen(
    route: LogPlayRoute,
    viewModel: LogPlayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val navigator = LocalAppNavigator.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val routeResults = LocalRouteResultCoordinator.current
    val firebaseAnalytics: FirebaseAnalytics = remember(context) { Firebase.analytics }
    val routeKey = remember(route) {
        listOf(
            route.internalId,
            route.gameId,
            route.gameName,
            route.heroImageUrl,
            route.customPlayerSort,
            route.isRequestingToEndPlay,
            route.isRequestingRematch,
            route.isChangingGame,
        ).joinToString("|")
    }

    var isUserShowingLocation by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingLength by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingQuantity by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingIncomplete by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingNoWinStats by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingComments by rememberSaveable(routeKey) { mutableStateOf(false) }
    var isUserShowingPlayers by rememberSaveable(routeKey) { mutableStateOf(false) }
    var shouldSaveOnPause by rememberSaveable(routeKey) { mutableStateOf(true) }
    var pendingPlayerRequestId by rememberSaveable(routeKey) { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val internalId by viewModel.internalId.collectAsStateWithLifecycle()
    val dateInMillis by viewModel.dateInMillis.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val length by viewModel.length.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()
    val incomplete by viewModel.incomplete.collectAsStateWithLifecycle()
    val noWinStats by viewModel.doNotCountWinStats.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val expansions by viewModel.loggableExpansions.collectAsStateWithLifecycle()
    val selectedExpansionIds by viewModel.selectedExpansionIds.collectAsStateWithLifecycle()
    val startTime by viewModel.startTime.collectAsStateWithLifecycle()
    val shouldCustomSortPlayers by viewModel.customPlayerSort.collectAsStateWithLifecycle()
    val gameColors by viewModel.colors.collectAsStateWithLifecycle()
    val availablePlayers by viewModel.playersByLocation.collectAsStateWithLifecycle()
    val fabColor = remember(context) { ContextCompat.getColor(context, R.color.accent) }
    val playerDescriptions = remember(players) {
        players.mapIndexed { index, player ->
            player.description.ifEmpty { context.getString(R.string.generic_player, index + 1) }
        }
    }
    val usedColors = remember(players) { players.map { it.color } }
    val shouldDeletePlayOnCancel = route.internalId == INVALID_ID.toLong() || route.isRequestingRematch || route.isChangingGame

    LaunchedEffect(route) {
        if (route.gameId <= 0) {
            val message = "Can't log a play without a game ID."
            Timber.w(message)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            navigator.popBackStackOrFinish(context)
            return@LaunchedEffect
        }

        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", if (route.internalId == INVALID_ID.toLong()) "Action" else "Edit")
            param("GameName", route.gameName)
        }

        viewModel.loadPlay(
            internalId = route.internalId,
            gameId = route.gameId,
            gameName = route.gameName,
            isRequestingToEndPlay = route.isRequestingToEndPlay,
            isRequestingRematch = route.isRequestingRematch,
            isChangingGame = route.isChangingGame,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.canFinish.collect {
            navigator.popBackStackOrFinish(context)
        }
    }

    LaunchedEffect(internalId, startTime, location, players.size, route.heroImageUrl, route.gameName) {
        if (internalId != INVALID_ID.toLong()) {
            if (startTime > 0L) {
                context.launchPlayingNotification(
                    internalId = internalId,
                    gameName = route.gameName,
                    location = location,
                    playerCount = players.size,
                    startTime = startTime,
                    route.heroImageUrl,
                )
            } else {
                context.cancelNotification(TAG_PLAY_TIMER, internalId)
            }
        }
    }

    LaunchedEffect(pendingPlayerRequestId) {
        val requestId = pendingPlayerRequestId ?: return@LaunchedEffect
        val result = routeResults.observe(requestId).filterNotNull().first() as? LogPlayerRouteResult ?: return@LaunchedEffect
        when {
            result.position == LogPlayerLauncher.INVALID_POSITION && result.player != null -> {
                viewModel.addPlayer(result.player)
                firebaseAnalytics.logEvent("DataManipulation") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayPlayer")
                    param("Action", "Add")
                    param("GameName", route.gameName)
                }
            }

            result.position in players.indices && result.player != null -> {
                viewModel.editPlayer(result.player, result.position)
            }

            result.position != LogPlayerLauncher.INVALID_POSITION && result.player == null -> {
                Timber.d("Edit player canceled at position %s", result.position)
            }
        }
        routeResults.clear(requestId)
        pendingPlayerRequestId = null
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, shouldSaveOnPause, comments) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && shouldSaveOnPause) {
                viewModel.updateComments(comments)
                viewModel.saveDraft(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun saveOrLog() {
        shouldSaveOnPause = false
        viewModel.updateComments(comments)
        if (startTime > 0L) {
            context.toast(R.string.msg_saving_draft)
            viewModel.saveDraft()
        } else {
            context.toast(R.string.msg_logging_play)
            viewModel.logPlay()
            context.cancelNotification(TAG_PLAY_TIMER, internalId)
        }
    }

    fun systemBack() {
        if (viewModel.isDirty()) {
            shouldSaveOnPause = false
            context.toast(R.string.msg_saving_draft)
            viewModel.saveDraft(true)
        } else {
            shouldSaveOnPause = false
            navigator.popBackStackOrFinish(context)
        }
    }

    fun cancel() {
        shouldSaveOnPause = false
        if (viewModel.isDirty()) {
            val currentActivity = activity
            if (currentActivity == null) {
                navigator.popBackStackOrFinish(context)
                return
            }
            if (shouldDeletePlayOnCancel) {
                currentActivity.createDiscardDialog(R.string.play, isNew = true, finishActivity = false) {
                    viewModel.deletePlay()
                    context.cancelNotification(TAG_PLAY_TIMER, internalId)
                }.show()
            } else {
                currentActivity.createDiscardDialog(R.string.play, isNew = false, finishActivity = false) {
                    navigator.popBackStackOrFinish(context)
                }.show()
            }
        } else {
            if (shouldDeletePlayOnCancel) {
                viewModel.deletePlay()
                context.cancelNotification(TAG_PLAY_TIMER, internalId)
            } else {
                navigator.popBackStackOrFinish(context)
            }
        }
    }

    fun showDatePicker() {
        val currentActivity = activity ?: return
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            currentActivity,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthOfYear)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.updateDate(selectedDate.fromLocalToUtc())
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun createLaunchInput(autoPosition: Int): LogPlayerLauncher.LaunchInput {
        return LogPlayerLauncher.LaunchInput(
            gameId = route.gameId,
            gameName = route.gameName,
            heroImageUrl = route.heroImageUrl,
            isRequestingToEndPlay = route.isRequestingToEndPlay,
            usedColors = usedColors,
            autoPosition = if (!shouldCustomSortPlayers) autoPosition else LogPlayerLauncher.INVALID_POSITION,
        )
    }

    fun launchPlayerRoute(playerRoute: com.boardgamegeek.ui.navigation.LogPlayerRoute) {
        pendingPlayerRequestId = playerRoute.requestId
        navigator.navigate(playerRoute)
    }

    fun addNewPlayer(autoPosition: Int = players.size + 1) {
        launchPlayerRoute(
            LogPlayerLauncher.addPlayerRoute(
                requestId = UUID.randomUUID().toString(),
                input = createLaunchInput(autoPosition),
            ),
        )
    }

    fun editPlayer(position: Int) {
        val player = players.getOrNull(position)
        if (player == null) {
            Timber.w("Attempting to edit a null player at position %s", position)
            return
        }
        launchPlayerRoute(
            LogPlayerLauncher.editPlayerRoute(
                requestId = UUID.randomUUID().toString(),
                input = createLaunchInput(player.seat),
                position = position,
                player = player,
            ),
        )
    }

    fun addPlayers(editPlayer: Boolean) {
        if (editPlayer) {
            if (!showPlayersToAddDialog(
                    context = context,
                    availablePlayers = availablePlayers,
                    onPlayersAdded = { viewModel.addPlayers(it) },
                    onAddMore = { addNewPlayer() },
                )
            ) {
                addNewPlayer()
            }
        } else {
            viewModel.addPlayer()
        }
    }

    fun promptToEditPlayers() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pref_edit_player_prompt_title)
            .setMessage(R.string.pref_edit_player_prompt_message)
            .setCancelable(true)
            .setPositiveButton(R.string.pref_edit_player_prompt_positive) { _, _ ->
                context.preferences()[LOG_EDIT_PLAYER] = true
                addPlayers(true)
            }
            .setNegativeButton(R.string.pref_edit_player_prompt_negative) { _, _ ->
                context.preferences()[LOG_EDIT_PLAYER] = false
                addPlayers(false)
            }
            .create()
            .show()
        context.preferences()[LOG_EDIT_PLAYER_PROMPTED] = true
    }

    fun addField(field: AddLogPlayField) {
        when (field) {
            AddLogPlayField.Location -> {
                isUserShowingLocation = true
            }

            AddLogPlayField.Length -> {
                isUserShowingLength = true
            }

            AddLogPlayField.Quantity -> {
                isUserShowingQuantity = true
                viewModel.updateQuantity(1)
            }

            AddLogPlayField.Incomplete -> {
                isUserShowingIncomplete = true
                viewModel.updateIncomplete(true)
            }

            AddLogPlayField.NoWinStats -> {
                isUserShowingNoWinStats = true
                viewModel.updateNoWinStats(true)
            }

            AddLogPlayField.Comments -> {
                isUserShowingComments = true
            }

            AddLogPlayField.Players -> {
                isUserShowingPlayers = true
                if (context.preferences()[LOG_EDIT_PLAYER_PROMPTED, false] == true) {
                    addPlayers(context.preferences()[LOG_EDIT_PLAYER, false] ?: false)
                } else {
                    promptToEditPlayers()
                }
            }
        }
        firebaseAnalytics.logEvent("AddField") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param(FirebaseAnalytics.Param.ITEM_NAME, addFieldName(context, field))
        }
    }

    fun assignColors() {
        if (usedColors.isNotEmpty()) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.title_clear_colors)
                .setMessage(R.string.msg_clear_colors)
                .setCancelable(true)
                .setNegativeButton(R.string.keep) { _: DialogInterface?, _: Int -> viewModel.assignColors() }
                .setPositiveButton(R.string.clear) { _: DialogInterface?, _: Int -> viewModel.assignColors(true) }
                .show()
        } else {
            viewModel.assignColors()
        }
    }

    fun showPlayerSortMenu() {
        val currentActivity = activity ?: return
        PopupMenu(currentActivity, currentActivity.findViewById(android.R.id.content)).apply {
            inflate(
                if (!shouldCustomSortPlayers && players.size > 1) {
                    R.menu.log_play_player_sort
                } else {
                    R.menu.log_play_player_sort_short
                },
            )
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_custom_player_order -> {
                        if (shouldCustomSortPlayers) {
                            logPlayerOrder(firebaseAnalytics, "NotCustom")
                            if (players.any { it.startingPosition.isNotBlank() }) {
                                MaterialAlertDialogBuilder(context)
                                    .setMessage(R.string.are_you_sure_player_sort_custom_off)
                                    .setCancelable(true)
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.sort) { _, _ -> viewModel.shouldCustomSort(false) }
                                    .show()
                            } else {
                                viewModel.shouldCustomSort(false)
                            }
                        } else {
                            logPlayerOrder(firebaseAnalytics, "Custom")
                            if (players.any { it.startingPosition.isNotBlank() }) {
                                MaterialAlertDialogBuilder(context)
                                    .setMessage(R.string.message_custom_player_order)
                                    .setPositiveButton(R.string.keep) { _, _ -> viewModel.shouldCustomSort(true) }
                                    .setNegativeButton(R.string.clear) { _, _ ->
                                        viewModel.shouldCustomSort(true)
                                        viewModel.clearPositions()
                                    }
                                    .setCancelable(true)
                                    .show()
                            } else {
                                viewModel.shouldCustomSort(true)
                            }
                        }
                        true
                    }

                    R.id.menu_pick_start_player -> {
                        logPlayerOrder(firebaseAnalytics, "Prompt")
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.title_pick_start_player)
                            .setItems(playerDescriptions.toTypedArray()) { _, which ->
                                viewModel.pickStartPlayer(which)
                            }
                            .show()
                        true
                    }

                    R.id.menu_random_start_player -> {
                        logPlayerOrder(firebaseAnalytics, "RandomStarter")
                        viewModel.randomizeStartPlayer()
                        true
                    }

                    R.id.menu_random_player_order -> {
                        logPlayerOrder(firebaseAnalytics, "Random")
                        viewModel.randomizePlayerOrder()
                        true
                    }

                    else -> false
                }
            }
            show()
        }
    }

    fun showScoreDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            showNumberPadDialog(
                context = context,
                titleResId = R.string.score,
                initialValue = player.score,
                colorDescription = player.color,
                subtitle = player.fullDescription(context),
            ) { score ->
                viewModel.addScoreToPlayer(position, score)
            }
        }
    }

    fun showRatingDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            showNumberPadDialog(
                context = context,
                titleResId = R.string.rating,
                initialValue = player.rating.asPersonalRating(context, 0),
                colorDescription = player.color,
                subtitle = player.fullDescription(context),
                minValue = 1.0,
                maxValue = 10.0,
                maxMantissa = 6,
            ) { rating ->
                viewModel.addRatingToPlayer(position, rating)
            }
        }
    }

    fun showColorDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            val disabledColors = players.filterIndexed { index, _ -> index != position }.map { it.color }
            showColorPickerDialog(
                context = context,
                title = player.fullDescription(context),
                featuredColors = gameColors,
                selectedColor = player.color,
                disabledColors = disabledColors,
            ) { color ->
                firebaseAnalytics.logEvent("DataManipulation") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                    param("Action", "Add")
                    param("Color", color)
                }
                viewModel.addColorToPlayer(position, color)
            }
        }
    }

    BackHandler(onBack = { systemBack() })

    AppTheme {
        LogPlayScreen(
            gameName = route.gameName,
            heroImageUrl = route.heroImageUrl,
            dateText = dateInMillis?.formatDateTime(
                context,
                flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_WEEKDAY,
            )?.toString().orEmpty(),
            isLoading = isLoading,
            showLocation = showLocation(context, isUserShowingLocation, location),
            location = location,
            showLength = showLength(context, isUserShowingLength, startTime, length),
            length = length,
            startTime = startTime,
            timerEnabled = ((dateInMillis ?: 0L) + (length * DateUtils.MINUTE_IN_MILLIS)).isToday(),
            showQuantity = showQuantity(context, isUserShowingQuantity, quantity),
            quantity = quantity,
            showIncomplete = showIncomplete(context, isUserShowingIncomplete, incomplete),
            incomplete = incomplete,
            showNoWinStats = showNoWinStats(context, isUserShowingNoWinStats, noWinStats),
            noWinStats = noWinStats,
            showComments = true,
            comments = comments,
            expansions = expansions,
            selectedExpansionIds = selectedExpansionIds,
            showPlayers = showPlayers(context, isUserShowingPlayers, players),
            playersLabel = if (players.isEmpty()) {
                stringResource(R.string.title_players)
            } else {
                stringResource(R.string.title_players_with_count, players.size)
            },
            canAssignColors = players.isNotEmpty(),
            players = players,
            availableFields = createAddFieldArray(
                context = context,
                showLocation = showLocation(context, isUserShowingLocation, location),
                showLength = showLength(context, isUserShowingLength, startTime, length),
                showQuantity = showQuantity(context, isUserShowingQuantity, quantity),
                showIncomplete = showIncomplete(context, isUserShowingIncomplete, incomplete),
                showNoWinStats = showNoWinStats(context, isUserShowingNoWinStats, noWinStats),
                startTime = startTime,
            ),
            fabColor = fabColor,
            onBack = { cancel() },
            onDone = { saveOrLog() },
            onDateClick = { showDatePicker() },
            onLocationChange = { viewModel.updateLocation(it.trim()) },
            onLengthChange = { viewModel.updateLength(it.toIntOrNull() ?: 0) },
            onStartTimerClick = {
                if (length == 0) {
                    viewModel.startTimer()
                } else {
                    context.createThemedBuilder()
                        .setMessage(R.string.are_you_sure_timer_reset)
                        .setPositiveButton(R.string.continue_) { _, _ -> viewModel.resumeTimer() }
                        .setNegativeButton(R.string.reset) { _, _ -> viewModel.startTimer() }
                        .setCancelable(true)
                        .show()
                }
            },
            onStopTimerClick = {
                viewModel.endTimer()
                context.cancelNotification(TAG_PLAY_TIMER, internalId)
            },
            onQuantityChange = { viewModel.updateQuantity(it.toIntOrNull()) },
            onIncompleteChange = { viewModel.updateIncomplete(it) },
            onNoWinStatsChange = { viewModel.updateNoWinStats(it) },
            onCommentsChange = { viewModel.updateComments(it) },
            onExpansionToggle = { expansionId, isSelected -> viewModel.toggleExpansion(expansionId, isSelected) },
            onAssignColorsClick = { assignColors() },
            onSortPlayersClick = { showPlayerSortMenu() },
            onAddField = { addField(it) },
            onEditPlayer = { editPlayer(it) },
            onScorePlayer = { showScoreDialog(it) },
            onRatingPlayer = { showRatingDialog(it) },
            onColorPlayer = { showColorDialog(it) },
            onToggleWin = { index, isWin -> viewModel.win(isWin, index) },
            onToggleNew = { index, isNew -> viewModel.new(isNew, index) },
        )
    }
}

private fun addFieldName(context: Context, field: AddLogPlayField): String {
    return when (field) {
        AddLogPlayField.Location -> context.getString(R.string.location)
        AddLogPlayField.Length -> context.getString(R.string.length)
        AddLogPlayField.Quantity -> context.getString(R.string.quantity)
        AddLogPlayField.Incomplete -> context.getString(R.string.incomplete)
        AddLogPlayField.NoWinStats -> context.getString(R.string.noWinStats)
        AddLogPlayField.Comments -> context.getString(R.string.comments)
        AddLogPlayField.Players -> context.getString(R.string.title_players)
    }
}

private fun createAddFieldArray(
    context: Context,
    showLocation: Boolean,
    showLength: Boolean,
    showQuantity: Boolean,
    showIncomplete: Boolean,
    showNoWinStats: Boolean,
    startTime: Long,
): List<AddLogPlayField> {
    val list = mutableListOf<AddLogPlayField>()
    if (!showLocation) list.add(AddLogPlayField.Location)
    if (!showLength && startTime <= 0L) list.add(AddLogPlayField.Length)
    if (!showQuantity) list.add(AddLogPlayField.Quantity)
    if (!showIncomplete) list.add(AddLogPlayField.Incomplete)
    if (!showNoWinStats) list.add(AddLogPlayField.NoWinStats)
    list.add(AddLogPlayField.Players)
    return list
}

private fun showLocation(context: Context, isUserShowingLocation: Boolean, location: String): Boolean {
    return isUserShowingLocation || context.preferences().showLogPlayLocation() || location.isNotEmpty()
}

private fun showLength(context: Context, isUserShowingLength: Boolean, startTime: Long, length: Int): Boolean {
    return startTime > 0L || length > 0 || isUserShowingLength || context.preferences().showLogPlayLength()
}

private fun showQuantity(context: Context, isUserShowingQuantity: Boolean, quantity: Int): Boolean {
    return quantity != 1 || isUserShowingQuantity || context.preferences().showLogPlayQuantity()
}

private fun showIncomplete(context: Context, isUserShowingIncomplete: Boolean, incomplete: Boolean): Boolean {
    return incomplete || isUserShowingIncomplete || context.preferences().showLogPlayIncomplete()
}

private fun showNoWinStats(context: Context, isUserShowingNoWinStats: Boolean, noWinStats: Boolean): Boolean {
    return noWinStats || isUserShowingNoWinStats || context.preferences().showLogPlayNoWinStats()
}

private fun showPlayers(context: Context, isUserShowingPlayers: Boolean, players: List<PlayPlayer>): Boolean {
    return isUserShowingPlayers || context.preferences().showLogPlayPlayerList() || players.isNotEmpty()
}

private fun showPlayersToAddDialog(
    context: Context,
    availablePlayers: List<Player>,
    onPlayersAdded: (List<Player>) -> Unit,
    onAddMore: () -> Unit,
): Boolean {
    if (availablePlayers.isEmpty()) return false
    val selectedPlayers = mutableListOf<Player>()
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.title_add_players)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onPlayersAdded(selectedPlayers)
        }
        .setNeutralButton(R.string.more) { _, _ ->
            onPlayersAdded(selectedPlayers)
            onAddMore()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .setMultiChoiceItems(availablePlayers.map { it.description }.toTypedArray(), null) { _, which, isChecked ->
            val player = availablePlayers[which]
            if (isChecked) {
                selectedPlayers.add(player)
            } else {
                selectedPlayers.remove(player)
            }
        }
        .show()
    return true
}

private fun showNumberPadDialog(
    context: Context,
    @androidx.annotation.StringRes titleResId: Int,
    initialValue: String,
    colorDescription: String? = null,
    subtitle: String? = null,
    minValue: Double = -Double.MAX_VALUE,
    maxValue: Double = Double.MAX_VALUE,
    maxMantissa: Int = 10,
    onDone: (Double) -> Unit,
) {
    val activity = context.findActivity() ?: return
    val binding = DialogNumberPadBinding.inflate(activity.layoutInflater)
    val decimal = DecimalFormatSymbols.getInstance().decimalSeparator
    binding.decimalSeparator.text = decimal.toString()
    binding.plusMinusView.visibility = if (minValue < 0.0) View.VISIBLE else View.GONE
    binding.titleView.setText(titleResId)
    binding.subtitleView.setTextOrHide(subtitle)

    if (initialValue.isNotBlank()) {
        binding.outputView.text = initialValue
    }

    val color = colorDescription.asColorRgb()
    if (color != Color.TRANSPARENT) {
        binding.headerView.setBackgroundColor(color)
        val textColor = color.getTextColor()
        binding.titleView.setTextColor(textColor)
        binding.subtitleView.setTextColor(textColor)
    }

    fun parseOutput(text: String): Double {
        val parsableText = when {
            text.isEmpty() || text == decimal.toString() || text == "-" || text == "-$decimal" -> ""
            text.endsWith(decimal) -> "${text}0"
            text.startsWith(decimal) -> "0$text"
            text.startsWith("-$decimal") -> "-0${text.substring(1)}"
            else -> text
        }
        return try {
            NumberFormat.getNumberInstance().parse(parsableText)?.toDouble() ?: 0.0
        } catch (_: ParseException) {
            0.0
        }
    }

    fun hasTwoDecimalPoints(text: String): Boolean {
        val decimalIndex = text.indexOf(decimal)
        return decimalIndex >= 0 && text.indexOf(decimal, decimalIndex + 1) >= 0
    }

    fun isWithinLength(text: String): Boolean {
        if (text.isEmpty()) return true
        val mantissaLength = text.substringAfter(decimal, "").length
        return text.length <= 10 && mantissaLength <= maxMantissa
    }

    fun isWithinRange(text: String): Boolean {
        if (text.isEmpty() || text == decimal.toString() || text == "-$decimal") return true
        if (hasTwoDecimalPoints(text)) return false
        val value = parseOutput(text)
        return value in minValue..maxValue
    }

    fun maybeUpdateOutput(output: String, source: View) {
        if (isWithinLength(output) && isWithinRange(output)) {
            if (context.preferences()[KEY_HAPTIC_FEEDBACK, true] == true) {
                source.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            binding.outputView.text = output
            binding.deleteView.isEnabled = binding.outputView.length() > 0
        }
    }

    binding.deleteView.isEnabled = binding.outputView.length() > 0
    binding.deleteView.setOnClickListener {
        val text = binding.outputView.text
        if (text.isNotEmpty()) {
            maybeUpdateOutput(text.substring(0, text.length - 1), it)
        }
    }
    binding.deleteView.setOnLongClickListener {
        binding.outputView.clearText()
        binding.deleteView.isEnabled = false
        true
    }
    binding.plusMinusView.setOnClickListener {
        val output = binding.outputView.text.toString()
        val signedOutput = if (output.startsWith("-")) output.substring(1) else "-$output"
        maybeUpdateOutput(signedOutput, it)
    }
    binding.numberPadView.childrenRecursiveSequence().filterIsInstance<TextView>().forEach { view ->
        view.setOnClickListener {
            maybeUpdateOutput(binding.outputView.text.toString() + view.text, view)
        }
    }

    val dialog = context.createThemedBuilder().setView(binding.root).create()
    binding.doneView.setOnClickListener {
        onDone(parseOutput(binding.outputView.text.toString()))
        dialog.dismiss()
    }
    dialog.show()
    dialog.window?.let { window ->
        val width = minOf(
            context.resources.getDimensionPixelSize(R.dimen.dialog_width),
            context.resources.displayMetrics.widthPixels * 3 / 4,
        )
        window.setLayout(width, window.attributes.height)
    }
}

private fun showColorPickerDialog(
    context: Context,
    title: String,
    featuredColors: List<String>,
    selectedColor: String?,
    disabledColors: List<String>,
    onColorSelected: (String) -> Unit,
) {
    val activity = context.findActivity() ?: return
    val binding = DialogColorsBinding.inflate(activity.layoutInflater)
    binding.addButton.visibility = View.GONE

    val choices = ArrayList(BggColors.colorList)
    val featured = ArrayList<Pair<String, Int>>()
    for (index in choices.indices.reversed()) {
        val pair = choices[index]
        if (featuredColors.contains(pair.first)) {
            choices.removeAt(index)
            featured.add(0, pair)
        }
    }

    val colorGridAdapter = com.boardgamegeek.ui.adapter.ColorGridAdapter(choices, ArrayList(disabledColors))
    colorGridAdapter.selectedColor = selectedColor
    binding.colorGrid.adapter = colorGridAdapter

    if (featured.isNotEmpty()) {
        val featuredGridAdapter = com.boardgamegeek.ui.adapter.ColorGridAdapter(featured, ArrayList(disabledColors))
        featuredGridAdapter.selectedColor = selectedColor
        binding.featuredColorGrid.adapter = featuredGridAdapter
        binding.featuredColorGrid.visibility = View.VISIBLE
        binding.moreView.visibility = View.VISIBLE
        binding.colorGrid.visibility = View.GONE
        binding.moreView.setOnClickListener {
            binding.moreView.visibility = View.GONE
            binding.dividerView.visibility = View.VISIBLE
            binding.colorGrid.visibility = View.VISIBLE
        }
    } else {
        binding.featuredColorGrid.visibility = View.GONE
        binding.moreView.visibility = View.GONE
        binding.colorGrid.visibility = View.VISIBLE
    }

    val dialog = context.createThemedBuilder()
        .setTitle(title)
        .setView(binding.root)
        .create()

    listOf(binding.colorGrid, binding.featuredColorGrid).forEach { grid ->
        grid.setOnItemClickListener { parent, _, position, _ ->
            val item = (parent.adapter as? com.boardgamegeek.ui.adapter.ColorGridAdapter)?.getItem(position)
            if (item != null) {
                onColorSelected(item.first)
            }
            dialog.dismiss()
        }
    }

    dialog.show()
}

private fun logPlayerOrder(firebaseAnalytics: FirebaseAnalytics, order: String) {
    firebaseAnalytics.logEvent("LogPlayPlayerOrder") {
        param("Order", order)
    }
}
