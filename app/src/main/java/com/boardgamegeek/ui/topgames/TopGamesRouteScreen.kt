package com.boardgamegeek.ui.topgames

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator

@Composable
fun TopGamesRouteScreen(
    viewModel: TopGamesViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    AppScreen(
        topBarTitle = stringResource(id = R.string.title_top_games),
        currentScreenRouteFromActivity = BottomNavItem.TopGames.route,
    ) { paddingValues ->
        TopGamesScreen(
            viewModel = viewModel,
            paddingValues = paddingValues,
            onGameClick = { gameId, gameName, thumbnailUrl ->
                navigator.navigate(
                    GameRoute(
                        gameId = gameId,
                        gameName = gameName,
                        thumbnailUrl = thumbnailUrl.orEmpty(),
                        heroImageUrl = thumbnailUrl.orEmpty(),
                    ),
                )
            },
        )
    }
}
