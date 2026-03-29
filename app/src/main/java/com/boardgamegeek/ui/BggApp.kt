package com.boardgamegeek.ui

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
import com.boardgamegeek.ui.buddies.BuddiesRouteScreen
import com.boardgamegeek.ui.categories.CategoriesRouteScreen
import com.boardgamegeek.ui.collection.CollectionRouteScreen
import com.boardgamegeek.ui.collectiondetails.CollectionDetailsRouteScreen
import com.boardgamegeek.ui.data.DataRouteScreen
import com.boardgamegeek.ui.designers.DesignersRouteScreen
import com.boardgamegeek.ui.forum.ForumRouteScreen
import com.boardgamegeek.ui.forums.ForumsRouteScreen
import com.boardgamegeek.ui.game.GameRouteScreen
import com.boardgamegeek.ui.geeklist.GeekListRouteScreen
import com.boardgamegeek.ui.geeklists.GeekListsRouteScreen
import com.boardgamegeek.ui.geeklistitem.GeekListItemRouteScreen
import com.boardgamegeek.ui.hotness.HotnessRouteScreen
import com.boardgamegeek.ui.image.ImageRouteScreen
import com.boardgamegeek.ui.login.LoginRouteScreen
import com.boardgamegeek.ui.mechanics.MechanicsRouteScreen
import com.boardgamegeek.ui.navigation.AppRoute
import com.boardgamegeek.ui.navigation.ArticleRoute
import com.boardgamegeek.ui.navigation.ArtistsRoute
import com.boardgamegeek.ui.navigation.BackStackAppNavigator
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CategoriesRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.DataRoute
import com.boardgamegeek.ui.navigation.DesignersRoute
import com.boardgamegeek.ui.navigation.ForumRoute
import com.boardgamegeek.ui.navigation.ForumsRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.GeekListItemRoute
import com.boardgamegeek.ui.navigation.GeekListRoute
import com.boardgamegeek.ui.navigation.GeekListsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.ImageRoute
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.MechanicsRoute
import com.boardgamegeek.ui.navigation.PersonRoute
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import com.boardgamegeek.ui.navigation.PublishersRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.SettingsRoute
import com.boardgamegeek.ui.navigation.SyncRoute
import com.boardgamegeek.ui.navigation.ThreadRoute
import com.boardgamegeek.ui.navigation.TopGamesRoute
import com.boardgamegeek.ui.person.PersonRouteScreen
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
            SearchRouteScreen(initialQuery = route.query)
        }
        entry<GameRoute> {
            GameRouteScreen(route = it)
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
