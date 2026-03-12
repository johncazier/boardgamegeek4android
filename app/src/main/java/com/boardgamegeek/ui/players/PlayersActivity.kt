package com.boardgamegeek.ui.players

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Player
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class PlayersActivity : ComponentActivity() {
    private val viewModel by viewModels<PlayersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Players")
            }
        }

        viewModel.sort(intent.extras?.getSerializableCompat(KEY_SORT_TYPE) ?: Player.SortType.NAME)

        setContent {
            val players by viewModel.players.collectAsStateWithLifecycle()
            val sortType by viewModel.sortType.collectAsStateWithLifecycle()
            val filter by viewModel.filter.collectAsStateWithLifecycle()
            var showSortMenu by remember { mutableStateOf(false) }
            var showFilter by remember { mutableStateOf(filter.isNotBlank()) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.title_players)) },
                        navigationIcon = {
                            IconButton(onClick = ::finish) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.menu_back)
                                )
                            }
                        },
                        actions = {
                            Text(
                                text = (players?.size ?: 0).toString(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = stringResource(R.string.menu_sort)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_name)) },
                                        onClick = {
                                            viewModel.sort(Player.SortType.NAME)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sortType == Player.SortType.NAME) {
                                                Text("\u2713")
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_quantity)) },
                                        onClick = {
                                            viewModel.sort(Player.SortType.PLAY_COUNT)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sortType == Player.SortType.PLAY_COUNT) {
                                                Text("\u2713")
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_sort_wins)) },
                                        onClick = {
                                            viewModel.sort(Player.SortType.WIN_COUNT)
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sortType == Player.SortType.WIN_COUNT) {
                                                Text("\u2713")
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showFilter = !showFilter },
                                enabled = (players?.isNotEmpty() == true) || filter.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.menu_filter)
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PlayersScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    showFilter = showFilter,
                    onShowFilterChange = { showFilter = it }
                )
            }
        }
    }

    companion object {
        private const val KEY_SORT_TYPE = "SORT_TYPE"

        fun start(context: Context) {
            context.startActivity<PlayersActivity>()
        }

        fun startByPlayCount(context: Context) {
            context.startActivity<PlayersActivity>(KEY_SORT_TYPE to Player.SortType.PLAY_COUNT)
        }
    }
}
