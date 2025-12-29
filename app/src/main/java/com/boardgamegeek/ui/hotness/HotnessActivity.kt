package com.boardgamegeek.ui.hotness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.shareGame
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.navigation.BottomNavItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HotnessActivity : ComponentActivity() {

    private val viewModel: HotnessViewModel by viewModels()

    private val activityScreenRoute: String = BottomNavItem.Hotness.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppScreen(topBarTitle = stringResource(id = R.string.title_hotness),
                currentScreenRouteFromActivity = activityScreenRoute, // Pass the Activity's route
                onSearchClick = {
                    // Search still uses the Activity's context if needed for startActivity
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                }
            ) { paddingValues ->
                HotnessScreen(
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