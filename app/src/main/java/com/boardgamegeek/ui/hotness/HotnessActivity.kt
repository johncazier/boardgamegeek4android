package com.boardgamegeek.ui.hotness

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
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.BottomNavItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HotnessActivity : ComponentActivity() {

    private val viewModel: HotnessViewModel by viewModels()

    private val activityScreenRoute: String = BottomNavItem.Hotness.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HotnessRouteScreen()
        }
    }
}

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
