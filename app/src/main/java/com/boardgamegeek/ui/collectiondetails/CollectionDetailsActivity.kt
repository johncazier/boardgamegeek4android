package com.boardgamegeek.ui.collectiondetails

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.SearchResultsActivity
import com.boardgamegeek.ui.collection.CollectionActivity
import com.boardgamegeek.ui.navigation.BottomNavItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailsActivity : ComponentActivity() {
    private val viewModel by viewModels<CollectionDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.refresh()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val loggedPlayResult by viewModel.loggedPlayResultFlow.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessageFlow.collectAsStateWithLifecycle()

            LaunchedEffect(loggedPlayResult) {
                loggedPlayResult?.let {
                    notifyLoggedPlay(it)
                    viewModel.clearLoggedPlayResult()
                }
            }

            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
                    viewModel.clearErrorMessage()
                }
            }

            AppScreen(
                topBarTitle = stringResource(R.string.title_collection_details),
                currentScreenRouteFromActivity = BottomNavItem.Collection.route,
                onSearchClick = { startActivity(intentFor<SearchResultsActivity>()) },
                drawerGesturesEnabled = false,
                snackbarHostState = snackbarHostState,
                topBarActions = {
                    IconButton(
                        onClick = { startActivity(intentFor<CollectionActivity>()) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CollectionsBookmark,
                            contentDescription = stringResource(R.string.title_collection)
                        )
                    }
                }
            ) { paddingValues ->
                CollectionDetailsScreen(viewModel = viewModel, paddingValues = paddingValues)
            }
        }
    }
}
