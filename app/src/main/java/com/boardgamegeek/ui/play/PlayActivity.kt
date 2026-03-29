package com.boardgamegeek.ui.play

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG_PLAY_TIMER
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.cancelNotification
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.launchPlayingNotification
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.logplay.LogPlayActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlayRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object PlayActivity {
    fun start(context: Context, internalId: Long) {
        context.startActivity(createIntent(context, internalId))
    }

    fun createIntent(context: Context, internalId: Long): Intent {
        return MainActivity.createIntent(
            context = context,
            route = PlayRoute(internalId = internalId),
        )
    }
}

@Composable
fun PlayRouteScreen(
    route: PlayRoute,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    var hasBeenNotified by rememberSaveable { mutableStateOf(false) }
    val play by viewModel.play.collectAsStateWithLifecycle()

    LaunchedEffect(route.internalId) {
        if (route.internalId == INVALID_ID.toLong()) {
            navigator.popBackStackOrFinish(context)
            return@LaunchedEffect
        }
        viewModel.setId(route.internalId)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param(FirebaseAnalytics.Param.ITEM_ID, route.internalId.toString())
        }
    }

    LaunchedEffect(play) {
        play?.let {
            if (it.hasStarted() && !hasBeenNotified) {
                context.launchPlayingNotification(
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

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppTheme {
        PlayScaffold(
            title = play?.gameName ?: stringResource(R.string.title_play),
            onBack = { navigator.popBackStackOrFinish(context) },
            menu = {
                play?.let { currentPlay ->
                    IconButton(
                        onClick = {
                            firebaseAnalytics.logEvent("DataManipulation") {
                                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                                param("Action", "Edit")
                                param("GameName", currentPlay.gameName)
                            }
                            LogPlayActivity.editPlay(
                                context,
                                currentPlay.internalId,
                                currentPlay.gameId,
                                currentPlay.gameName,
                                currentPlay.robustHeroImageUrl,
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.menu_edit),
                        )
                    }
                }

                PlayOverflowMenu(
                    play = play,
                    onDiscard = {
                        (context as? Activity)?.createDiscardDialog(R.string.play, isNew = true, finishActivity = false) {
                            firebaseAnalytics.logEvent("DataManipulation") {
                                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                                param("Action", "Discard")
                                param("GameName", play?.gameName.orEmpty())
                            }
                            viewModel.discard()
                        }?.show()
                    },
                    onSend = {
                        firebaseAnalytics.logEvent("DataManipulation") {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                            param("Action", "Send")
                            param("GameName", it.gameName)
                        }
                        viewModel.send()
                    },
                    onDelete = { targetPlay ->
                        context.createThemedBuilder()
                            .setMessage(R.string.are_you_sure_delete_play)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                if (targetPlay.hasStarted()) {
                                    context.cancelNotification(TAG_PLAY_TIMER, targetPlay.internalId)
                                }
                                firebaseAnalytics.logEvent("DataManipulation") {
                                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                                    param("Action", "Delete")
                                    param("GameName", targetPlay.gameName)
                                }
                                viewModel.delete()
                                navigator.popBackStackOrFinish(context)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(true)
                            .show()
                    },
                    onRematch = { targetPlay ->
                        firebaseAnalytics.logEvent("DataManipulation") {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                            param("Action", "Rematch")
                            param("GameName", targetPlay.gameName)
                        }
                        LogPlayActivity.rematch(
                            context,
                            targetPlay.internalId,
                            targetPlay.gameId,
                            targetPlay.gameName,
                            targetPlay.robustHeroImageUrl,
                            targetPlay.arePlayersCustomSorted(),
                        )
                        navigator.popBackStackOrFinish(context)
                    },
                    onChangeGame = { targetPlay ->
                        firebaseAnalytics.logEvent("DataManipulation") {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                            param("Action", "ChangeGame")
                            param("GameName", targetPlay.gameName)
                        }
                        com.boardgamegeek.ui.collection.CollectionActivity.startForGameChange(context, targetPlay.internalId)
                        navigator.popBackStackOrFinish(context)
                    },
                    onShare = { targetPlay ->
                        sharePlay(context, firebaseAnalytics, targetPlay)
                    },
                )
            },
        ) { paddingValues ->
            PlayScreen(
                viewModel = viewModel,
                contentPadding = paddingValues,
                onThumbnailClicked = { targetPlay ->
                    com.boardgamegeek.ui.game.GameActivity.start(context, targetPlay.gameId, targetPlay.gameName)
                },
                onEndTimerClicked = { targetPlay ->
                    LogPlayActivity.endPlay(
                        context,
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

private fun sharePlay(context: Context, firebaseAnalytics: FirebaseAnalytics, play: Play) {
    val subject = context.getString(R.string.play_description_game_segment, play.gameName) + context.getString(
        R.string.play_description_date_segment,
        play.dateInMillis.formatDateTime(context),
    )
    val sb = StringBuilder()
    sb.append(context.getString(R.string.play_description_game_segment, play.gameName))
    if (play.dateInMillis != Play.UNKNOWN_DATE) {
        sb.append(
            context.getString(
                R.string.play_description_date_segment,
                play.dateInMillis.formatDateTime(
                    context,
                    DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
                ),
            ),
        )
    }
    if (play.quantity > 1) {
        sb.append(context.resources.getQuantityString(R.plurals.play_description_quantity_segment, play.quantity, play.quantity))
    }
    if (play.location.isNotBlank()) sb.append(context.getString(R.string.play_description_location_segment, play.location))
    if (play.length > 0) sb.append(context.getString(R.string.play_description_length_segment, play.length.asTime()))
    if (play.players.isNotEmpty()) {
        sb.append(" ").append(context.getString(R.string.with))
        if (play.arePlayersCustomSorted()) {
            for (player in play.players) {
                sb.append("\n").append(describePlayer(context, player))
            }
        } else {
            for (i in play.sortedPlayers.indices) {
                play.getPlayerAtSeat(i + 1)?.let { player ->
                    sb.append("\n").append(describePlayer(context, player))
                }
            }
        }
    }
    if (play.comments.isNotBlank()) {
        sb.append("\n\n").append(play.comments)
    }
    if (play.playId > 0) {
        sb.append("\n\n").append(context.getString(R.string.play_description_play_url_segment, play.playId.toString()).trim())
    } else {
        sb.append("\n\n").append(context.getString(R.string.play_description_game_url_segment, play.gameId.toString()).trim())
    }

    (context as? Activity)?.share(subject, sb.toString(), R.string.share_play_title) ?: return
    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
        param(FirebaseAnalytics.Param.ITEM_ID, play.playId.toString())
        param(FirebaseAnalytics.Param.ITEM_NAME, subject)
    }
}

private fun describePlayer(context: Context, player: PlayPlayer): String {
    val sb = StringBuilder()
    if (player.seat != PlayPlayer.SEAT_UNKNOWN) sb.append(context.getString(R.string.player_description_starting_position_segment, player.seat))
    sb.append(player.name)
    if (player.username.isNotEmpty()) sb.append(context.getString(R.string.player_description_username_segment, player.username))
    if (player.isNew) sb.append(context.getString(R.string.player_description_new_segment))
    if (player.color.isNotBlank()) sb.append(context.getString(R.string.player_description_color_segment, player.color))
    if (player.score.isNotBlank()) sb.append(context.getString(R.string.player_description_score_segment, player.score))
    if (player.isWin) sb.append(context.getString(R.string.player_description_win_segment))
    return sb.toString()
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
                },
            )
        }

        play?.let { currentPlay ->
            if (canSend) {
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(stringResource(R.string.send)) },
                    onClick = {
                        expanded = false
                        onSend(currentPlay)
                    },
                )
            }

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_delete)) },
                onClick = {
                    expanded = false
                    onDelete(currentPlay)
                },
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.rematch)) },
                onClick = {
                    expanded = false
                    onRematch(currentPlay)
                },
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_change_game)) },
                onClick = {
                    expanded = false
                    onChangeGame(currentPlay)
                },
            )

            DropdownMenuItem(
                text = { androidx.compose.material3.Text(stringResource(R.string.menu_share)) },
                onClick = {
                    expanded = false
                    onShare(currentPlay)
                },
            )
        }
    }
}
