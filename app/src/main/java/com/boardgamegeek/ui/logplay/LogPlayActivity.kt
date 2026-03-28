package com.boardgamegeek.ui.logplay

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogColorsBinding
import com.boardgamegeek.databinding.DialogNumberPadBinding
import com.boardgamegeek.databinding.RowLogplayPlayerBinding
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
import com.boardgamegeek.extensions.getBitmap
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.indefiniteSnackbar
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.isToday
import com.boardgamegeek.extensions.launchPlayingNotification
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.extensions.setTextWithStyle
import com.boardgamegeek.extensions.showLogPlayComments
import com.boardgamegeek.extensions.showLogPlayIncomplete
import com.boardgamegeek.extensions.showLogPlayLength
import com.boardgamegeek.extensions.showLogPlayLocation
import com.boardgamegeek.extensions.showLogPlayNoWinStats
import com.boardgamegeek.extensions.showLogPlayPlayerList
import com.boardgamegeek.extensions.showLogPlayQuantity
import com.boardgamegeek.extensions.toast
import com.boardgamegeek.extensions.fromLocalToUtc
import com.boardgamegeek.model.GameExpansion
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.adapter.ColorGridAdapter
import com.boardgamegeek.ui.logplayer.LogPlayerActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Calendar
import kotlin.math.abs

@AndroidEntryPoint
class LogPlayActivity : AppCompatActivity() {
    private val playerAdapter: PlayerAdapter by lazy { PlayerAdapter() }
    private val viewModel by viewModels<LogPlayViewModel>()
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }
    
    private var internalId = INVALID_ID.toLong()
    private var gameId = INVALID_ID
    private var gameName: String = ""
    private var isRequestingToEndPlay = false
    private var isRequestingRematch = false
    private var isChangingGame = false
    private var heroImageUrl: String = ""

    private var lastRemovedPlayer: PlayPlayer? = null
    private val gameColors = ArrayList<String>()
    private val availablePlayers = mutableListOf<Player>()

    private var fabColor by mutableStateOf(Color.TRANSPARENT)
    private val swipePaint = Paint()
    private val deleteIcon: Bitmap by lazy { this.getBitmap(R.drawable.ic_baseline_delete_24, Color.WHITE) }
    private var horizontalPadding = 0f
    private var itemTouchHelper: ItemTouchHelper? = null
    private var playerRecyclerView: RecyclerView? = null
                    
    private var isLoading by mutableStateOf(false)
    private var isUserShowingLocation by mutableStateOf(false)
    private var isUserShowingLength by mutableStateOf(false)
    private var isUserShowingQuantity by mutableStateOf(false)
    private var isUserShowingIncomplete by mutableStateOf(false)
    private var isUserShowingNoWinStats by mutableStateOf(false)
    private var isUserShowingComments by mutableStateOf(false)
    private var isUserShowingPlayers by mutableStateOf(false)

    private var shouldDeletePlayOnActivityCancel = false
    private var isLaunchingActivity = false
    private var shouldSaveOnPause = true

    private var dateInMillis by mutableStateOf<Long?>(null)
    private var location by mutableStateOf("")
    private var startTime by mutableStateOf(0L)
    private var length by mutableStateOf(0)
    private var quantity by mutableStateOf(1)
    private var incomplete by mutableStateOf(false)
    private var noWinStats by mutableStateOf(false)
    private var comments by mutableStateOf("")
    private var expansions by mutableStateOf(emptyList<GameExpansion>())
    private var selectedExpansionIds by mutableStateOf(emptySet<Int>())
    private var playersHaveStartingPositions = false
    private var players by mutableStateOf(emptyList<PlayPlayer>())
    private var shouldCustomSortPlayers by mutableStateOf(false)
    private var playerDescriptions = emptyList<String>()
    private var usedColors = emptyList<String>()

    private val addPlayerLauncher: ActivityResultLauncher<LogPlayerActivity.LaunchInput> = registerForActivityResult(
        LogPlayerActivity.AddPlayerContract()
    ) { player ->
        player?.let {
            viewModel.addPlayer(it)
            addNewPlayer(players.size + 2)
        }
    }

    private val editPlayerLauncher = registerForActivityResult(LogPlayerActivity.EditPlayerContract()) { (position, player) ->
        when {
            position == LogPlayerActivity.INVALID_POSITION -> Timber.w("Invalid player position after edit")
            player == null -> Timber.w("No player found after edit")
            else -> viewModel.editPlayer(player, position)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_CANCELED)
            if (viewModel.isDirty()) {
                saveDraft(true)
                toast(R.string.msg_saving_draft)
            } else {
                shouldSaveOnPause = false
                finish()
            }
        }

        horizontalPadding = resources.getDimension(R.dimen.material_margin_horizontal)
        swipePaint.color = ContextCompat.getColor(this, R.color.delete)
        setupItemTouchHelper()

        readIntent(savedInstanceState)

        FirebaseAnalytics.getInstance(this).logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", if (internalId == INVALID_ID.toLong()) "Action" else "Edit")
            param("GameName", gameName)
        }

        if (gameId <= 0) {
            val message = "Can't log a play without a game ID."
            Timber.w(message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        shouldDeletePlayOnActivityCancel = if (internalId == INVALID_ID.toLong()) true else (isRequestingRematch || isChangingGame)

        setContent {
            AppTheme {
                LogPlayScreen(
                    gameName = gameName,
                    heroImageUrl = heroImageUrl,
                    dateText = dateInMillis?.formatDateTime(
                        this,
                        flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_WEEKDAY
                    )?.toString() ?: "",
                    isLoading = isLoading,
                    showLocation = showLocation(),
                    location = location,
                    showLength = showLength(),
                    length = length,
                    startTime = startTime,
                    timerEnabled = ((dateInMillis ?: 0) + (length * DateUtils.MINUTE_IN_MILLIS)).isToday(),
                    showQuantity = showQuantity(),
                    quantity = quantity,
                    showIncomplete = showIncomplete(),
                    incomplete = incomplete,
                    showNoWinStats = showNoWinStats(),
                    noWinStats = noWinStats,
                    showComments = showComments(),
                    comments = comments,
                    expansions = expansions,
                    selectedExpansionIds = selectedExpansionIds,
                    showPlayers = showPlayers(),
                    playersLabel = if (players.isEmpty()) getString(R.string.title_players) else getString(R.string.title_players_with_count, players.size),
                    canAssignColors = players.isNotEmpty(),
                    players = players,
                    availableFields = createAddFieldArray(),
                    fabColor = fabColor,
                    onBack = { cancel() },
                    onDone = { saveOrLog() },
                    onDateClick = { showDatePicker() },
                    onLocationChange = {
                        location = it
                        viewModel.updateLocation(it.trim())
                        updateNotification()
                    },
                    onLengthChange = {
                        length = it.toIntOrNull() ?: 0
                        viewModel.updateLength(length)
                    },
                    onStartTimerClick = {
                        if (length == 0) {
                            viewModel.startTimer()
                        } else {
                            this.createThemedBuilder()
                                .setMessage(R.string.are_you_sure_timer_reset)
                                .setPositiveButton(R.string.continue_) { _, _ -> viewModel.resumeTimer() }
                                .setNegativeButton(R.string.reset) { _, _ -> viewModel.startTimer() }
                                .setCancelable(true)
                                .show()
                        }
                    },
                    onStopTimerClick = {
                        isRequestingToEndPlay = true
                        viewModel.endTimer()
                        cancelPlayingNotification()
                    },
                    onQuantityChange = {
                        quantity = it.toIntOrNull() ?: 1
                        viewModel.updateQuantity(it.toIntOrNull())
                    },
                    onIncompleteChange = {
                        incomplete = it
                        viewModel.updateIncomplete(it)
                    },
                    onNoWinStatsChange = {
                        noWinStats = it
                        viewModel.updateNoWinStats(it)
                    },
                    onCommentsChange = {
                        comments = it
                        viewModel.updateComments(it)
                    },
                    onExpansionToggle = { expansionId, isSelected ->
                        viewModel.toggleExpansion(expansionId, isSelected)
                    },
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

        if (isRequestingToEndPlay) {
            cancelPlayingNotification()
        }

        observeViewModel()
        viewModel.loadPlay(internalId, gameId, gameName, isRequestingToEndPlay, isRequestingRematch, isChangingGame)
    }

    override fun onResume() {
        super.onResume()
        isLaunchingActivity = false
        shouldSaveOnPause = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_INTERNAL_ID, internalId)
        outState.putBoolean(KEY_IS_USER_SHOWING_LOCATION, isUserShowingLocation)
        outState.putBoolean(KEY_IS_USER_SHOWING_LENGTH, isUserShowingLength)
        outState.putBoolean(KEY_IS_USER_SHOWING_QUANTITY, isUserShowingQuantity)
        outState.putBoolean(KEY_IS_USER_SHOWING_INCOMPLETE, isUserShowingIncomplete)
        outState.putBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS, isUserShowingNoWinStats)
        outState.putBoolean(KEY_IS_USER_SHOWING_COMMENTS, isUserShowingComments)
        outState.putBoolean(KEY_IS_USER_SHOWING_PLAYERS, isUserShowingPlayers)
        outState.putBoolean(KEY_CUSTOM_PLAYER_SORT, shouldCustomSortPlayers)
    }

    override fun onPause() {
        super.onPause()
        updateNotification()
        if (shouldSaveOnPause && !isLaunchingActivity) {
            saveDraft(false)
        }
    }

    private fun readIntent(savedInstanceState: Bundle?) {
        internalId = intent.getLongExtra(KEY_ID, INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        isRequestingToEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false)
        isRequestingRematch = intent.getBooleanExtra(KEY_REMATCH, false)
        isChangingGame = intent.getBooleanExtra(KEY_CHANGE_GAME, false)
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        shouldCustomSortPlayers = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)

        savedInstanceState?.let {
            internalId = it.getLong(KEY_INTERNAL_ID, INVALID_ID.toLong())
            isUserShowingLocation = it.getBoolean(KEY_IS_USER_SHOWING_LOCATION)
            isUserShowingLength = it.getBoolean(KEY_IS_USER_SHOWING_LENGTH)
            isUserShowingQuantity = it.getBoolean(KEY_IS_USER_SHOWING_QUANTITY)
            isUserShowingIncomplete = it.getBoolean(KEY_IS_USER_SHOWING_INCOMPLETE)
            isUserShowingNoWinStats = it.getBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS)
            isUserShowingComments = it.getBoolean(KEY_IS_USER_SHOWING_COMMENTS)
            isUserShowingPlayers = it.getBoolean(KEY_IS_USER_SHOWING_PLAYERS)
            shouldCustomSortPlayers = it.getBoolean(KEY_CUSTOM_PLAYER_SORT)
        }

        fabColor = ContextCompat.getColor(this, R.color.accent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading = it }
                }
                launch {
                    viewModel.customPlayerSort.collect { shouldCustomSortPlayers = it }
                }
                launch {
                    viewModel.colors.collect {
                        gameColors.clear()
                        gameColors.addAll(it)
                    }
                }
                launch {
                    viewModel.locations.collect { }
                }
                launch {
                    viewModel.playersByLocation.collect {
                        availablePlayers.clear()
                        availablePlayers.addAll(it)
                    }
                }
                launch {
                    viewModel.internalId.collect {
                        internalId = it
                        updateNotification()
                    }
                }
                launch {
                    viewModel.canFinish.collect { finish() }
                }
                launch {
                    viewModel.dateInMillis.collect { dateInMillis = it }
                }
                launch {
                    viewModel.location.collect {
                        location = it
                        updateNotification()
                    }
                }
                launch {
                    viewModel.length.collect { length = it }
                }
                launch {
                    viewModel.startTime.collect {
                        startTime = it
                        updateNotification()
                    }
                }
                launch {
                    viewModel.quantity.collect { quantity = it }
                }
                launch {
                    viewModel.incomplete.collect { incomplete = it }
                }
                launch {
                    viewModel.doNotCountWinStats.collect { noWinStats = it }
                }
                launch {
                    viewModel.comments.collect { comments = it }
                }
                launch {
                    viewModel.loggableExpansions.collect { expansions = it }
                }
                launch {
                    viewModel.selectedExpansionIds.collect { selectedExpansionIds = it }
                }
                launch {
                    viewModel.players.collect { value ->
                playersHaveStartingPositions = value.any { player -> player.startingPosition.isNotBlank() }
                players = value
                playerDescriptions = value.mapIndexed { i, p ->
                    p.description.ifEmpty { String.format(resources.getString(R.string.generic_player), i + 1) }
                }
                usedColors = value.map { p -> p.color }
                playerAdapter.submit(value)
                updateNotification()
                    }
                }
            }
        }
    }

    private fun setupItemTouchHelper() {
        itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean,
                ) {
                    if (viewHolder is PlayerAdapter.PlayerViewHolder && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val itemView = viewHolder.itemView

                        val width = itemView.width.toFloat()
                        val alpha = 1.0f - abs(dX) / width
                        itemView.alpha = alpha
                        itemView.translationX = dX

                        val verticalPadding = (itemView.height - deleteIcon.height) / 2f
                        val background: RectF
                        val iconSrc: Rect
                        val iconDst: RectF
                        if (dX > 0) {
                            background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                            iconSrc = Rect(
                                0,
                                0,
                                (dX - itemView.left - horizontalPadding).toInt().coerceAtMost(deleteIcon.width),
                                deleteIcon.height,
                            )
                            iconDst = RectF(
                                itemView.left.toFloat() + horizontalPadding,
                                itemView.top.toFloat() + verticalPadding,
                                (itemView.left + horizontalPadding + deleteIcon.width).coerceAtMost(dX),
                                itemView.bottom.toFloat() - verticalPadding,
                            )
                        } else {
                            background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                            iconSrc = Rect(
                                (deleteIcon.width + horizontalPadding.toInt() + dX.toInt()).coerceAtLeast(0),
                                0,
                                deleteIcon.width,
                                deleteIcon.height,
                            )
                            iconDst = RectF(
                                (itemView.right.toFloat() + dX).coerceAtLeast(itemView.right.toFloat() - horizontalPadding - deleteIcon.width),
                                itemView.top.toFloat() + verticalPadding,
                                itemView.right.toFloat() - horizontalPadding,
                                itemView.bottom.toFloat() - verticalPadding,
                            )
                        }

                        c.drawRect(background, swipePaint)
                        c.drawBitmap(deleteIcon, iconSrc, iconDst, swipePaint)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    lastRemovedPlayer = playerAdapter.getPlayer(viewHolder.bindingAdapterPosition)
                    lastRemovedPlayer?.let { player ->
                        findViewById<View>(android.R.id.content).indefiniteSnackbar(
                            getString(R.string.msg_player_deleted, player.fullDescription(this@LogPlayActivity)),
                            getString(R.string.undo)
                        ) {
                            lastRemovedPlayer?.let { viewModel.addPlayer(it) }
                        }
                        viewModel.removePlayer(player)
                    }
                }

                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    if (target !is PlayerAdapter.PlayerViewHolder) return false
                    viewModel.reorderPlayers(viewHolder.bindingAdapterPosition + 1, target.bindingAdapterPosition + 1)
                    return true
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    (viewHolder as? PlayerAdapter.PlayerViewHolder)?.onItemClear()
                    super.clearView(recyclerView, viewHolder)
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        (viewHolder as? PlayerAdapter.PlayerViewHolder)?.onItemDragging()
                    }
                    super.onSelectedChanged(viewHolder, actionState)
                }

                override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    return if (shouldCustomSortPlayers) {
                        makeMovementFlags(0, getSwipeDirs(recyclerView, viewHolder))
                    } else {
                        super.getMovementFlags(recyclerView, viewHolder)
                    }
                }

                override fun isLongPressDragEnabled() = false
            }
        )
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateInMillis ?: System.currentTimeMillis()
        }
        DatePickerDialog(
            this,
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

    private fun saveOrLog() {
        shouldSaveOnPause = false
        viewModel.updateComments(comments)
        if (startTime > 0L) {
            toast(R.string.msg_saving_draft)
            viewModel.saveDraft()
        } else {
            toast(R.string.msg_logging_play)
            viewModel.logPlay()
            cancelPlayingNotification()
        }
        setResult(RESULT_OK)
    }

    private fun saveDraft(wantToFinish: Boolean) {
        shouldSaveOnPause = false
        viewModel.updateComments(comments)
        viewModel.saveDraft(wantToFinish)
    }

    private fun cancel() {
        shouldSaveOnPause = false
        if (viewModel.isDirty()) {
            if (shouldDeletePlayOnActivityCancel) {
                createDiscardDialog(R.string.play, isNew = true, finishActivity = false) {
                    viewModel.deletePlay()
                    cancelPlayingNotification()
                    setResult(RESULT_CANCELED)
                }.show()
            } else {
                createDiscardDialog(R.string.play, isNew = false).show()
            }
        } else {
            if (shouldDeletePlayOnActivityCancel) {
                viewModel.deletePlay()
                cancelPlayingNotification()
                setResult(RESULT_CANCELED)
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun updateNotification() {
        if (internalId != INVALID_ID.toLong()) {
            if (startTime > 0L) {
                this.launchPlayingNotification(internalId, gameName, location, players.size, startTime, heroImageUrl)
            } else {
                cancelPlayingNotification()
            }
        }
    }

    private fun assignColors() {
        if (usedColors.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
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

    private fun showPlayerSortMenu() {
        val anchor = playerRecyclerView ?: findViewById(android.R.id.content)
        val popup = PopupMenu(this, anchor)
        popup.inflate(if (!shouldCustomSortPlayers && players.size > 1) R.menu.log_play_player_sort else R.menu.log_play_player_sort_short)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_custom_player_order -> {
                    if (shouldCustomSortPlayers) {
                        logPlayerOrder("NotCustom")
                        if (playersHaveStartingPositions) {
                            MaterialAlertDialogBuilder(this)
                                .setMessage(R.string.are_you_sure_player_sort_custom_off)
                                .setCancelable(true)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.sort) { _: DialogInterface?, _: Int -> viewModel.shouldCustomSort(false) }
                                .create()
                                .show()
                        } else {
                            viewModel.shouldCustomSort(false)
                        }
                    } else {
                        logPlayerOrder("Custom")
                        if (playersHaveStartingPositions) {
                            MaterialAlertDialogBuilder(this)
                                .setMessage(R.string.message_custom_player_order)
                                .setPositiveButton(R.string.keep) { _: DialogInterface?, _: Int -> viewModel.shouldCustomSort(true) }
                                .setNegativeButton(R.string.clear) { _: DialogInterface?, _: Int ->
                                    viewModel.shouldCustomSort(true)
                                    viewModel.clearPositions()
                                }
                                .setCancelable(true)
                                .show()
                        }
                    }
                    true
                }

                R.id.menu_pick_start_player -> {
                    logPlayerOrder("Prompt")
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.title_pick_start_player)
                        .setItems(playerDescriptions.toTypedArray()) { _, which: Int ->
                            viewModel.pickStartPlayer(which)
                        }
                        .show()
                    true
                }

                R.id.menu_random_start_player -> {
                    logPlayerOrder("RandomStarter")
                    viewModel.randomizeStartPlayer()
                    true
                }

                R.id.menu_random_player_order -> {
                    logPlayerOrder("Random")
                    viewModel.randomizePlayerOrder()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun addField(field: AddLogPlayField) {
        when (field) {
            AddLogPlayField.Location -> {
                isUserShowingLocation = true
            }

            AddLogPlayField.Length -> {
                isUserShowingLength = true
            }

            AddLogPlayField.Quantity -> {
                isUserShowingQuantity = true
                quantity = 1
                viewModel.updateQuantity(1)
            }

            AddLogPlayField.Incomplete -> {
                isUserShowingIncomplete = true
                incomplete = true
                viewModel.updateIncomplete(true)
            }

            AddLogPlayField.NoWinStats -> {
                isUserShowingNoWinStats = true
                noWinStats = true
                viewModel.updateNoWinStats(true)
            }

            AddLogPlayField.Comments -> {
                isUserShowingComments = true
            }

            AddLogPlayField.Players -> {
                isUserShowingPlayers = true
                if (preferences()[LOG_EDIT_PLAYER_PROMPTED, false] == true) {
                    addPlayers(preferences()[LOG_EDIT_PLAYER, false] ?: false)
                } else {
                    promptToEditPlayers()
                }
            }
        }
        firebaseAnalytics.logEvent("AddField") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param(FirebaseAnalytics.Param.ITEM_NAME, addFieldName(field))
        }
    }

    private fun addFieldName(field: AddLogPlayField): String {
        return when (field) {
            AddLogPlayField.Location -> getString(R.string.location)
            AddLogPlayField.Length -> getString(R.string.length)
            AddLogPlayField.Quantity -> getString(R.string.quantity)
            AddLogPlayField.Incomplete -> getString(R.string.incomplete)
            AddLogPlayField.NoWinStats -> getString(R.string.noWinStats)
            AddLogPlayField.Comments -> getString(R.string.comments)
            AddLogPlayField.Players -> getString(R.string.title_players)
        }
    }

    private fun createAddFieldArray(): List<AddLogPlayField> {
        val list = mutableListOf<AddLogPlayField>()
        if (!showLocation()) list.add(AddLogPlayField.Location)
        if (!showLength() && startTime <= 0L) list.add(AddLogPlayField.Length)
        if (!showQuantity()) list.add(AddLogPlayField.Quantity)
        if (!showIncomplete()) list.add(AddLogPlayField.Incomplete)
        if (!showNoWinStats()) list.add(AddLogPlayField.NoWinStats)
        if (!showComments()) list.add(AddLogPlayField.Comments)
        list.add(AddLogPlayField.Players)
        return list
    }

    private fun addPlayers(editPlayer: Boolean) {
        if (editPlayer) {
            if (!showPlayersToAddDialog()) {
                addNewPlayer()
            }
        } else {
            viewModel.addPlayer()
        }
    }

    private fun promptToEditPlayers() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pref_edit_player_prompt_title)
            .setMessage(R.string.pref_edit_player_prompt_message)
            .setCancelable(true)
            .setPositiveButton(R.string.pref_edit_player_prompt_positive, onPromptClickListener(true))
            .setNegativeButton(R.string.pref_edit_player_prompt_negative, onPromptClickListener(false))
            .create()
            .show()
        preferences()[LOG_EDIT_PLAYER_PROMPTED] = true
    }

    private fun onPromptClickListener(value: Boolean): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            preferences()[LOG_EDIT_PLAYER] = value
            addPlayers(value)
        }
    }

    private fun showPlayersToAddDialog(): Boolean {
        if (availablePlayers.isEmpty()) return false
        val playersToAdd = mutableListOf<Player>()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_add_players)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.addPlayers(playersToAdd)
            }
            .setNeutralButton(R.string.more) { _, _ ->
                viewModel.addPlayers(playersToAdd)
                addNewPlayer()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setMultiChoiceItems(availablePlayers.map { it.description }.toTypedArray<CharSequence>(), null) { _, which, isChecked ->
                val player = availablePlayers[which]
                if (isChecked) {
                    playersToAdd.add(player)
                } else {
                    playersToAdd.remove(player)
                }
            }
            .create()
            .show()
        return true
    }

    private fun addNewPlayer(autoPosition: Int = players.size + 1) {
        isLaunchingActivity = true
        addPlayerLauncher.launch(createLaunchInput(autoPosition))
    }

    private fun editPlayer(position: Int) {
        isLaunchingActivity = true
        val player = players.getOrNull(position)
        if (player != null) {
            val input = createLaunchInput(player.seat)
            editPlayerLauncher.launch(input to (position to player))
        } else {
            Timber.w("Attempting to edit a null player at position $position")
        }
    }

    private fun showScoreDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            showNumberPadDialog(
                R.string.score,
                player.score,
                player.color,
                player.fullDescription(this),
            ) { score ->
                viewModel.addScoreToPlayer(position, score)
            }
        }
    }

    private fun showRatingDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            showNumberPadDialog(
                R.string.rating,
                player.rating.asPersonalRating(this, 0),
                player.color,
                player.fullDescription(this),
                minValue = 1.0,
                maxValue = 10.0,
                maxMantissa = 6,
            ) { rating ->
                viewModel.addRatingToPlayer(position, rating)
            }
        }
    }

    private fun showColorDialog(position: Int) {
        players.getOrNull(position)?.let { player ->
            val usedColors = players.filterIndexed { i, _ -> i != position }.map { it.color }
            showColorPickerDialog(
                title = player.fullDescription(this),
                featuredColors = gameColors,
                selectedColor = player.color,
                disabledColors = usedColors,
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

    private fun showNumberPadDialog(
        @androidx.annotation.StringRes titleResId: Int,
        initialValue: String,
        colorDescription: String? = null,
        subtitle: String? = null,
        minValue: Double = -Double.MAX_VALUE,
        maxValue: Double = Double.MAX_VALUE,
        maxMantissa: Int = 10,
        onDone: (Double) -> Unit,
    ) {
        val binding = DialogNumberPadBinding.inflate(layoutInflater)
        val decimal = DecimalFormatSymbols.getInstance().decimalSeparator
        binding.decimalSeparator.text = decimal.toString()
        binding.plusMinusView.isVisible = minValue < 0.0
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
                if (preferences()[KEY_HAPTIC_FEEDBACK, true] == true) {
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

        val dialog = createThemedBuilder().setView(binding.root).create()
        binding.doneView.setOnClickListener {
            onDone(parseOutput(binding.outputView.text.toString()))
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.let { window ->
            val width = minOf(
                resources.getDimensionPixelSize(R.dimen.dialog_width),
                resources.displayMetrics.widthPixels * 3 / 4,
            )
            window.setLayout(width, window.attributes.height)
        }
    }

    private fun showColorPickerDialog(
        title: String,
        featuredColors: List<String>,
        selectedColor: String?,
        disabledColors: List<String>,
        onColorSelected: (String) -> Unit,
    ) {
        val binding = DialogColorsBinding.inflate(layoutInflater)
        binding.addButton.isVisible = false

        val choices = ArrayList(BggColors.colorList)
        val featured = ArrayList<Pair<String, Int>>()
        for (i in choices.indices.reversed()) {
            val pair = choices[i]
            if (featuredColors.contains(pair.first)) {
                choices.removeAt(i)
                featured.add(0, pair)
            }
        }

        val colorGridAdapter = ColorGridAdapter(choices, ArrayList(disabledColors))
        colorGridAdapter.selectedColor = selectedColor
        binding.colorGrid.adapter = colorGridAdapter

        if (featured.isNotEmpty()) {
            val featuredGridAdapter = ColorGridAdapter(featured, ArrayList(disabledColors))
            featuredGridAdapter.selectedColor = selectedColor
            binding.featuredColorGrid.adapter = featuredGridAdapter
            binding.featuredColorGrid.isVisible = true
            binding.moreView.isVisible = true
            binding.colorGrid.isVisible = false
            binding.moreView.setOnClickListener {
                binding.moreView.isVisible = false
                binding.dividerView.isVisible = true
                binding.colorGrid.isVisible = true
            }
        } else {
            binding.featuredColorGrid.isVisible = false
            binding.moreView.isVisible = false
            binding.colorGrid.isVisible = true
        }

        val dialog = createThemedBuilder()
            .setTitle(title)
            .setView(binding.root)
            .create()

        listOf(binding.colorGrid, binding.featuredColorGrid).forEach { grid ->
            grid.setOnItemClickListener { parent, _, position, _ ->
                val item = (parent.adapter as? ColorGridAdapter)?.getItem(position)
                if (item != null) {
                    onColorSelected(item.first)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun createLaunchInput(autoPosition: Int) = LogPlayerActivity.LaunchInput(
        gameId = gameId,
        gameName = gameName,
        heroImageUrl = heroImageUrl,
        isRequestingToEndPlay = isRequestingToEndPlay,
        fabColor = fabColor,
        usedColors = usedColors,
        autoPosition = if (!shouldCustomSortPlayers) autoPosition else LogPlayerActivity.INVALID_POSITION,
    )

    private fun cancelPlayingNotification() {
        cancelNotification(TAG_PLAY_TIMER, internalId)
    }

    private fun showLocation() = isUserShowingLocation || preferences().showLogPlayLocation() || location.isNotEmpty()

    private fun showLength() = startTime > 0L || length > 0 || isUserShowingLength || preferences().showLogPlayLength()

    private fun showQuantity() = quantity != 1 || isUserShowingQuantity || preferences().showLogPlayQuantity()

    private fun showIncomplete() = incomplete || isUserShowingIncomplete || preferences().showLogPlayIncomplete()

    private fun showNoWinStats() = noWinStats || isUserShowingNoWinStats || preferences().showLogPlayNoWinStats()

    private fun showComments() = true

    private fun showPlayers() = isUserShowingPlayers || preferences().showLogPlayPlayerList() || players.isNotEmpty()

    class PlayerCallback : ListUpdateCallback {
        private var adapter: RecyclerView.Adapter<*>? = null

        fun bind(adapter: RecyclerView.Adapter<*>) {
            this.adapter = adapter
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter?.notifyItemRangeChanged(position, count, payload)
        }

        override fun onInserted(position: Int, count: Int) {
            adapter?.notifyItemRangeInserted(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter?.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter?.notifyItemRangeRemoved(position, count)
        }
    }

    inner class PlayerAdapter : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {
        var isDragging = false

        private var players = emptyList<PlayPlayer>()
        private val callback = PlayerCallback()
        var shouldCustomSortPlayers = false
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private inner class Diff(private val oldList: List<PlayPlayer>, private val newList: List<PlayPlayer>) : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size

            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].uiId == newList[newItemPosition].uiId &&
                    oldList[oldItemPosition].startingPosition == newList[newItemPosition].startingPosition
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return if (isDragging) {
                    areItemsTheSame(oldItemPosition, newItemPosition)
                } else {
                    oldList[oldItemPosition] == newList[newItemPosition]
                }
            }
        }

        fun submit(players: List<PlayPlayer>) {
            val oldPlayers = this.players
            this.players = players
            val diffResult = DiffUtil.calculateDiff(Diff(oldPlayers, this.players))
            diffResult.dispatchUpdatesTo(callback)
        }

        init {
            setHasStableIds(true)
            callback.bind(this)
        }

        override fun getItemCount(): Int = players.size

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.uiId?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            return PlayerViewHolder(parent.inflate(R.layout.row_logplay_player))
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.bind(position)
        }

        fun getPlayer(position: Int): PlayPlayer? = players.getOrNull(position)

        inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val binding = RowLogplayPlayerBinding.bind(itemView)
            private val ratingFormat = DecimalFormat("0.0######")

            private val nameColor: Int
                get() = binding.usernameView.textColors.defaultColor

            fun onItemDragging() {
                isDragging = true
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_blue_transparent))
            }

            @SuppressLint("NotifyDataSetChanged")
            fun onItemClear() {
                isDragging = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            @SuppressLint("ClickableViewAccessibility")
            fun bind(position: Int) {
                binding.dragHandle.visibility = if (!shouldCustomSortPlayers) View.VISIBLE else View.GONE

                val player = getPlayer(position) ?: PlayPlayer()
                val playerName = player.name.trim()
                val playerUsername = player.username.trim()

                binding.seatView.text = player.startingPosition
                if (playerName.isEmpty() && playerUsername.isEmpty()) {
                    val name = if (player.seat == PlayPlayer.SEAT_UNKNOWN) {
                        resources.getString(R.string.title_player)
                    } else {
                        resources.getString(R.string.generic_player, player.seat)
                    }
                    binding.nameView.setTextWithStyle(name, player.isNew, player.isWin, nameColor)
                    binding.usernameView.visibility = View.GONE
                } else if (playerName.isEmpty()) {
                    binding.nameView.setTextWithStyle(playerUsername, player.isNew, player.isWin, nameColor)
                    binding.usernameView.visibility = View.GONE
                } else {
                    binding.nameView.setTextWithStyle(playerName, player.isNew, player.isWin, nameColor)
                    binding.usernameView.setTextWithStyle(playerUsername, player.isNew, player.isWin)
                }
                binding.nameContainer.setOnClickListener { editPlayer(position) }

                val scoreDescription = player.numericScore?.asScore(itemView.context) ?: player.score
                binding.scoreView.setTextWithStyle(scoreDescription, false, player.isWin, nameColor)
                binding.scoreButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                binding.scoreButton.setOnClickListener { showScoreDialog(position) }

                binding.ratingButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.button_under_text), PorterDuff.Mode.SRC_IN)
                if (player.rating == 0.0) {
                    binding.ratingView.visibility = View.GONE
                } else {
                    binding.ratingView.setTextOrHide(player.rating.asBoundedRating(itemView.context, format = ratingFormat))
                }
                binding.ratingButton.setOnClickListener { showRatingDialog(position) }

                val color = player.color.asColorRgb()
                binding.colorView.setColorViewValue(color)
                binding.teamColorView.setTextOrHide(player.color)
                binding.teamColorView.visibility = if (color == Color.TRANSPARENT && player.color.isNotBlank()) View.VISIBLE else View.GONE
                binding.colorView.setOnClickListener { showColorDialog(position) }

                if (player.seat == PlayPlayer.SEAT_UNKNOWN) {
                    binding.seatView.visibility = View.GONE
                    binding.startingPositionView.setTextOrHide(player.startingPosition)
                } else {
                    binding.seatView.setTextColor(color.getTextColor())
                    binding.seatView.setTextOrHide(player.startingPosition)
                    binding.startingPositionView.visibility = View.GONE
                }

                binding.moreButton.setOnClickListener {
                    players.getOrNull(position)?.let { current ->
                        PopupMenu(this@LogPlayActivity, binding.moreButton).apply {
                            inflate(R.menu.log_play_player)
                            menu.findItem(R.id.win)?.isChecked = current.isWin
                            menu.findItem(R.id.new_)?.isChecked = current.isNew
                            setOnMenuItemClickListener { item: MenuItem ->
                                when (item.itemId) {
                                    R.id.win -> {
                                        viewModel.win(!item.isChecked, position)
                                        true
                                    }

                                    R.id.new_ -> {
                                        viewModel.new(!item.isChecked, position)
                                        true
                                    }

                                    else -> false
                                }
                            }
                            show()
                        }
                    }
                }

                binding.dragHandle.setOnTouchListener { _: View?, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper?.startDrag(this@PlayerViewHolder)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun logPlayerOrder(order: String) {
        firebaseAnalytics.logEvent("LogPlayPlayerOrder") {
            param("Order", order)
        }
    }

    companion object {
        private const val KEY_ID = "ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_END_PLAY = "END_PLAY"
        private const val KEY_REMATCH = "REMATCH"
        private const val KEY_CHANGE_GAME = "CHANGE_GAME"
        private const val KEY_INTERNAL_ID = "INTERNAL_ID"
        private const val KEY_IS_USER_SHOWING_LOCATION = "IS_USER_SHOWING_LOCATION"
        private const val KEY_IS_USER_SHOWING_LENGTH = "IS_USER_SHOWING_LENGTH"
        private const val KEY_IS_USER_SHOWING_QUANTITY = "IS_USER_SHOWING_QUANTITY"
        private const val KEY_IS_USER_SHOWING_INCOMPLETE = "IS_USER_SHOWING_INCOMPLETE"
        private const val KEY_IS_USER_SHOWING_NO_WIN_STATS = "IS_USER_SHOWING_NO_WIN_STATS"
        private const val KEY_IS_USER_SHOWING_COMMENTS = "IS_USER_SHOWING_COMMENTS"
        private const val KEY_IS_USER_SHOWING_PLAYERS = "IS_USER_SHOWING_PLAYERS"

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
            context.startActivity(createIntent(context, internalId, gameId, gameName, heroImageUrl, false).also {
                it.putExtra(KEY_END_PLAY, true)
            })
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
            context.startActivity(createIntent(context, internalId, gameId, gameName, heroImageUrl, false).also {
                it.putExtra(KEY_CHANGE_GAME, true)
            })
        }

        fun createRematchIntent(
            context: Context,
            internalId: Long,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            customPlayerSort: Boolean,
        ): Intent {
            return createIntent(context, internalId, gameId, gameName, heroImageUrl, customPlayerSort).also {
                it.putExtra(KEY_REMATCH, true)
            }
        }

        private fun createIntent(
            context: Context,
            internalId: Long,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            customPlayerSort: Boolean,
        ): Intent {
            return context.intentFor<LogPlayActivity>(
                KEY_ID to internalId,
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_HERO_IMAGE_URL to heroImageUrl,
                KEY_CUSTOM_PLAYER_SORT to customPlayerSort,
            )
        }
    }
}
