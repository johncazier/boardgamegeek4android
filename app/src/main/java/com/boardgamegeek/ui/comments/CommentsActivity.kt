package com.boardgamegeek.ui.comments

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.CommentsRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object CommentsActivity {
    const val SORT_TYPE_USER = 0
    const val SORT_TYPE_RATING = 1

    fun startRating(context: Context, gameId: Int, gameName: String) {
        context.startActivity(
            MainActivity.createIntent(
                context = context,
                route = CommentsRoute(
                    gameId = gameId,
                    gameName = gameName,
                    sortType = SORT_TYPE_RATING,
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsRouteScreen(
    route: CommentsRoute,
    viewModel: GameCommentsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val initialSort = if (route.sortType == CommentsActivity.SORT_TYPE_USER) {
        GameCommentsViewModel.SortType.USER
    } else {
        GameCommentsViewModel.SortType.RATING
    }

    LaunchedEffect(route.gameId, route.sortType) {
        viewModel.setGameId(route.gameId)
        viewModel.setSort(initialSort)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameComments")
            param(FirebaseAnalytics.Param.ITEM_ID, route.gameId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.gameName)
        }
    }

    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (sort == GameCommentsViewModel.SortType.RATING) {
                                route.gameName.ifEmpty { stringResource(R.string.title_ratings) }
                            } else {
                                route.gameName.ifEmpty { stringResource(R.string.title_comments) }
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
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
                                    text = { Text(stringResource(R.string.title_comments)) },
                                    onClick = {
                                        viewModel.setSort(GameCommentsViewModel.SortType.USER)
                                        showSortMenu = false
                                    },
                                    trailingIcon = if (sort == GameCommentsViewModel.SortType.USER) ({ Text("\u2713") }) else null,
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_ratings)) },
                                    onClick = {
                                        viewModel.setSort(GameCommentsViewModel.SortType.RATING)
                                        showSortMenu = false
                                    },
                                    trailingIcon = if (sort == GameCommentsViewModel.SortType.RATING) ({ Text("\u2713") }) else null,
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                CommentsScreen(viewModel = viewModel)
            }
        }
    }
}
