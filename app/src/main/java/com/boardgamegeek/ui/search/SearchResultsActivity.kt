package com.boardgamegeek.ui.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.longToast
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchResultsActivity : ComponentActivity() {
    companion object {
        private const val KEY_SEARCH_TEXT = "com.boardgamegeek.SEARCH_TEXT"
        private const val ACTION_VOICE_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION"
    }

    private var searchText: String? = null
    private var initialQuery by mutableStateOf("")

    private val viewModel by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            searchText = it.getString(KEY_SEARCH_TEXT)
        }
        readIntent(intent)

        setContent {
            SearchRouteScreen(
                initialQuery = initialQuery,
                onBack = { finish() },
                onQueryChange = { searchText = it },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SEARCH_TEXT, searchText)
    }

    private fun readIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri == null) {
                    longToast(R.string.search_error_no_data)
                } else {
                    val gameName = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY).orEmpty()
                    startActivity(
                        com.boardgamegeek.ui.MainActivity.createIntent(
                            this,
                            GameRoute(gameId = Games.getGameId(uri), gameName = gameName),
                        ),
                    )
                }
                finish()
            }
            Intent.ACTION_SEARCH,
            ACTION_VOICE_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY).orEmpty()
                viewModel.search(query)
                searchText = query
                initialQuery = query
            }
            else -> {
                if (!searchText.isNullOrBlank()) {
                    initialQuery = searchText.orEmpty()
                }
            }
        }
    }
}

@Composable
fun SearchRouteScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    AppTheme {
        SearchResultsScreen(
            viewModel = viewModel,
            initialQuery = initialQuery,
            onQueryChange = onQueryChange,
            onBack = onBack,
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
