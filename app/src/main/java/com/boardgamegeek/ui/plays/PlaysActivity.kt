package com.boardgamegeek.ui.plays

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlaysRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import java.util.Calendar
import java.util.GregorianCalendar

object PlaysActivity {
    fun start(context: Context) {
        context.startActivity(MainActivity.createIntent(context, PlaysRoute))
    }
}

@Composable
fun PlaysRouteScreen(
    viewModel: PlaysViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(Unit) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
        }
        viewModel.setAll()
    }

    AppTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val plays by viewModel.plays.collectAsState()
        val filterType by viewModel.filterType.collectAsState()
        val sortType by viewModel.sortType.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.errorMessageFlow.collect { message ->
                if (!message.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearErrorMessage()
                }
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.loggedPlayResultFlow.collect { result ->
                result?.let {
                    context.notifyLoggedPlay(it)
                    viewModel.clearLoggedPlayResult()
                }
            }
        }

        PlaysActivityScaffold(
            playCount = plays.sumOf { it.quantity },
            filterType = filterType,
            sortType = sortType,
            snackbarHostState = snackbarHostState,
            onBack = { navigator.popBackStackOrFinish(context) },
            onFilter = { type ->
                FirebaseAnalytics.getInstance(context).logEvent("Filter") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
                    bundle.putString("FilterBy", type.toString())
                }
                viewModel.setFilter(type)
            },
            onSort = { type ->
                FirebaseAnalytics.getInstance(context).logEvent("Sort") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
                    param("SortBy", type.toString())
                }
                viewModel.setSort(type)
            },
            onRefreshOnDate = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        viewModel.refreshPlaysByDate(GregorianCalendar(year, month, day).timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        ) { paddingValues ->
            PlaysScreen(
                viewModel = viewModel,
                emptyStringResId = R.string.empty_plays,
                showGameName = true,
                gameId = com.boardgamegeek.provider.BggContract.INVALID_ID,
                gameName = "",
                heroImageUrl = "",
                arePlayersCustomSorted = false,
                iconColor = android.graphics.Color.TRANSPARENT,
                contentPadding = paddingValues,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaysActivityScaffold(
    playCount: Int,
    filterType: PlaysViewModel.FilterType,
    sortType: PlaysViewModel.SortType,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFilter: (PlaysViewModel.FilterType) -> Unit,
    onSort: (PlaysViewModel.SortType) -> Unit,
    onRefreshOnDate: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.title_plays))
                        Text(
                            text = when (filterType) {
                                PlaysViewModel.FilterType.PENDING -> stringResource(R.string.menu_plays_filter_pending)
                                PlaysViewModel.FilterType.DIRTY -> stringResource(R.string.menu_plays_filter_in_progress)
                                PlaysViewModel.FilterType.ALL -> when (sortType) {
                                    PlaysViewModel.SortType.DATE -> stringResource(R.string.by_prefix, stringResource(R.string.menu_plays_sort_date))
                                    PlaysViewModel.SortType.LOCATION -> stringResource(R.string.by_prefix, stringResource(R.string.menu_plays_sort_location))
                                    PlaysViewModel.SortType.GAME -> stringResource(R.string.by_prefix, stringResource(R.string.menu_plays_sort_game))
                                    PlaysViewModel.SortType.LENGTH -> stringResource(R.string.by_prefix, stringResource(R.string.menu_plays_sort_length))
                                }
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                actions = {
                    Text(text = playCount.toString())
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.menu_plays_filter))
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_filter_all)) },
                            onClick = { showFilterMenu = false; onFilter(PlaysViewModel.FilterType.ALL) },
                            trailingIcon = if (filterType == PlaysViewModel.FilterType.ALL) ({ Text("•") }) else null,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_filter_in_progress)) },
                            onClick = { showFilterMenu = false; onFilter(PlaysViewModel.FilterType.DIRTY) },
                            trailingIcon = if (filterType == PlaysViewModel.FilterType.DIRTY) ({ Text("•") }) else null,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_filter_pending)) },
                            onClick = { showFilterMenu = false; onFilter(PlaysViewModel.FilterType.PENDING) },
                            trailingIcon = if (filterType == PlaysViewModel.FilterType.PENDING) ({ Text("•") }) else null,
                        )
                    }

                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.menu_sort))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_sort_date)) },
                            onClick = { showSortMenu = false; onSort(PlaysViewModel.SortType.DATE) },
                            trailingIcon = if (sortType == PlaysViewModel.SortType.DATE) ({ Text("•") }) else null,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_sort_location)) },
                            onClick = { showSortMenu = false; onSort(PlaysViewModel.SortType.LOCATION) },
                            trailingIcon = if (sortType == PlaysViewModel.SortType.LOCATION) ({ Text("•") }) else null,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_sort_game)) },
                            onClick = { showSortMenu = false; onSort(PlaysViewModel.SortType.GAME) },
                            trailingIcon = if (sortType == PlaysViewModel.SortType.GAME) ({ Text("•") }) else null,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_plays_sort_length)) },
                            onClick = { showSortMenu = false; onSort(PlaysViewModel.SortType.LENGTH) },
                            trailingIcon = if (sortType == PlaysViewModel.SortType.LENGTH) ({ Text("•") }) else null,
                        )
                    }

                    IconButton(onClick = onRefreshOnDate) {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.menu_refresh_on))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = content,
    )
}
