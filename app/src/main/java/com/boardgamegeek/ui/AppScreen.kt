package com.boardgamegeek.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.AppBottomNavigationBar
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.BottomNavItem
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.CollectionRoute
import com.boardgamegeek.ui.navigation.DataRoute
import com.boardgamegeek.ui.navigation.ForumsRoute
import com.boardgamegeek.ui.navigation.GeekListsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.SettingsRoute
import com.boardgamegeek.ui.navigation.SyncRoute
import com.boardgamegeek.ui.navigation.TopGamesRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    topBarTitle: String,
    topBarTitleContent: (@Composable () -> Unit)? = null,
    currentScreenRouteFromActivity: String,
    currentDrawerRouteFromActivity: String = currentScreenRouteFromActivity,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSearchClick: (() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val navigator = LocalAppNavigator.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("BoardGameGeek", modifier = Modifier.padding(16.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_collection)) },
                        selected = currentDrawerRouteFromActivity == DrawerRoute.CollectionDetails,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigate(CollectionDetailsRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    listOf(
                        BottomNavItem.Hotness,
                        BottomNavItem.TopGames,
                        BottomNavItem.GeekLists,
                    ).forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.titleRes)) },
                            selected = item.route == currentDrawerRouteFromActivity,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigator.navigateTopLevel(item.toRoute())
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(BottomNavItem.Collection.icon, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_legacy_collection)) },
                        selected = currentDrawerRouteFromActivity == BottomNavItem.Collection.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigateTopLevel(CollectionRoute())
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_plays)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigateTopLevel(PlaysSummaryRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_buddies)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigateTopLevel(BuddiesRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Forum, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_forums)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigateTopLevel(ForumsRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_sync)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigate(SyncRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.FileCopy, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_backup)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigate(DataRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigator.navigate(SettingsRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            },
            gesturesEnabled = drawerGesturesEnabled,
        ) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { (topBarTitleContent ?: { Text(topBarTitle) })() },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.menu_open_drawer),
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick ?: { navigator.navigate(SearchRoute()) }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.menu_search),
                                )
                            }
                            topBarActions()
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                },
                bottomBar = {
                    AppBottomNavigationBar(
                        currentRoute = currentScreenRouteFromActivity,
                        onItemSelected = { route ->
                            navigator.navigateTopLevel(route.toBottomNavRoute())
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                content(paddingValues)
            }
        }
}

object DrawerRoute {
    const val CollectionDetails = "collection_details"
}

private fun BottomNavItem.toRoute() = when (this) {
    BottomNavItem.Collection -> CollectionRoute()
    BottomNavItem.Hotness -> HotnessRoute
    BottomNavItem.TopGames -> TopGamesRoute
    BottomNavItem.GeekLists -> GeekListsRoute
}

private fun String.toBottomNavRoute() = when (this) {
    BottomNavItem.Collection.route -> CollectionDetailsRoute
    BottomNavItem.TopGames.route -> TopGamesRoute
    BottomNavItem.GeekLists.route -> GeekListsRoute
    else -> HotnessRoute
}
