package com.boardgamegeek.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.boardgamegeek.ui.article.ArticleRouteScreen
import com.boardgamegeek.ui.artists.ArtistsRouteScreen
import com.boardgamegeek.ui.buddy.BuddyRouteScreen
import com.boardgamegeek.ui.buddycollection.BuddyCollectionRouteScreen
import com.boardgamegeek.ui.buddies.BuddiesRouteScreen
import com.boardgamegeek.ui.categories.CategoriesRouteScreen
import com.boardgamegeek.ui.collection.CollectionRouteScreen
import com.boardgamegeek.ui.collectiondetails.CollectionDetailsRouteScreen
import com.boardgamegeek.ui.comments.CommentsRouteScreen
import com.boardgamegeek.ui.data.DataRouteScreen
import com.boardgamegeek.ui.designers.DesignersRouteScreen
import com.boardgamegeek.ui.forum.ForumRouteScreen
import com.boardgamegeek.ui.forums.ForumsRouteScreen
import com.boardgamegeek.ui.game.GameRouteScreen
import com.boardgamegeek.ui.gamecollectionitem.GameCollectionItemRouteScreen
import com.boardgamegeek.ui.gamecolors.GameColorsRouteScreen
import com.boardgamegeek.ui.gamedetail.GameDetailRouteScreen
import com.boardgamegeek.ui.geeklist.GeekListRouteScreen
import com.boardgamegeek.ui.geeklists.GeekListsRouteScreen
import com.boardgamegeek.ui.geeklistitem.GeekListItemRouteScreen
import com.boardgamegeek.ui.hotness.HotnessRouteScreen
import com.boardgamegeek.ui.image.ImageRouteScreen
import com.boardgamegeek.ui.login.LoginRouteScreen
import com.boardgamegeek.ui.logplay.LogPlayRouteScreen
import com.boardgamegeek.ui.logplayer.LogPlayerRouteScreen
import com.boardgamegeek.ui.locations.LocationsRouteScreen
import com.boardgamegeek.ui.mechanic.MechanicRouteScreen
import com.boardgamegeek.ui.mechanics.MechanicsRouteScreen
import com.boardgamegeek.ui.navigation.AppRoute
import com.boardgamegeek.ui.navigation.ArticleRoute
import com.boardgamegeek.ui.navigation.ArtistsRoute
import com.boardgamegeek.ui.navigation.BackStackAppNavigator
import com.boardgamegeek.ui.navigation.BuddyCollectionRoute
import com.boardgamegeek.ui.navigation.BuddyPlaysRoute
import com.boardgamegeek.ui.navigation.BuddyRoute
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CategoryRoute
import com.boardgamegeek.ui.navigation.CategoriesRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CommentsRoute
import com.boardgamegeek.ui.navigation.DataRoute
import com.boardgamegeek.ui.navigation.DesignersRoute
import com.boardgamegeek.ui.navigation.ForumRoute
import com.boardgamegeek.ui.navigation.ForumsRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.GameColorsRoute
import com.boardgamegeek.ui.navigation.GameDetailRoute
import com.boardgamegeek.ui.navigation.GameCollectionItemRoute
import com.boardgamegeek.ui.navigation.GamePlayStatsRoute
import com.boardgamegeek.ui.navigation.GamePlaysRoute
import com.boardgamegeek.ui.navigation.GeekListItemRoute
import com.boardgamegeek.ui.navigation.GeekListRoute
import com.boardgamegeek.ui.navigation.GeekListsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.ImageRoute
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.navigation.LocalRouteResultCoordinator
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LocationRoute
import com.boardgamegeek.ui.navigation.LocationsRoute
import com.boardgamegeek.ui.navigation.LogPlayRoute
import com.boardgamegeek.ui.navigation.LogPlayerRoute
import com.boardgamegeek.ui.navigation.MechanicRoute
import com.boardgamegeek.ui.navigation.MechanicsRoute
import com.boardgamegeek.ui.navigation.PersonRoute
import com.boardgamegeek.ui.navigation.PlayRoute
import com.boardgamegeek.ui.navigation.PlayStatsRoute
import com.boardgamegeek.ui.navigation.PlayerColorsRoute
import com.boardgamegeek.ui.navigation.PlayerPlaysRoute
import com.boardgamegeek.ui.navigation.PlayersRoute
import com.boardgamegeek.ui.navigation.PlaysRoute
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import com.boardgamegeek.ui.navigation.PublishersRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.SettingsRoute
import com.boardgamegeek.ui.navigation.SyncRoute
import com.boardgamegeek.ui.navigation.ThreadRoute
import com.boardgamegeek.ui.navigation.TopGamesRoute
import com.boardgamegeek.ui.navigation.rememberRouteResultCoordinator
import com.boardgamegeek.ui.play.PlayRouteScreen
import com.boardgamegeek.ui.playercolors.PlayerColorsRouteScreen
import com.boardgamegeek.ui.players.PlayersRouteScreen
import com.boardgamegeek.ui.plays.BuddyPlaysRouteScreen
import com.boardgamegeek.ui.plays.GamePlaysRouteScreen
import com.boardgamegeek.ui.plays.LocationRouteScreen
import com.boardgamegeek.ui.plays.PlayerPlaysRouteScreen
import com.boardgamegeek.ui.plays.PlaysRouteScreen
import com.boardgamegeek.ui.person.PersonRouteScreen
import com.boardgamegeek.ui.playstats.GamePlayStatsRouteScreen
import com.boardgamegeek.ui.playstats.PlayStatsRouteScreen
import com.boardgamegeek.ui.playssummary.PlaysSummaryRouteScreen
import com.boardgamegeek.ui.publishers.PublishersRouteScreen
import com.boardgamegeek.ui.search.SearchRouteScreen
import com.boardgamegeek.ui.settings.SettingsRouteScreen
import com.boardgamegeek.ui.sync.SyncRouteScreen
import com.boardgamegeek.ui.thread.ThreadRouteScreen
import com.boardgamegeek.ui.topgames.TopGamesRouteScreen

@Composable
fun BggApp(
    initialRoute: AppRoute,
    pendingExternalRoute: AppRoute?,
    pendingExternalRouteShouldReplace: Boolean,
    onExternalRouteConsumed: () -> Unit,
    onLoginSuccess: ((String) -> Unit)? = null,
) {
    val backStack = rememberNavBackStack(initialRoute)
    val navigator = remember(backStack) { BackStackAppNavigator(backStack) }
    val routeResultCoordinator = rememberRouteResultCoordinator()
    val entries = entryProvider<NavKey> {
        entry<CollectionRoute> {
            CollectionRouteScreen(route = it)
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
            LoginRouteScreen(
                initialUsername = it.username,
                onLoginSuccess = onLoginSuccess,
            )
        }
        entry<LogPlayRoute> {
            LogPlayRouteScreen(route = it)
        }
        entry<LogPlayerRoute> {
            LogPlayerRouteScreen(route = it)
        }
        entry<SearchRoute> { route ->
            SearchRouteScreen(initialQuery = route.query)
        }
        entry<GameRoute> {
            GameRouteScreen(route = it)
        }
        entry<MechanicRoute> {
            MechanicRouteScreen(route = it)
        }
        entry<CategoryRoute> {
            com.boardgamegeek.ui.category.CategoryRouteScreen(route = it)
        }
        entry<BuddyRoute> {
            BuddyRouteScreen(route = it)
        }
        entry<BuddyCollectionRoute> {
            BuddyCollectionRouteScreen(route = it)
        }
        entry<BuddyPlaysRoute> {
            BuddyPlaysRouteScreen(route = it)
        }
        entry<PlayersRoute> {
            PlayersRouteScreen(route = it)
        }
        entry<LocationsRoute> {
            LocationsRouteScreen()
        }
        entry<PlayStatsRoute> {
            PlayStatsRouteScreen()
        }
        entry<CommentsRoute> {
            CommentsRouteScreen(route = it)
        }
        entry<GameDetailRoute> {
            GameDetailRouteScreen(route = it)
        }
        entry<GameColorsRoute> {
            GameColorsRouteScreen(route = it)
        }
        entry<GameCollectionItemRoute> {
            GameCollectionItemRouteScreen(route = it)
        }
        entry<PlayerColorsRoute> {
            PlayerColorsRouteScreen(route = it)
        }
        entry<PlaysRoute> {
            PlaysRouteScreen()
        }
        entry<GamePlaysRoute> {
            GamePlaysRouteScreen(route = it)
        }
        entry<PlayerPlaysRoute> {
            PlayerPlaysRouteScreen(route = it)
        }
        entry<LocationRoute> {
            LocationRouteScreen(route = it)
        }
        entry<PlayRoute> {
            PlayRouteScreen(route = it)
        }
        entry<GamePlayStatsRoute> {
            GamePlayStatsRouteScreen(route = it)
        }
        entry<PersonRoute> {
            PersonRouteScreen(route = it)
        }
        entry<ForumRoute> {
            ForumRouteScreen(route = it)
        }
        entry<ThreadRoute> {
            ThreadRouteScreen(route = it)
        }
        entry<ArticleRoute> {
            ArticleRouteScreen(route = it)
        }
        entry<ImageRoute> {
            ImageRouteScreen(route = it)
        }
        entry<DesignersRoute> {
            DesignersRouteScreen()
        }
        entry<ArtistsRoute> {
            ArtistsRouteScreen()
        }
        entry<PublishersRoute> {
            PublishersRouteScreen()
        }
        entry<CategoriesRoute> {
            CategoriesRouteScreen()
        }
        entry<MechanicsRoute> {
            MechanicsRouteScreen()
        }
        entry<GeekListRoute> {
            GeekListRouteScreen(route = it)
        }
        entry<GeekListItemRoute> {
            GeekListItemRouteScreen(route = it)
        }
    }

    LaunchedEffect(pendingExternalRoute, pendingExternalRouteShouldReplace) {
        pendingExternalRoute?.let {
            if (pendingExternalRouteShouldReplace) {
                navigator.replace(it)
            } else {
                navigator.navigate(it)
            }
            onExternalRouteConsumed()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppNavigator provides navigator,
        LocalRouteResultCoordinator provides routeResultCoordinator,
    ) {
        NavDisplay(
            backStack = backStack,
            entryProvider = entries,
            onBack = { navigator.popBackStack() },
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                )
            },
        )
    }
}

@Composable
private fun PlaceholderRouteScreen(label: String) {
    Text(label)
}
