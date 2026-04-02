package com.boardgamegeek.ui.search

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme

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
