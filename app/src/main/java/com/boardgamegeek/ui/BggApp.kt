package com.boardgamegeek.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.boardgamegeek.ui.buddies.BuddiesRouteScreen
import com.boardgamegeek.ui.collection.CollectionRouteScreen
import com.boardgamegeek.ui.collectiondetails.CollectionDetailsRouteScreen
import com.boardgamegeek.ui.data.DataRouteScreen
import com.boardgamegeek.ui.forums.ForumsRouteScreen
import com.boardgamegeek.ui.game.GameRouteScreen
import com.boardgamegeek.ui.geeklists.GeekListsRouteScreen
import com.boardgamegeek.ui.hotness.HotnessRouteScreen
import com.boardgamegeek.ui.login.LoginRouteScreen
import com.boardgamegeek.ui.navigation.AppRoute
import com.boardgamegeek.ui.navigation.BackStackAppNavigator
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.DataRoute
import com.boardgamegeek.ui.navigation.ForumsRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.GeekListsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.SettingsRoute
import com.boardgamegeek.ui.navigation.SyncRoute
import com.boardgamegeek.ui.navigation.TopGamesRoute
import com.boardgamegeek.ui.playssummary.PlaysSummaryRouteScreen
import com.boardgamegeek.ui.search.SearchRouteScreen
import com.boardgamegeek.ui.settings.SettingsRouteScreen
import com.boardgamegeek.ui.sync.SyncRouteScreen
import com.boardgamegeek.ui.topgames.TopGamesRouteScreen

@Composable
fun BggApp(
    initialRoute: AppRoute,
    pendingExternalRoute: AppRoute?,
    onExternalRouteConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(initialRoute)
    val navigator = remember(backStack) { BackStackAppNavigator(backStack) }
    val entries = entryProvider<NavKey> {
        entry<CollectionRoute> {
            CollectionRouteScreen()
        }
        entry<HotnessRoute> {
            HotnessRouteScreen()
        }
        entry<TopGamesRoute> {
            TopGamesRouteScreen()
        }
        entry<GeekListsRoute> {
            GeekListsRouteScreen()
        }
        entry<CollectionDetailsRoute> {
            CollectionDetailsRouteScreen()
        }
        entry<PlaysSummaryRoute> {
            PlaysSummaryRouteScreen()
        }
        entry<BuddiesRoute> {
            BuddiesRouteScreen()
        }
        entry<ForumsRoute> {
            ForumsRouteScreen()
        }
        entry<SyncRoute> {
            SyncRouteScreen()
        }
        entry<DataRoute> {
            DataRouteScreen()
        }
        entry<SettingsRoute> {
            SettingsRouteScreen()
        }
        entry<LoginRoute> {
            LoginRouteScreen(initialUsername = it.username)
        }
        entry<SearchRoute> { route ->
            SearchRouteScreen(
                initialQuery = route.query,
                onBack = { navigator.popBackStack() },
                onQueryChange = {},
            )
        }
        entry<GameRoute> {
            GameRouteScreen(route = it)
        }
    }

    LaunchedEffect(pendingExternalRoute) {
        pendingExternalRoute?.let {
            navigator.navigate(it)
            onExternalRouteConsumed()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppNavigator provides navigator,
    ) {
        NavDisplay(
            backStack = backStack,
            entryProvider = entries,
            onBack = { navigator.popBackStack() },
        )
    }
}

@Composable
private fun PlaceholderRouteScreen(label: String) {
    Text(label)
}
