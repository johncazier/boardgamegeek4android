package com.boardgamegeek.ui.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.ui.game.GameViewModel
import com.boardgamegeek.ui.game.GameViewModel.ProducerType
import com.boardgamegeek.ui.navigation.GameDetailRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailRouteScreen(
    route: GameDetailRoute,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val type = remember(route.producerType) {
        ProducerType.entries.firstOrNull { it.name == route.producerType } ?: ProducerType.UNKNOWN
    }
    val sort by viewModel.producerSort.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(route.gameId, route.producerType) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameDetail${route.title}")
            param(FirebaseAnalytics.Param.ITEM_ID, route.gameId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.gameName)
        }
        viewModel.setId(route.gameId)
        viewModel.setProducerType(type)
        when (type) {
            ProducerType.DESIGNER -> viewModel.refreshDesignerImages()
            ProducerType.ARTIST -> viewModel.refreshArtistImages()
            ProducerType.PUBLISHER -> viewModel.refreshPublisherImages()
            ProducerType.EXPANSION -> viewModel.refreshMissingExpansionDetails()
            else -> Unit
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { GameDetailTitle(route.gameName, route.title) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        if (type == ProducerType.EXPANSION) {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.menu_sort),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_name)) },
                                        onClick = {
                                            viewModel.setProducerSort(GameViewModel.ProducerSort.NAME)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sort == GameViewModel.ProducerSort.NAME) Text("\u2713")
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.collection_sort_average_rating)) },
                                        onClick = {
                                            viewModel.setProducerSort(GameViewModel.ProducerSort.RATING)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sort == GameViewModel.ProducerSort.RATING) Text("\u2713")
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            GameDetailScreen(viewModel = viewModel, paddingValues = paddingValues)
        }
    }
}

@Composable
private fun GameDetailTitle(title: String, subtitle: String) {
    Column {
        Text(text = title)
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
