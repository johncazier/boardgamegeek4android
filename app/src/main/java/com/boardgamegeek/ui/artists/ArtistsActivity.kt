package com.boardgamegeek.ui.artists

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Person
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class ArtistsActivity : ComponentActivity() {
    private val viewModel by viewModels<ArtistsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val artists by viewModel.artists.collectAsStateWithLifecycle()
                val sortType by viewModel.sortType.collectAsStateWithLifecycle()
                var showSortMenu by remember { mutableStateOf(false) }
                var showOverflowMenu by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.title_artists)) },
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
                                    text = (artists?.size ?: 0).toString(),
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
                                                viewModel.sort(Person.SortType.NAME)
                                                showSortMenu = false
                                            },
                                            trailingIcon = {
                                                if (sortType == Person.SortType.NAME) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_sort_item_count)) },
                                            onClick = {
                                                viewModel.sort(Person.SortType.ITEM_COUNT)
                                                showSortMenu = false
                                            },
                                            trailingIcon = {
                                                if (sortType == Person.SortType.ITEM_COUNT) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_sort_whitmore_score)) },
                                            onClick = {
                                                viewModel.sort(Person.SortType.WHITMORE_SCORE)
                                                showSortMenu = false
                                            },
                                            trailingIcon = {
                                                if (sortType == Person.SortType.WHITMORE_SCORE) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_refresh)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                viewModel.refresh()
                                                showOverflowMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_search)) },
                                            onClick = {
                                                startActivity(Intent(this@ArtistsActivity, SearchResultsActivity::class.java))
                                                showOverflowMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    ArtistsScreen(
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                        onArtistClick = { artist ->
                            PersonActivity.startForArtist(this, artist.id, artist.name)
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity<ArtistsActivity>()
        }
    }
}
