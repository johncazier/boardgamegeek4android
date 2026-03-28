package com.boardgamegeek.ui.gamecolors

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class GameColorsActivity : ComponentActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""

    @ColorInt
    private var iconColor: Int = Color.TRANSPARENT

    private val viewModel by viewModels<GameColorsViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameColors")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        viewModel.setGameId(gameId)

        setContent {
            val colors by viewModel.colors.collectAsStateWithLifecycle()
            val selectedColors = remember { mutableStateListOf<String>() }
            val selectionMode = selectedColors.isNotEmpty()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            var showMenu by remember { mutableStateOf(false) }
            var showAddDialog by remember { mutableStateOf(false) }

            AppTheme {
                Scaffold(
                    topBar = {
                        if (selectionMode) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = resources.getQuantityString(
                                            R.plurals.msg_colors_selected,
                                            selectedColors.size,
                                            selectedColors.size
                                        )
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { selectedColors.clear() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cancel)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            val count = selectedColors.size
                                            selectedColors.forEach { viewModel.removeColor(it) }
                                            selectedColors.clear()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    resources.getQuantityString(R.plurals.msg_colors_deleted, count, count)
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.menu_delete)
                                        )
                                    }
                                }
                            )
                        } else {
                            TopAppBar(
                                title = { Text(text = stringResource(R.string.title_favorite_colors)) },
                                navigationIcon = {
                                    IconButton(onClick = ::finish) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.menu_back)
                                        )
                                    }
                                },
                                actions = {
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
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_colors_generate)) },
                                            onClick = {
                                                viewModel.computeColors()
                                                val message = getString(R.string.msg_colors_generated)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (!selectionMode) {
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = androidx.compose.ui.graphics.Color(iconColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.title_add_color)
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    GameColorsScreen(
                        colors = colors,
                        selectedColors = selectedColors,
                        selectionMode = selectionMode,
                        onColorDelete = { color ->
                            viewModel.removeColor(color)
                            scope.launch {
                                showUndoSnackbar(
                                    snackbarHostState = snackbarHostState,
                                    message = getString(R.string.msg_color_deleted, color),
                                    actionLabel = getString(R.string.undo),
                                    onUndo = { viewModel.addColor(color) }
                                )
                            }
                        },
                        paddingValues = paddingValues
                    )
                }

                if (showAddDialog) {
                    AddColorDialog(
                        onDismiss = { showAddDialog = false },
                        onConfirm = { colorName ->
                            viewModel.addColor(colorName)
                            showAddDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun readIntent() {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun start(context: Context, gameId: Int, gameName: String, @ColorInt iconColor: Int) {
            context.startActivity<GameColorsActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_ICON_COLOR to iconColor
            )
        }
    }
}
