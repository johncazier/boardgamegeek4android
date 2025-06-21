package com.boardgamegeek.ui.hotness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.shareGame
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class HotnessActivity : ComponentActivity() {

    private val viewModel: HotnessViewModel by viewModels()

    private var currentScreenRoute: String = BottomNavItem.Hotness.route

    private fun navigateToScreen(route: String) {
        if (route == currentScreenRoute && this::class.java.simpleName.startsWith(route.capitalize(Locale.getDefault()))) {
            // Already on this screen, do nothing or maybe refresh
            return
        }

        when (route) {
            BottomNavItem.Collection.route -> startActivity<CollectionActivity>() // Or CollectionDetailsActivity
            BottomNavItem.Hotness.route -> {
                // Check if already HotnessActivity to prevent re-launching itself
                if (this::class.java != HotnessActivity::class.java) {
                    startActivity<HotnessActivity>()
                }
            }
            BottomNavItem.TopGames.route -> startActivity<TopGamesActivity>()
            BottomNavItem.GeekLists.route -> startActivity<GeekListsActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppScreen(
                topBarTitle = stringResource(id = R.string.title_hotness),
                initialSelectedRoute = currentScreenRoute,
                onNavigate = { route ->
                    navigateToScreen(route)
                },
                onSearchClick = {
                    startActivity<SearchResultsActivity>()
                }
            ) { paddingValues ->
                HotnessScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    onGameClick = { gameId, gameName, thumbnailUrl ->
                        GameActivity.Companion.start(this, gameId, gameName, thumbnailUrl ?: "")
                    },
                    onLogPlayForm = { gameId, gameName, thumbnailUrl ->
                        LogPlayActivity.Companion.logPlay(this, gameId, gameName, thumbnailUrl ?: "")
                    },
                    onLogPlayWizard = { gameId, gameName ->
                        NewPlayActivity.Companion.start(this, gameId, gameName)
                    },
                    onComposeLogPlay = { gameId, gameName, thumbnailUrl ->
                        ComposeLogPlayActivity.Companion.start(this, gameId, gameName, thumbnailUrl ?: "")
                    },
                    onShareGame = { gameId, gameName, shareMethod ->
                        this.shareGame(gameId, gameName, shareMethod)
                    },
                    onShareGames = { gamesToShare, shareMethod ->
                        this.shareGames(gamesToShare, shareMethod)
                    },
                    onLinkBgg = { gameId ->
                        this.linkBgg(gameId)
                    }
                )
            }
        }
    }
}