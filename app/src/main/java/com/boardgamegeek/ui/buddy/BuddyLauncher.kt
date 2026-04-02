package com.boardgamegeek.ui.buddy

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTask
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.buddycollection.BuddyCollectionLauncher
import com.boardgamegeek.ui.navigation.BuddyRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlayerColorsRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.playercolors.PlayerColorsLauncher
import com.boardgamegeek.ui.plays.BuddyPlaysLauncher
import com.boardgamegeek.ui.plays.PlayerPlaysLauncher
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

object BuddyLauncher {
    fun start(context: Context, username: String?, playerName: String?) {
        createIntent(context, username, playerName)?.let(context::startActivity)
    }

    fun startUp(context: Context, username: String?, playerName: String? = null) {
        createIntent(context, username, playerName)?.let {
            context.startActivity(it.clearTask().clearTop())
        }
    }

    fun createIntent(context: Context, username: String?, playerName: String?): Intent? {
        if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
            Timber.w("Unable to create a BuddyLauncher intent - missing both a username and a player name")
            return null
        }
        return MainActivity.createIntent(
            context = context,
            route = BuddyRoute(
                username = username,
                playerName = playerName,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyRouteScreen(
    route: BuddyRoute,
    viewModel: BuddyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }

    LaunchedEffect(route.username, route.playerName) {
        val username = route.username
        val playerName = route.playerName
        if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
            navigator.popBackStackOrFinish(context)
            return@LaunchedEffect
        }

        if (!username.isNullOrBlank()) {
            viewModel.setUsername(username)
        } else {
            viewModel.setPlayerName(playerName)
        }

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Buddy")
            param(FirebaseAnalytics.Param.ITEM_ID, username.orEmpty())
            param(FirebaseAnalytics.Param.ITEM_NAME, playerName.orEmpty())
        }
    }

    val currentUsername by viewModel.username.collectAsStateWithLifecycle()
    val currentPlayerName by viewModel.playerName.collectAsStateWithLifecycle()
    val buddy by viewModel.buddy.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val isUsernameValid by viewModel.isUsernameValid.collectAsStateWithLifecycle()
    val isValidatingUsername by viewModel.validatingUsername.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showRenamePlayerDialog by remember { mutableStateOf(false) }
    var showAddUsernameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.updateMessage.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.title_buddy))
                            Text(text = currentUsername ?: currentPlayerName.orEmpty())
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        if (!currentUsername.isNullOrBlank()) {
                            IconButton(onClick = { context.linkToBgg("user/$currentUsername") }) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = stringResource(R.string.menu_view_in_browser),
                                )
                            }
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (currentUsername.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_add_username)) },
                                    onClick = {
                                        showAddUsernameDialog = true
                                        showMenu = false
                                    },
                                )
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BuddyScreen(
                buddy = buddy,
                player = player,
                colors = colors,
                isRefreshing = refreshing,
                canRefresh = !currentUsername.isNullOrBlank() && buddy != null,
                onRefresh = viewModel::refresh,
                onEditNickname = {
                    if (currentUsername.isNullOrBlank()) showRenamePlayerDialog = true
                    else showNicknameDialog = true
                },
                onOpenCollection = {
                    BuddyCollectionLauncher.start(context, currentUsername)
                },
                onOpenPlays = {
                    if (currentUsername.isNullOrBlank()) {
                        PlayerPlaysLauncher.start(context, currentPlayerName)
                    } else {
                        BuddyPlaysLauncher.start(context, currentUsername)
                    }
                },
                onOpenColors = {
                    PlayerColorsLauncher.start(context, currentUsername, currentPlayerName)
                },
                paddingValues = paddingValues,
            )
        }

        if (showNicknameDialog) {
            EditBuddyNicknameDialog(
                initialNickname = currentPlayerName,
                onDismiss = { showNicknameDialog = false },
                onConfirm = { nickName, updatePlays ->
                    viewModel.updateNickName(nickName, updatePlays)
                    showNicknameDialog = false
                },
            )
        }

        if (showRenamePlayerDialog) {
            RenamePlayerDialog(
                initialName = currentPlayerName,
                onDismiss = { showRenamePlayerDialog = false },
                onConfirm = { newName ->
                    viewModel.renamePlayer(newName)
                    showRenamePlayerDialog = false
                },
            )
        }

        if (showAddUsernameDialog) {
            AddUsernameDialog(
                isUsernameValid = isUsernameValid,
                isValidating = isValidatingUsername,
                onValidate = viewModel::validateUsername,
                onUsernameChanged = viewModel::clearUsernameValidation,
                onDismiss = {
                    viewModel.clearUsernameValidation()
                    showAddUsernameDialog = false
                },
                onConfirm = { newUsername ->
                    viewModel.addUsernameToPlayer(newUsername)
                    showAddUsernameDialog = false
                },
            )
        }
    }
}
