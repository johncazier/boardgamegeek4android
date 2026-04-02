package com.boardgamegeek.ui.collectiondetails

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.DrawerRoute
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator

@Composable
fun CollectionDetailsRouteScreen(
    viewModel: CollectionDetailsViewModel = hiltViewModel(),
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val loggedPlayResult by viewModel.loggedPlayResultFlow.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessageFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(loggedPlayResult) {
        loggedPlayResult?.let {
            context.notifyLoggedPlay(it)
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
        currentDrawerRouteFromActivity = DrawerRoute.CollectionDetails,
        drawerGesturesEnabled = false,
        snackbarHostState = snackbarHostState,
        topBarActions = {
            IconButton(
                onClick = { navigator.navigateTopLevel(CollectionRoute()) },
            ) {
                Icon(
                    imageVector = Icons.Filled.CollectionsBookmark,
                    contentDescription = stringResource(R.string.title_collection),
                )
            }
        },
    ) { paddingValues ->
        CollectionDetailsScreen(viewModel = viewModel, paddingValues = paddingValues)
    }
}
