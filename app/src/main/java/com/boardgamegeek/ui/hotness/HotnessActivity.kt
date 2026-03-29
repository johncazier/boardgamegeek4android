package com.boardgamegeek.ui.hotness

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.GameRoute

@Composable
fun HotnessRouteScreen(
    viewModel: HotnessViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    AppScreen(
        topBarTitle = stringResource(id = R.string.title_hotness),
        currentScreenRouteFromActivity = BottomNavItem.Hotness.route,
    ) { paddingValues ->
        HotnessScreen(
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
