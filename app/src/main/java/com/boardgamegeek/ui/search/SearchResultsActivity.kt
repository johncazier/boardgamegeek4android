package com.boardgamegeek.ui.search

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme

object SearchResultsActivity {
    fun start(context: Context, query: String = "") {
        context.startActivity(
            MainActivity.createIntent(
                context = context,
                route = SearchRoute(query = query),
            ),
        )
    }
}

@Composable
fun SearchRouteScreen(
    initialQuery: String,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    val context = androidx.compose.ui.platform.LocalContext.current
    AppTheme {
        SearchResultsScreen(
            viewModel = viewModel,
            initialQuery = initialQuery,
            onQueryChange = {},
            onBack = { navigator.popBackStackOrFinish(context) },
            onSearchSubmit = { query ->
                if (query.length > 1) {
                    viewModel.search(query)
                }
            },
            onGameOpen = { result ->
                navigator.navigate(GameRoute(gameId = result.id, gameName = result.name))
            },
        )
    }
}
