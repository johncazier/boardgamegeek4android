package com.boardgamegeek.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.toLongLabel
import com.boardgamegeek.extensions.toShortLabel
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.LegacyCollectionActivity.Companion.createShortcutName
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CollectionActivity : ComponentActivity() {

    private val viewModel: CollectionViewModel by viewModels()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Intent Extras
    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = BggContract.INVALID_ID.toLong()
    private var initialViewId: Int = CollectionViewPrefs.DEFAULT_DEFAULT_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        readIntentExtras()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
            }
            // ViewModel's init block or selectedViewId's initial value should handle
            // the default view or the one from intent.
            // We ensure selectView is called if a specific initialViewId is determined.
            if (initialViewId != viewModel.selectedViewId.value ||
                initialViewId != CollectionViewPrefs.DEFAULT_DEFAULT_ID) { // Ensure selection if not default
                viewModel.selectView(initialViewId)
            }
        }

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current // For Toasts

            // Collect event flows
            LaunchedEffect(Unit) {
                viewModel.toastMessageEvents.collectLatest { event ->
                    event.getContentIfNotHandled()?.let { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.errorMessageEvents.collectLatest { event ->
                    event.getContentIfNotHandled()?.let { errorMessage ->
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.loggedPlayResultEvents.collectLatest { event ->
                    event.getContentIfNotHandled()?.let { result: PlayUploadResult ->
                        // Notify logged play, e.g., show a Snackbar
                        /* todo
                        val message = if (result.isSuccess) {
                            getString(R.string.msg_play_logged_for_game, result.play.gameName)
                        } else {
                            getString(R.string.msg_play_log_failed) // Or a more specific error
                        }
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                         */
                    }
                }
            }

            val currentTopBarTitle by viewModel.selectedViewName.collectAsState()
            val finalTopBarTitle = currentTopBarTitle.ifEmpty {
                stringResource(id = R.string.title_collection)
            }

            AppScreen(
                topBarTitle = finalTopBarTitle,
                currentScreenRouteFromActivity = BottomNavItem.Collection.route,
                snackbarHostState = snackbarHostState, // Pass SnackbarHostState
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                // TODO: Implement Collection View Selector (Spinner replacement)
                // topBarActions = { /* Spinner replacement or view selector UI */ }
            ) { paddingValues ->
                CollectionScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    isCreatingShortcut = isCreatingShortcut,
                    changingGamePlayId = changingGamePlayId,
                    onGameClick = { gameId, gameName, thumbnailUrl, heroImageUrl ->
                        GameActivity.start(this, gameId, gameName, thumbnailUrl!!, heroImageUrl!!)
                    },
                    onLogPlayForm = { gameId, gameName, thumbnailUrl ->
                        LogPlayActivity.logPlay(this, gameId, gameName, thumbnailUrl ?: "") // Provide default if null
                    },
                    onLogPlayWizard = { gameId, gameName ->
                        NewPlayActivity.start(this, gameId, gameName)
                    },
                    onCreateShortcut = {
                        // This might be triggered from a UI element within CollectionScreen now
                        // For example, if there's a "Create Shortcut" button for the current view
                        viewModel.createShortcut()
                    }
                    // Pass other necessary callbacks for CollectionScreen
                )
            }
        }
    }

    private fun readIntentExtras() {
        isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT == intent.action
        changingGamePlayId = intent.getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())
        val hideNavigationFeatures = isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID.toLong()

        // Determine the initialViewId based on intent, respecting hideNavigationFeatures
        initialViewId = if (hideNavigationFeatures) {
            CollectionViewPrefs.DEFAULT_DEFAULT_ID // Default view if creating shortcut or changing play
        } else {
            intent.getIntExtra(KEY_VIEW_ID, viewModel.defaultViewIdFlow.value) // Use default from ViewModel if not in intent
        }
    }

    companion object {
        private const val KEY_VIEW_ID = "VIEW_ID"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun startForGameChange(context: Context, playId: Long) {
            val intent = Intent(context, CollectionActivity::class.java).apply {
                putExtra(KEY_CHANGING_GAME_PLAY_ID, playId)
            }
            context.startActivity(intent)
        }

        fun createShortcutInfo(context: Context, viewId: Int, viewName: String): ShortcutInfoCompat {
            val intent = Intent(context, CollectionActivity::class.java).apply {
                action = Intent.ACTION_VIEW // Standard action for viewing data
                putExtra(KEY_VIEW_ID, viewId)
                // Clear task and new task for shortcuts to ensure they don't just resume an existing task stack inappropriately
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return ShortcutInfoCompat.Builder(context, createShortcutName(viewId))
                .setShortLabel(viewName.toShortLabel()) // Ensure .toShortLabel() is robust
                .setLongLabel(viewName.toLongLabel())   // Ensure .toLongLabel() is robust
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
                .setIntent(intent)
                .build()
        }
    }
}