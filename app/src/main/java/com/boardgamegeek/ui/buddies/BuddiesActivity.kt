package com.boardgamegeek.ui.buddies

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.model.User
import com.boardgamegeek.ui.AppScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BuddiesActivity : ComponentActivity() {
    private val viewModel by viewModels<BuddiesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BuddiesRouteScreen()
        }
    }
}

@Composable
fun BuddiesRouteScreen(
    viewModel: BuddiesViewModel = hiltViewModel(),
) {
    val buddies by viewModel.buddies.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    AppScreen(
        topBarTitle = stringResource(R.string.title_buddies),
        currentScreenRouteFromActivity = "buddies",
        snackbarHostState = snackbarHostState,
        topBarActions = {
            Text(
                text = (buddies?.size ?: 0).toString(),
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
                        text = { Text(stringResource(R.string.menu_sort_username)) },
                        onClick = {
                            viewModel.sort(User.SortType.USERNAME)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (sortType == User.SortType.USERNAME) {
                                Text("\u2713")
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_sort_first_name)) },
                        onClick = {
                            viewModel.sort(User.SortType.FIRST_NAME)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (sortType == User.SortType.FIRST_NAME) {
                                Text("\u2713")
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_sort_last_name)) },
                        onClick = {
                            viewModel.sort(User.SortType.LAST_NAME)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (sortType == User.SortType.LAST_NAME) {
                                Text("\u2713")
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        BuddiesScreen(
            viewModel = viewModel,
            paddingValues = paddingValues,
            snackbarHostState = snackbarHostState,
        )
    }
}
