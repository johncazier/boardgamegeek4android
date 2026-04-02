package com.boardgamegeek.ui.gamedetail

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
