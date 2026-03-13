package com.boardgamegeek.ui.playssummary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaysSummaryActivity : ComponentActivity() {
    private val viewModel by viewModels<PlaysSummaryViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showOverflowMenu by remember { mutableStateOf(false) }
            var showResyncDialog by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }

            AppTheme {
                AppScreen(
                    topBarTitle = stringResource(R.string.title_plays),
                    currentScreenRouteFromActivity = "plays",
                    onSearchClick = { startActivity(Intent(this, SearchResultsActivity::class.java)) },
                    snackbarHostState = snackbarHostState,
                    topBarActions = {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.re_sync)) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showResyncDialog = true
                                    }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    PlaysSummaryScreen(
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }

            if (showResyncDialog) {
                AlertDialog(
                    onDismissRequest = { showResyncDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResyncDialog = false
                                viewModel.reset()
                            }
                        ) {
                            Text(text = stringResource(R.string.re_sync))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResyncDialog = false }) {
                            Text(text = stringResource(R.string.cancel))
                        }
                    },
                    title = { Text(text = stringResource(R.string.pref_sync_re_sync_plays) + "?") },
                    text = { Text(text = stringResource(R.string.pref_sync_re_sync_plays_info_message)) },
                )
            }
        }
    }
}
