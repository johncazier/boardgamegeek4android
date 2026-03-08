package com.boardgamegeek.ui.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG_PLAY_TIMER
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.cancelNotification
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.launchPlayingNotification
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.logplay.LogPlayActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.ui.play.PlayViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayActivity : ComponentActivity() {
    private var internalId = BggContract.INVALID_ID.toLong()
    private val viewModel by viewModels<PlayViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    private var hasBeenNotified = false
    private var currentPlay: Play? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasBeenNotified = savedInstanceState?.getBoolean(KEY_HAS_BEEN_NOTIFIED) ?: false
        readIntent()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                param(FirebaseAnalytics.Param.ITEM_ID, internalId.toString())
            }
        }

        if (internalId == BggContract.INVALID_ID.toLong()) {
            finish()
            return
        }

        viewModel.setId(internalId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.play.collect { play ->
                        currentPlay = play
                        maybeShowNotification(play)
                    }
                }
            }
        }

        setContent {
            AppTheme {
                val play by viewModel.play.collectAsStateWithLifecycle()
                PlayScaffold(
                    title = play?.gameName ?: stringResource(R.string.title_play),
                    onBack = { finish() },
                    menu = {
                        play?.let { currentPlay ->
                            IconButton(
                                onClick = {
                                    logDataManipulationAction("Edit", currentPlay)
                                    LogPlayActivity.editPlay(
                                        this,
                                        currentPlay.internalId,
                                        currentPlay.gameId,
                                        currentPlay.gameName,
                                        currentPlay.robustHeroImageUrl,
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.menu_edit)
                                )
                            }
                        }

                        PlayOverflowMenu(
                            play = play,
                            onDiscard = {
                                createDiscardDialog(R.string.play, isNew = true, finishActivity = false) {
                                    logDataManipulationAction("Discard", play)
                                    viewModel.discard()
                                }.show()
                            },
                            onSend = { targetPlay ->
                                logDataManipulationAction("Send", targetPlay)
                                viewModel.send()
                            },
                            onDelete = { targetPlay ->
                                createThemedBuilder()
                                    .setMessage(R.string.are_you_sure_delete_play)
                                    .setPositiveButton(R.string.delete) { _, _ ->
                                        if (targetPlay.hasStarted()) {
                                            cancelNotification(TAG_PLAY_TIMER, targetPlay.internalId)
                                        }
                                        logDataManipulationAction("Delete", targetPlay)
                                        viewModel.delete()
                                        finish()
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .setCancelable(true)
                                    .show()
                            },
                            onRematch = { targetPlay ->
                                logDataManipulationAction("Rematch", targetPlay)
                                LogPlayActivity.rematch(
                                    this,
                                    targetPlay.internalId,
                                    targetPlay.gameId,
                                    targetPlay.gameName,
                                    targetPlay.robustHeroImageUrl,
                                    targetPlay.arePlayersCustomSorted(),
                                )
                                finish()
                            },
                            onChangeGame = { targetPlay ->
                                logDataManipulationAction("ChangeGame", targetPlay)
                                com.boardgamegeek.ui.collection.CollectionActivity.startForGameChange(this, targetPlay.internalId)
                                finish()
                            },
                            onShare = { targetPlay ->
                                sharePlay(targetPlay)
                            },
                        )
                    },
                ) { paddingValues ->
                    PlayScreen(
                        viewModel = viewModel,
                        contentPadding = paddingValues,
                        onThumbnailClicked = { targetPlay ->
                            com.boardgamegeek.ui.game.GameActivity.start(this, targetPlay.gameId, targetPlay.gameName)
                        },
                        onEndTimerClicked = { targetPlay ->
                            LogPlayActivity.endPlay(
                                this,
                                targetPlay.internalId,
                                targetPlay.gameId,
                                targetPlay.gameName,
                                targetPlay.robustHeroImageUrl,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_BEEN_NOTIFIED, hasBeenNotified)
    }

    private fun readIntent() {
        internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID.toLong())
    }

    private fun maybeShowNotification(play: Play?) {
        play?.let {
            if (it.hasStarted() && !hasBeenNotified) {
                launchPlayingNotification(
                    it.internalId,
                    it.gameName,
                    it.location,
                    it.playerCount,
                    it.startTime,
                    it.heroImageUrl,
                )
                hasBeenNotified = true
            }
        }
    }

    private fun sharePlay(play: Play) {
        val subject = getString(R.string.play_description_game_segment, play.gameName) + getString(
            R.string.play_description_date_segment,
            play.dateInMillis.formatDateTime(this)
        )
        val sb = StringBuilder()
        sb.append(getString(R.string.play_description_game_segment, play.gameName))
        if (play.dateInMillis != Play.UNKNOWN_DATE) {
            sb.append(
                getString(
                    R.string.play_description_date_segment,
                    play.dateInMillis.formatDateTime(
                        this,
                        DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
                    )
                )
            )
        }
        if (play.quantity > 1) {
            sb.append(resources.getQuantityString(R.plurals.play_description_quantity_segment, play.quantity, play.quantity))
        }
        if (play.location.isNotBlank()) sb.append(getString(R.string.play_description_location_segment, play.location))
        if (play.length > 0) sb.append(getString(R.string.play_description_length_segment, play.length.asTime()))
        if (play.players.isNotEmpty()) {
            sb.append(" ").append(getString(R.string.with))
            if (play.arePlayersCustomSorted()) {
                for (player in play.players) {
                    sb.append("\n").append(describePlayer(player))
                }
            } else {
                for (i in play.sortedPlayers.indices) {
                    play.getPlayerAtSeat(i + 1)?.let { player ->
                        sb.append("\n").append(describePlayer(player))
                    }
                }
            }
        }
        if (play.comments.isNotBlank()) {
            sb.append("\n\n").append(play.comments)
        }
        if (play.playId > 0) {
            sb.append("\n\n").append(getString(R.string.play_description_play_url_segment, play.playId.toString()).trim())
        } else {
            sb.append("\n\n").append(getString(R.string.play_description_game_url_segment, play.gameId.toString()).trim())
        }

        share(subject, sb.toString(), R.string.share_play_title)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param(FirebaseAnalytics.Param.ITEM_ID, play.playId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, subject)
        }
    }

    private fun describePlayer(player: PlayPlayer): String {
        val sb = StringBuilder()
        if (player.seat != PlayPlayer.SEAT_UNKNOWN) sb.append(getString(R.string.player_description_starting_position_segment, player.seat))
        sb.append(player.name)
        if (player.username.isNotEmpty()) sb.append(getString(R.string.player_description_username_segment, player.username))
        if (player.isNew) sb.append(getString(R.string.player_description_new_segment))
        if (player.color.isNotBlank()) sb.append(getString(R.string.player_description_color_segment, player.color))
        if (player.score.isNotBlank()) sb.append(getString(R.string.player_description_score_segment, player.score))
        if (player.isWin) sb.append(getString(R.string.player_description_win_segment))
        return sb.toString()
    }

    private fun logDataManipulationAction(action: String, play: Play?) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", action)
            param("GameName", play?.gameName.orEmpty())
        }
    }

    companion object {
        private const val KEY_ID = "ID"
        private const val KEY_HAS_BEEN_NOTIFIED = "HAS_BEEN_NOTIFIED"

        fun start(context: Context, internalId: Long) {
            context.startActivity(createIntent(context, internalId))
        }

        fun createIntent(context: Context, internalId: Long): Intent {
            return context.intentFor<PlayActivity>(KEY_ID to internalId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayScaffold(
    title: String,
    onBack: () -> Unit,
    menu: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                actions = { menu() },
            )
        },
        content = content,
    )
}

@Composable
private fun PlayOverflowMenu(
    play: Play?,
    onDiscard: () -> Unit,
    onSend: (Play) -> Unit,
    onDelete: (Play) -> Unit,
    onRematch: (Play) -> Unit,
    onChangeGame: (Play) -> Unit,
    onShare: (Play) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canDiscard = (play?.playId ?: INVALID_ID) != INVALID_ID && (play?.dirtyTimestamp ?: 0L) > 0L
    val canSend = (play?.dirtyTimestamp ?: 0L) > 0L

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (canDiscard) {
            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_discard_changes)) },
                onClick = {
                    expanded = false
                    onDiscard()
                }
            )
        }

        play?.let { currentPlay ->
            if (canSend) {
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(stringResource(R.string.send)) },
                    onClick = {
                        expanded = false
                        onSend(currentPlay)
                    }
                )
            }

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_delete)) },
                onClick = {
                    expanded = false
                    onDelete(currentPlay)
                }
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.rematch)) },
                onClick = {
                    expanded = false
                    onRematch(currentPlay)
                }
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_change_game)) },
                onClick = {
                    expanded = false
                    onChangeGame(currentPlay)
                }
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_share)) },
                onClick = {
                    expanded = false
                    onShare(currentPlay)
                }
            )
        }
    }
}
