package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.buddies.BuddiesActivity
import com.boardgamegeek.ui.data.DataActivity
import com.boardgamegeek.ui.collection.CollectionActivity
import com.boardgamegeek.ui.geeklists.GeekListsActivity
import com.boardgamegeek.ui.sync.SyncActivity
import com.boardgamegeek.ui.playssummary.PlaysSummaryActivity
import com.boardgamegeek.ui.hotness.HotnessActivity
import com.boardgamegeek.ui.navigation.AppBottomNavigationBar
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.ui.topgames.TopGamesActivity
import kotlinx.coroutines.launch
import java.util.Locale

// Helper extension function for starting activities from Context
inline fun <reified T : Activity> Context.startActivity(noinline init: (Intent.() -> Unit)? = null) {
    val intent = Intent(this, T::class.java)
    if (init != null) {
        intent.init()
    }
    startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    topBarTitle: String,
    topBarTitleContent: (@Composable () -> Unit)? = null,
    currentScreenRouteFromActivity: String,
    currentDrawerRouteFromActivity: String = currentScreenRouteFromActivity,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSearchClick: () -> Unit,
    drawerGesturesEnabled: Boolean = true,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current

    var selectedRoute by remember { mutableStateOf(currentScreenRouteFromActivity) }

    val navigateToScreen = remember<(String) -> Unit> {
        { route ->
            val currentActivityClass = (context as? ComponentActivity)?.javaClass
            val alreadyOnTarget = when (route) {
                BottomNavItem.Collection.route -> currentActivityClass == CollectionActivity::class.java
                BottomNavItem.Hotness.route -> currentActivityClass == HotnessActivity::class.java
                BottomNavItem.TopGames.route -> currentActivityClass == TopGamesActivity::class.java
                BottomNavItem.GeekLists.route -> currentActivityClass == GeekListsActivity::class.java
                else -> false
            }

            if (selectedRoute == route && alreadyOnTarget) {
                return@remember
            }

            // Update the selectedRoute for the BottomNav immediately
            selectedRoute = route

            when (route) {
                BottomNavItem.Collection.route -> {
                    if ((context as? ComponentActivity)?.javaClass != CollectionActivity::class.java) {
                        context.startActivity<CollectionActivity>()
                    }
                }
                BottomNavItem.Hotness.route -> {
                    if ((context as? ComponentActivity)?.javaClass != HotnessActivity::class.java) {
                        context.startActivity<HotnessActivity>()
                    }
                }
                BottomNavItem.TopGames.route -> context.startActivity<TopGamesActivity>()
                BottomNavItem.GeekLists.route -> context.startActivity<GeekListsActivity>()
                // Add other navigation cases as needed
            }
        }
    }

    AppTheme {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("BoardGameGeek", modifier = Modifier.padding(16.dp))
                    listOf(
                        BottomNavItem.Collection,
                        BottomNavItem.Hotness,
                        BottomNavItem.TopGames,
                        BottomNavItem.GeekLists
                    ).forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.titleRes)) },
                            selected = item.route == currentDrawerRouteFromActivity,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateToScreen(item.route)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_collection_details)) },
                        selected = currentDrawerRouteFromActivity == DrawerRoute.CollectionDetails,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if ((context as? ComponentActivity)?.javaClass != com.boardgamegeek.ui.collectiondetails.CollectionDetailsActivity::class.java) {
                                context.startActivity<com.boardgamegeek.ui.collectiondetails.CollectionDetailsActivity>()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_plays)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity<PlaysSummaryActivity>()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_buddies)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity<BuddiesActivity>()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_sync)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity<SyncActivity>()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.FileCopy, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_backup)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity<DataActivity>()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity<SettingsActivity>()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            },
            gesturesEnabled = drawerGesturesEnabled
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { (topBarTitleContent ?: { Text(topBarTitle) })() },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.menu_open_drawer)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.menu_search)
                                )
                            }
                            topBarActions()
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                },
                bottomBar = {
                    AppBottomNavigationBar(
                        currentRoute = selectedRoute,
                        onItemSelected = { route ->
                            navigateToScreen(route)
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    }
}

object DrawerRoute {
    const val CollectionDetails = "collection_details"
}
