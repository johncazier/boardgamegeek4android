package com.boardgamegeek.ui.plays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlayerPlaysRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPlaysScaffold(
    subtitle: String,
    playCount: Int,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    androidx.compose.material3.Text(text = stringResource(R.string.title_plays) + if (subtitle.isNotBlank()) " - $subtitle" else "")
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.menu_back),
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.Text(text = playCount.toString())
                },
            )
        },
        content = content,
    )
}

@Composable
fun PlayerPlaysRouteScreen(
    route: PlayerPlaysRoute,
    viewModel: PlaysViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(route.playerName) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerPlays")
            param(FirebaseAnalytics.Param.ITEM_NAME, route.playerName)
        }
        viewModel.setPlayerName(route.playerName)
    }

    AppTheme {
        val plays by viewModel.plays.collectAsState()
        PlayerPlaysScaffold(
            subtitle = route.playerName,
            playCount = plays.sumOf { it.quantity },
            onBack = { navigator.popBackStackOrFinish(context) },
        ) { paddingValues ->
            PlaysScreen(
                viewModel = viewModel,
                emptyStringResId = R.string.empty_plays_player,
                showGameName = true,
                gameId = com.boardgamegeek.provider.BggContract.INVALID_ID,
                gameName = "",
                heroImageUrl = "",
                arePlayersCustomSorted = false,
                iconColor = android.graphics.Color.TRANSPARENT,
                contentPadding = paddingValues,
            )
        }
    }
}
