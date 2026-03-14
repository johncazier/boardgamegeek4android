package com.boardgamegeek.ui.buddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.ui.PlayerColorsActivity
import com.boardgamegeek.ui.buddycollection.BuddyCollectionActivity
import com.boardgamegeek.ui.plays.BuddyPlaysActivity
import com.boardgamegeek.ui.plays.PlayerPlaysActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class BuddyActivity : ComponentActivity() {
    private var name: String? = null
    private var username: String? = null

    private val viewModel by viewModels<BuddyViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()
        if (name.isNullOrBlank() && username.isNullOrBlank()) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Buddy")
                param(FirebaseAnalytics.Param.ITEM_ID, username.orEmpty())
                param(FirebaseAnalytics.Param.ITEM_NAME, name.orEmpty())
            }
        }

        if (!username.isNullOrBlank()) {
            viewModel.setUsername(username)
        } else {
            viewModel.setPlayerName(name)
        }

        setContent {
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
                                IconButton(onClick = ::finish) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                if (!currentUsername.isNullOrBlank()) {
                                    IconButton(onClick = { linkToBgg("user/$currentUsername") }) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = stringResource(R.string.menu_view_in_browser)
                                        )
                                    }
                                }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (currentUsername.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_add_username)) },
                                            onClick = {
                                                showAddUsernameDialog = true
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                            BuddyCollectionActivity.start(this, currentUsername)
                        },
                        onOpenPlays = {
                            if (currentUsername.isNullOrBlank()) {
                                PlayerPlaysActivity.start(this, currentPlayerName)
                            } else {
                                BuddyPlaysActivity.start(this, currentUsername)
                            }
                        },
                        onOpenColors = {
                            PlayerColorsActivity.start(this, currentUsername, currentPlayerName)
                        },
                        paddingValues = paddingValues
                    )
                }

                if (showNicknameDialog) {
                    EditBuddyNicknameDialog(
                        initialNickname = currentPlayerName,
                        onDismiss = { showNicknameDialog = false },
                        onConfirm = { nickName, updatePlays ->
                            viewModel.updateNickName(nickName, updatePlays)
                            showNicknameDialog = false
                        }
                    )
                }

                if (showRenamePlayerDialog) {
                    RenamePlayerDialog(
                        initialName = currentPlayerName,
                        onDismiss = { showRenamePlayerDialog = false },
                        onConfirm = { newName ->
                            viewModel.renamePlayer(newName)
                            showRenamePlayerDialog = false
                        }
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
                        }
                    )
                }
            }
        }
    }

    private fun readIntent() {
        name = intent.getStringExtra(KEY_PLAYER_NAME)
        username = intent.getStringExtra(KEY_USERNAME)
    }

    companion object {
        private const val KEY_USERNAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, username: String?, playerName: String?) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it)
            }
        }

        fun startUp(context: Context, username: String?, playerName: String? = null) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it.clearTop())
            }
        }

        fun createIntent(context: Context, username: String?, playerName: String?): Intent? {
            if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
                Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name")
                return null
            }
            return context.intentFor<BuddyActivity>(
                KEY_USERNAME to username,
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}
