package com.boardgamegeek.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFragment
import com.boardgamegeek.ui.dialog.CollectionSortDialogFragment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import com.boardgamegeek.extensions.clearTask
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.newTask
import com.boardgamegeek.extensions.toLongLabel
import com.boardgamegeek.extensions.toShortLabel
import com.boardgamegeek.ui.collectiondetails.CollectionDetailsActivity

@AndroidEntryPoint
class CollectionActivity : AppCompatActivity() {

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
            viewModel.refresh()
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
            val defaultTitle = stringResource(id = R.string.title_collection)
            val finalTopBarTitle = currentTopBarTitle.ifEmpty { defaultTitle }
            val currentSort by viewModel.effectiveSort.collectAsState()
            val currentSortType = currentSort?.first?.getType(currentSort?.second ?: false)
                ?: com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT
            val currentFilters by viewModel.effectiveFilters.collectAsState()
            val views by viewModel.views.collectAsState()
            val selectedViewId by viewModel.selectedViewId.collectAsState()
            val canSaveView = currentFilters.isNotEmpty() ||
                currentSortType != com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT
            val viewOptions = remember(views, defaultTitle) {
                if (views.any { it.id == CollectionViewPrefs.DEFAULT_DEFAULT_ID }) {
                    views
                } else {
                    listOf(
                        CollectionView(
                            id = CollectionViewPrefs.DEFAULT_DEFAULT_ID,
                            name = defaultTitle
                        )
                    ) + views
                }
            }

            AppScreen(
                topBarTitle = finalTopBarTitle,
                topBarTitleContent = {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = true }) {
                        Text(finalTopBarTitle)
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        viewOptions.forEach { view ->
                            DropdownMenuItem(
                                text = { Text(view.name.ifEmpty { stringResource(id = R.string.title_collection) }) },
                                onClick = {
                                    expanded = false
                                    if (view.id != selectedViewId) {
                                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
                                        }
                                        viewModel.selectView(view.id)
                                    }
                                }
                            )
                        }
                    }
                },
                currentScreenRouteFromActivity = BottomNavItem.Collection.route,
                snackbarHostState = snackbarHostState, // Pass SnackbarHostState
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                // TODO: Implement Collection View Selector (Spinner replacement)
                // topBarActions = { /* Spinner replacement or view selector UI */ }
                topBarActions = {
                    IconButton(
                        onClick = { startActivity(Intent(this@CollectionActivity, CollectionDetailsActivity::class.java)) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = stringResource(R.string.title_collection_details)
                        )
                    }
                    IconButton(
                        onClick = {
                            showSaveViewDialog(
                                if (selectedViewId <= 0) "" else finalTopBarTitle,
                                createViewDescription(currentSort, currentFilters)
                            )
                        },
                        enabled = canSaveView
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.menu_collection_view_save)
                        )
                    }
                    IconButton(onClick = { showSortDialog(currentSortType) }) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = stringResource(R.string.menu_sort)
                        )
                    }
                    IconButton(onClick = { showFilterDialog() }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.menu_collection_filter_add)
                        )
                    }
                }
            ) { paddingValues ->
                CollectionScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    isCreatingShortcut = isCreatingShortcut,
                    changingGamePlayId = changingGamePlayId,
                    onGameClick = { gameId, gameName, thumbnailUrl, heroImageUrl ->
                        GameActivity.start(this, gameId, gameName, thumbnailUrl!!, heroImageUrl!!)
                    },
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

    private fun showFilterDialog() {
        CollectionFilterDialogFragment().show(supportFragmentManager, "collection_filter")
    }

    private fun showSortDialog(sortType: Int) {
        CollectionSortDialogFragment.newInstance(sortType)
            .show(supportFragmentManager, "collection_sort")
    }

    private fun showSaveViewDialog(name: String, description: String) {
        com.boardgamegeek.ui.dialog.SaveViewDialogFragment.newInstance(name, description)
            .show(supportFragmentManager, "view_save")
    }

    private fun showDeleteViewDialog() {
        com.boardgamegeek.ui.dialog.DeleteViewDialogFragment.newInstance()
            .show(supportFragmentManager, "view_delete")
    }

    private fun createViewDescription(
        sort: Pair<com.boardgamegeek.sorter.CollectionSorter, Boolean>?,
        filters: List<com.boardgamegeek.filterer.CollectionFilterer>
    ): String {
        val text = StringBuilder()
        if (filters.isNotEmpty()) {
            text.append(getString(R.string.filtered_by))
            filters.map { "\n\u2022 ${it.description()}" }.forEach { text.append(it) }
        }
        text.append("\n\n")
        sort?.let {
            if (it.first.getType(it.second) != com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT) {
                text.append(getString(R.string.sort_description, it.first.description))
            }
        }
        return text.trim().toString()
    }

    companion object {
        private const val KEY_VIEW_ID = "VIEW_ID"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun startForGameChange(context: Context, playId: Long) {
            context.startActivity(MainActivity.createIntent(context, CollectionRoute))
        }

        fun createShortcutInfo(context: Context, viewId: Int, viewName: String): ShortcutInfoCompat {
            val intent = MainActivity.createIntent(context, CollectionRoute)
                .clearTask()
                .newTask()
                .apply { action = Intent.ACTION_VIEW }
            return ShortcutInfoCompat.Builder(context, createShortcutName(viewId))
                .setShortLabel(viewName.toShortLabel())
                .setLongLabel(viewName.toLongLabel())
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_ic_collection))
                .setIntent(intent)
                .build()
        }

        fun createShortcutName(viewId: Int) = "collection_view-$viewId"
    }
}

@Composable
fun CollectionRouteScreen(
    initialViewId: Int? = null,
    changingGamePlayId: Long = BggContract.INVALID_ID.toLong(),
    isCreatingShortcut: Boolean = false,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialViewId) {
        if (initialViewId != null && initialViewId != CollectionViewPrefs.DEFAULT_DEFAULT_ID) {
            viewModel.selectView(initialViewId)
        }
        viewModel.refresh()
    }

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
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    val currentTopBarTitle by viewModel.selectedViewName.collectAsState()
    val defaultTitle = stringResource(id = R.string.title_collection)
    val finalTopBarTitle = currentTopBarTitle.ifEmpty { defaultTitle }
    val currentSort by viewModel.effectiveSort.collectAsState()
    val currentSortType = currentSort?.first?.getType(currentSort?.second ?: false)
        ?: com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT
    val currentFilters by viewModel.effectiveFilters.collectAsState()
    val views by viewModel.views.collectAsState()
    val selectedViewId by viewModel.selectedViewId.collectAsState()
    val canSaveView = currentFilters.isNotEmpty() ||
        currentSortType != com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT
    val viewOptions = remember(views, defaultTitle) {
        if (views.any { it.id == CollectionViewPrefs.DEFAULT_DEFAULT_ID }) {
            views
        } else {
            listOf(
                CollectionView(
                    id = CollectionViewPrefs.DEFAULT_DEFAULT_ID,
                    name = defaultTitle,
                ),
            ) + views
        }
    }

    AppScreen(
        topBarTitle = finalTopBarTitle,
        topBarTitleContent = {
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) {
                Text(finalTopBarTitle)
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                viewOptions.forEach { view ->
                    DropdownMenuItem(
                        text = { Text(view.name.ifEmpty { stringResource(id = R.string.title_collection) }) },
                        onClick = {
                            expanded = false
                            if (view.id != selectedViewId) {
                                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
                                }
                                viewModel.selectView(view.id)
                            }
                        },
                    )
                }
            }
        },
        currentScreenRouteFromActivity = BottomNavItem.Collection.route,
        snackbarHostState = snackbarHostState,
        topBarActions = {
            IconButton(onClick = { navigator.navigate(CollectionDetailsRoute) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = stringResource(R.string.title_collection_details),
                )
            }
            IconButton(
                onClick = {
                    activity?.let {
                        com.boardgamegeek.ui.dialog.SaveViewDialogFragment
                            .newInstance(
                                if (selectedViewId <= 0) "" else finalTopBarTitle,
                                buildViewDescription(context, currentSort, currentFilters),
                            )
                            .show(it.supportFragmentManager, "view_save")
                    }
                },
                enabled = canSaveView,
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.menu_collection_view_save),
                )
            }
            IconButton(onClick = {
                activity?.let {
                    CollectionSortDialogFragment.newInstance(currentSortType)
                        .show(it.supportFragmentManager, "collection_sort")
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = stringResource(R.string.menu_sort),
                )
            }
            IconButton(onClick = {
                activity?.let {
                    CollectionFilterDialogFragment().show(it.supportFragmentManager, "collection_filter")
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.menu_collection_filter_add),
                )
            }
        },
    ) { paddingValues ->
        CollectionScreen(
            viewModel = viewModel,
            paddingValues = paddingValues,
            isCreatingShortcut = isCreatingShortcut,
            changingGamePlayId = changingGamePlayId,
            onGameClick = { gameId, gameName, thumbnailUrl, heroImageUrl ->
                navigator.navigate(
                    GameRoute(
                        gameId = gameId,
                        gameName = gameName,
                        thumbnailUrl = thumbnailUrl.orEmpty(),
                        heroImageUrl = heroImageUrl.orEmpty(),
                    ),
                )
            },
        )
    }
}

private fun buildViewDescription(
    context: Context,
    sort: Pair<com.boardgamegeek.sorter.CollectionSorter, Boolean>?,
    filters: List<com.boardgamegeek.filterer.CollectionFilterer>,
): String {
    val text = StringBuilder()
    if (filters.isNotEmpty()) {
        text.append(context.getString(R.string.filtered_by))
        filters.map { "\n\u2022 ${it.description()}" }.forEach { text.append(it) }
    }
    text.append("\n\n")
    sort?.let {
        if (it.first.getType(it.second) != com.boardgamegeek.sorter.CollectionSorterFactory.TYPE_DEFAULT) {
            text.append(context.getString(R.string.sort_description, it.first.description))
        }
    }
    return text.trim().toString()
}
