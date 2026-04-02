package com.boardgamegeek.ui.buddycollection

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createStatusMap
import com.boardgamegeek.mappers.mapFromResourceToEnum
import com.boardgamegeek.mappers.mapToResource
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.BuddyCollectionRoute
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyCollectionRouteScreen(
    route: BuddyCollectionRoute,
    viewModel: BuddyCollectionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val status by viewModel.status.collectAsStateWithLifecycle()
    val collectionResource by viewModel.collection.collectAsStateWithLifecycle()
    val statuses = remember(context) { context.createStatusMap() }
    var showFilterMenu by remember { mutableStateOf(false) }

    val statusLabel = statuses[status.mapToResource()].orEmpty()
    val collection = collectionResource?.data.orEmpty()

    LaunchedEffect(route.buddyName) {
        viewModel.setUsername(route.buddyName)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
            param(FirebaseAnalytics.Param.ITEM_ID, route.buddyName)
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = if (statusLabel.isBlank()) route.buddyName else "${route.buddyName} - $statusLabel")
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
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.menu_collection_status_),
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            statuses.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        viewModel.setStatus(key.mapFromResourceToEnum())
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                        if (collection.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    collection.randomOrNull()?.let { randomItem ->
                                        navigator.navigate(GameRoute(randomItem.gameId, randomItem.gameName, randomItem.thumbnailUrl))
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = stringResource(R.string.menu_collection_random_game),
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            BuddyCollectionScreen(
                resource = collectionResource,
                paddingValues = paddingValues,
                onGameClick = { item ->
                    navigator.navigate(GameRoute(item.gameId, item.gameName, item.thumbnailUrl))
                },
            )
        }
    }
}
