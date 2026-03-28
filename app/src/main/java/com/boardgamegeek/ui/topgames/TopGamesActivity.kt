package com.boardgamegeek.ui.topgames

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopGamesActivity : ComponentActivity() {

    private val viewModel: TopGamesViewModel by viewModels()

    private val activityScreenRoute: String = BottomNavItem.TopGames.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TopGamesRouteScreen()
        }
    }
}

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
