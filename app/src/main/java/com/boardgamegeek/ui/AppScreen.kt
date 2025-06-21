package com.boardgamegeek.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.AppBottomNavigationBar
import com.boardgamegeek.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    topBarTitle: String,
    initialSelectedRoute: String,
    onNavigate: (route: String) -> Unit,
    onSearchClick: () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit = {}, // Default to no extra actions
    content: @Composable (PaddingValues) -> Unit
) {
    AppTheme {
        var selectedRoute by remember { mutableStateOf(initialSelectedRoute) }

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.menu_search)
                            )
                        }
                        topBarActions() // Allow additional actions
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                AppBottomNavigationBar(
                    currentRoute = selectedRoute,
                    onItemSelected = { route ->
                        selectedRoute = route
                        onNavigate(route)
                    }
                )
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}