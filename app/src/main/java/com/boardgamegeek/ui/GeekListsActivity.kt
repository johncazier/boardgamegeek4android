package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeekListsActivity : ComponentActivity() {
    private val viewModel by viewModels<GeekListsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.ITEM_LIST_NAME, "GeekLists")
        }

        setContent {
            var showMenu by remember { mutableStateOf(false) }

            AppScreen(
                topBarTitle = stringResource(id = R.string.title_geeklists),
                currentScreenRouteFromActivity = BottomNavItem.GeekLists.route,
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                topBarActions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(id = R.string.menu_sort)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sort_geeklists_hot)) },
                                onClick = {
                                    viewModel.setSort(GeekListsViewModel.SortType.HOT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sort_geeklists_recent)) },
                                onClick = {
                                    viewModel.setSort(GeekListsViewModel.SortType.RECENT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sort_geeklists_active)) },
                                onClick = {
                                    viewModel.setSort(GeekListsViewModel.SortType.ACTIVE)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                GeekListsScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues
                )
            }
        }
    }
}
