package com.boardgamegeek.ui.mechanic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.game.GameLauncher
import com.boardgamegeek.ui.linkedcollection.LinkedCollectionScreen
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.MechanicRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicRouteScreen(
    route: MechanicRoute,
    viewModel: MechanicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(route.mechanicId) {
        viewModel.setId(route.mechanicId)
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = route.mechanicName)
                            Text(
                                text = stringResource(R.string.title_mechanic),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
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
                        Box {
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
                                    text = { Text(stringResource(R.string.menu_sort_name)) },
                                    onClick = {
                                        viewModel.setSort(CollectionItem.SortType.NAME)
                                        showMenu = false
                                    },
                                    trailingIcon = {
                                        if (sort == CollectionItem.SortType.NAME) {
                                            Text("\u2713")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_sort_rating)) },
                                    onClick = {
                                        viewModel.setSort(CollectionItem.SortType.RATING)
                                        showMenu = false
                                    },
                                    trailingIcon = {
                                        if (sort == CollectionItem.SortType.RATING) {
                                            Text("\u2713")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_refresh)) },
                                    onClick = {
                                        viewModel.reload()
                                        showMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_view)) },
                                    onClick = {
                                        context.linkToBgg("boardgamemechanic", route.mechanicId)
                                        showMenu = false
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            LinkedCollectionScreen(
                collection = collection,
                emptyMessage = stringResource(
                    R.string.empty_linked_collection,
                    stringResource(R.string.title_mechanic).lowercase(Locale.getDefault()),
                ),
                isRefreshing = isRefreshing,
                onRefresh = viewModel::reload,
                onItemClick = { item ->
                    GameLauncher.start(
                        context,
                        item.gameId,
                        item.gameName,
                        item.gameThumbnailUrl,
                        item.gameHeroImageUrl,
                    )
                },
                paddingValues = paddingValues,
            )
        }
    }
}
