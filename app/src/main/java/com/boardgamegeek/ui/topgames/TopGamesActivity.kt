package com.boardgamegeek.ui.topgames

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.topgames.TopGamesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopGamesActivity : ComponentActivity() {

    private val viewModel: TopGamesViewModel by viewModels()

    private val activityScreenRoute: String = BottomNavItem.TopGames.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppScreen(
                topBarTitle = stringResource(id = R.string.title_top_games),
                currentScreenRouteFromActivity = activityScreenRoute,
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                }
            ) { paddingValues ->
                TopGamesScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    onGameClick = { gameId, gameName, thumbnailUrl ->
                        GameActivity.Companion.start(this, gameId, gameName, thumbnailUrl ?: "")
                    }
                )
            }
        }
    }
}
