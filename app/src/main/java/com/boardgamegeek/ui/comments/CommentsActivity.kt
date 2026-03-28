package com.boardgamegeek.ui.comments

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class CommentsActivity : ComponentActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var sortType = SORT_TYPE_USER
    private val viewModel by viewModels<GameCommentsViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameComments")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        viewModel.setGameId(gameId)
        viewModel.setSort(if (sortType == SORT_TYPE_USER) GameCommentsViewModel.SortType.USER else GameCommentsViewModel.SortType.RATING)

        setContent {
            AppTheme {
                val sort by viewModel.sort.collectAsStateWithLifecycle()
                var showSortMenu by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (sort == GameCommentsViewModel.SortType.RATING) {
                                        if (gameName.isNotEmpty()) gameName else stringResource(R.string.title_ratings)
                                    } else {
                                        if (gameName.isNotEmpty()) gameName else stringResource(R.string.title_comments)
                                    }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = ::navigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
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
                                            text = { Text(stringResource(R.string.title_comments)) },
                                            onClick = {
                                                sortType = SORT_TYPE_USER
                                                viewModel.setSort(GameCommentsViewModel.SortType.USER)
                                                showSortMenu = false
                                            },
                                            trailingIcon = {
                                                if (sort == GameCommentsViewModel.SortType.USER) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.title_ratings)) },
                                            onClick = {
                                                sortType = SORT_TYPE_RATING
                                                viewModel.setSort(GameCommentsViewModel.SortType.RATING)
                                                showSortMenu = false
                                            },
                                            trailingIcon = {
                                                if (sort == GameCommentsViewModel.SortType.RATING) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        CommentsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun readIntent() {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        sortType = intent.getIntExtra(KEY_SORT_TYPE, SORT_TYPE_USER)
    }

    private fun navigateUp() {
        GameActivity.startUp(this, gameId, gameName)
        finish()
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_SORT_TYPE = "SORT_TYPE"
        const val SORT_TYPE_USER = 0
        const val SORT_TYPE_RATING = 1

        fun startRating(context: Context, gameId: Int, gameName: String) {
            context.startActivity<CommentsActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_SORT_TYPE to SORT_TYPE_RATING,
            )
        }
    }
}
