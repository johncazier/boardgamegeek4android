package com.boardgamegeek.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isCollectionSetToSync
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.ui.navigation.AppRoute
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingExternalRoute by mutableStateOf<AppRoute?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExternalRoute = readIntentRoute(intent)
        setContent {
            BggApp(
                initialRoute = pendingExternalRoute ?: resolveInitialRoute(this),
                pendingExternalRoute = pendingExternalRoute,
                onExternalRouteConsumed = { pendingExternalRoute = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingExternalRoute = readIntentRoute(intent)
    }

    private fun readIntentRoute(intent: Intent): AppRoute? {
        intent.getStringExtra(KEY_ROUTE)?.let { encoded ->
            return runCatching {
                json.decodeFromString<AppRoute>(encoded)
            }.getOrNull()
        }

        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return null
                GameRoute(
                    gameId = Games.getGameId(uri),
                    gameName = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY).orEmpty(),
                )
            }

            Intent.ACTION_SEARCH,
            ACTION_VOICE_SEARCH -> SearchRoute(
                query = intent.getStringExtra(SearchManager.QUERY).orEmpty(),
            )

            else -> null
        }
    }

    companion object {
        private const val KEY_ROUTE = "app_route"
        private const val ACTION_VOICE_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION"

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun createIntent(context: Context, route: AppRoute): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(KEY_ROUTE, json.encodeToString(AppRoute.serializer(), route))
            }
        }
    }
}

private fun resolveInitialRoute(context: Context): AppRoute {
    val prefs = context.preferences()
    return if (Authenticator.isSignedIn(context)) {
        when {
            Authenticator.isOldAuth(context) -> {
                Authenticator.signOut(context)
                HotnessRoute
            }

            prefs.isCollectionSetToSync() -> CollectionDetailsRoute
            prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true -> PlaysSummaryRoute
            prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true -> BuddiesRoute
            else -> HotnessRoute
        }
    } else {
        HotnessRoute
    }
}
