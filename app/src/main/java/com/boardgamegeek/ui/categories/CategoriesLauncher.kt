package com.boardgamegeek.ui.categories

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
import com.boardgamegeek.model.Category
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.category.CategoryLauncher
import com.boardgamegeek.ui.navigation.CategoriesRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.search.SearchResultsLauncher
import com.boardgamegeek.ui.theme.AppTheme

object CategoriesLauncher {
    fun start(context: Context) {
        context.startActivity(MainActivity.createIntent(context, CategoriesRoute))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesRouteScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_categories)) },
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
                            text = (categories?.size ?: 0).toString(),
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
                                        viewModel.sort(Category.SortType.NAME)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortType == Category.SortType.NAME) {
                                            Text("\u2713")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_sort_item_count)) },
                                    onClick = {
                                        viewModel.sort(Category.SortType.ITEM_COUNT)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortType == Category.SortType.ITEM_COUNT) {
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
                                        SearchResultsLauncher.start(context)
                                        showOverflowMenu = false
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            CategoriesScreen(
                viewModel = viewModel,
                paddingValues = paddingValues,
                onCategoryClick = { category ->
                    CategoryLauncher.start(context, category.id, category.name)
                },
            )
        }
    }
}
