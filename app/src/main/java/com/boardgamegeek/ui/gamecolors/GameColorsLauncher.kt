package com.boardgamegeek.ui.gamecolors

import android.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.GameColorsRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameColorsRouteScreen(
    route: GameColorsRoute,
    viewModel: GameColorsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val selectedColors = remember { mutableStateListOf<String>() }
    val selectionMode = selectedColors.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(route.gameId, route.gameName) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameColors")
            param(FirebaseAnalytics.Param.ITEM_ID, route.gameId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.gameName)
        }
        viewModel.setGameId(route.gameId)
    }

    AppTheme {
        Scaffold(
            topBar = {
                if (selectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = context.resources.getQuantityString(
                                    R.plurals.msg_colors_selected,
                                    selectedColors.size,
                                    selectedColors.size,
                                ),
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedColors.clear() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val count = selectedColors.size
                                    selectedColors.forEach(viewModel::removeColor)
                                    selectedColors.clear()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.resources.getQuantityString(R.plurals.msg_colors_deleted, count, count),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.menu_delete),
                                )
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.title_favorite_colors)) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.menu_back),
                                )
                            }
                        },
                        actions = {
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
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_colors_generate)) },
                                    onClick = {
                                        viewModel.computeColors()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.msg_colors_generated))
                                        }
                                        showMenu = false
                                    },
                                )
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (!selectionMode) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = ComposeColor(route.iconColor.takeUnless { it == 0 } ?: Color.TRANSPARENT),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.title_add_color),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            message = context.getString(R.string.msg_color_deleted, color),
                            actionLabel = context.getString(R.string.undo),
                            onUndo = { viewModel.addColor(color) },
                        )
                    }
                },
                paddingValues = paddingValues,
            )
        }

        if (showAddDialog) {
            AddColorDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { colorName ->
                    viewModel.addColor(colorName)
                    showAddDialog = false
                },
            )
        }
    }
}
