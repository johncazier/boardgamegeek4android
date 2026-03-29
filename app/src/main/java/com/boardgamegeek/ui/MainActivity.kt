package com.boardgamegeek.ui

import android.app.SearchManager
import android.accounts.AccountManager
import android.accounts.AccountAuthenticatorResponse
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
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.DataRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import com.boardgamegeek.ui.navigation.SettingsRoute
import com.boardgamegeek.ui.navigation.SyncRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingExternalRoute by mutableStateOf<AppRoute?>(null)
    private var pendingExternalRouteShouldReplace by mutableStateOf(false)
    private var platformLoginHandler by mutableStateOf<((String) -> Unit)?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePlatformLoginHandler(intent)
        val initialNavigation = readIntentRoute(intent)
        val initialRoute = initialNavigation?.route ?: resolveInitialRoute(this)
        setContent {
            BggApp(
                initialRoute = initialRoute,
                pendingExternalRoute = pendingExternalRoute,
                pendingExternalRouteShouldReplace = pendingExternalRouteShouldReplace,
                onExternalRouteConsumed = {
                    pendingExternalRoute = null
                    pendingExternalRouteShouldReplace = false
                },
                onLoginSuccess = platformLoginHandler,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePlatformLoginHandler(intent)
        readIntentRoute(intent)?.let {
            pendingExternalRoute = it.route
            pendingExternalRouteShouldReplace = it.replaceBackStack
        }
    }

    private fun updatePlatformLoginHandler(intent: Intent) {
        val response = intent.getParcelableExtra<AccountAuthenticatorResponse>(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        if (response == null) {
            platformLoginHandler = null
            return
        }
        response.onRequestContinued()
        platformLoginHandler = { username ->
            val extras = Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, username)
                putString(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE)
            }
            setResult(RESULT_OK, Intent().putExtras(extras))
            response.onResult(extras)
            platformLoginHandler = null
            finish()
        }
    }

    private fun readIntentRoute(intent: Intent): PendingRoute? {
        intent.getStringExtra(KEY_ROUTE)?.let { encoded ->
            return runCatching {
                PendingRoute(
                    route = json.decodeFromString<AppRoute>(encoded),
                    replaceBackStack = intent.getBooleanExtra(KEY_REPLACE_BACK_STACK, false),
                )
            }.getOrNull()
        }

        val route = when (intent.action) {
            Intent.ACTION_CREATE_SHORTCUT -> CollectionRoute(isCreatingShortcut = true)

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

            Intent.ACTION_APPLICATION_PREFERENCES,
            Intent.ACTION_MANAGE_NETWORK_USAGE,
            getString(com.boardgamegeek.R.string.intent_action_account) -> SettingsRoute

            getString(com.boardgamegeek.R.string.intent_action_sync) -> SyncRoute

            getString(com.boardgamegeek.R.string.intent_action_data) -> DataRoute

            else -> null
        }
        if (route != null) {
            return PendingRoute(route, replaceBackStack = intent.action == Intent.ACTION_CREATE_SHORTCUT)
        }

        if (intent.hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            return PendingRoute(
                route = LoginRoute(username = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)),
                replaceBackStack = true,
            )
        }

        return null
    }

    companion object {
        private const val KEY_ROUTE = "app_route"
        private const val KEY_REPLACE_BACK_STACK = "replace_back_stack"
        private const val ACTION_VOICE_SEARCH = "com.google.android.gms.actions.SEARCH_ACTION"

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun createIntent(
            context: Context,
            route: AppRoute,
            replaceBackStack: Boolean = false,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(KEY_ROUTE, json.encodeToString(AppRoute.serializer(), route))
                putExtra(KEY_REPLACE_BACK_STACK, replaceBackStack)
            }
        }
    }
}

private data class PendingRoute(
    val route: AppRoute,
    val replaceBackStack: Boolean,
)

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
