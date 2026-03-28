package com.boardgamegeek.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.clearTask
import com.boardgamegeek.extensions.newTask
import com.boardgamegeek.extensions.toLongLabel
import com.boardgamegeek.extensions.toShortLabel
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CollectionActivity : AppCompatActivity() {

    private val viewModel: CollectionViewModel by viewModels()

    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = BggContract.INVALID_ID.toLong()
    private var initialViewId: Int = CollectionViewPrefs.DEFAULT_DEFAULT_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readIntentExtras()

        if (savedInstanceState == null) {
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Collection")
            }
        }

        setContent {
            CollectionRouteScreen(
                initialViewId = initialViewId,
                changingGamePlayId = changingGamePlayId,
                isCreatingShortcut = isCreatingShortcut,
                viewModel = viewModel,
            )
        }
    }

    private fun readIntentExtras() {
        isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT == intent.action
        changingGamePlayId = intent.getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())
        val hideNavigationFeatures = isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID.toLong()
        initialViewId = if (hideNavigationFeatures) {
            CollectionViewPrefs.DEFAULT_DEFAULT_ID
        } else {
            intent.getIntExtra(KEY_VIEW_ID, viewModel.defaultViewIdFlow.value)
        }
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
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var isSortSheetOpen by remember { mutableStateOf(false) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var saveViewDialog by remember { mutableStateOf<SaveViewDialogState?>(null) }

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
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
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
                    saveViewDialog = SaveViewDialogState(
                        initialName = if (selectedViewId <= 0) "" else finalTopBarTitle,
                        description = buildViewDescription(context, currentSort, currentFilters),
                    )
                },
                enabled = canSaveView,
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.menu_collection_view_save),
                )
            }
            IconButton(onClick = { isSortSheetOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = stringResource(R.string.menu_sort),
                )
            }
            IconButton(onClick = { isFilterSheetOpen = true }) {
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

    saveViewDialog?.let { state ->
        SaveCollectionViewDialog(
            initialName = state.initialName,
            description = state.description,
            selectedViewId = selectedViewId,
            viewModel = viewModel,
            onDismiss = { saveViewDialog = null },
        )
    }

    if (isSortSheetOpen) {
        CollectionSortSheet(
            currentSortType = currentSortType,
            onDismiss = { isSortSheetOpen = false },
            onSortSelected = viewModel::setSort,
        )
    }

    if (isFilterSheetOpen) {
        CollectionFilterSheet(
            filters = currentFilters,
            onDismiss = { isFilterSheetOpen = false },
            onFilterRemoved = viewModel::removeFilter,
        )
    }
}

private data class SaveViewDialogState(
    val initialName: String,
    val description: String,
)

private fun buildViewDescription(
    context: Context,
    sort: Pair<com.boardgamegeek.sorter.CollectionSorter, Boolean>?,
    filters: List<CollectionFilterer>,
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
