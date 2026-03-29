package com.boardgamegeek.ui.mechanics

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.mechanic.MechanicActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.MechanicsRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.theme.AppTheme

object MechanicsActivity {
    fun start(context: Context) {
        context.startActivity(MainActivity.createIntent(context, MechanicsRoute))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicsRouteScreen(
    viewModel: MechanicsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val mechanics by viewModel.mechanics.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_mechanics)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        Text(
                            text = (mechanics?.size ?: 0).toString(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.menu_sort),
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_sort_name)) },
                                    onClick = {
                                        viewModel.sort(Mechanic.SortType.NAME)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortType == Mechanic.SortType.NAME) {
                                            Text("\u2713")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_sort_item_count)) },
                                    onClick = {
                                        viewModel.sort(Mechanic.SortType.ITEM_COUNT)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortType == Mechanic.SortType.ITEM_COUNT) {
                                            Text("\u2713")
                                        }
                                    },
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more),
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_refresh)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        viewModel.refresh()
                                        showOverflowMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_search)) },
                                    onClick = {
                                        SearchResultsActivity.start(context)
                                        showOverflowMenu = false
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            MechanicsScreen(
                viewModel = viewModel,
                paddingValues = paddingValues,
                onMechanicClick = { mechanic ->
                    MechanicActivity.start(context, mechanic.id, mechanic.name)
                },
            )
        }
    }
}
