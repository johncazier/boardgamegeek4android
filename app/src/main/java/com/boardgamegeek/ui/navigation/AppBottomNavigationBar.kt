package com.boardgamegeek.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    onItemSelected: (route: String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Collection,
        BottomNavItem.Hotness,
        BottomNavItem.TopGames,
        BottomNavItem.GeekLists
    )

    NavigationBar { // This is the Material 3 Bottom Navigation Bar
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                label = { Text(stringResource(screen.titleRes)) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) { // Avoid re-selecting the same item
                        onItemSelected(screen.route)
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewAppBottomNavigationBar() {
    var selectedRoute by remember { mutableStateOf(BottomNavItem.Hotness.route) }
    MaterialTheme {
        AppBottomNavigationBar(currentRoute = selectedRoute) { route ->
            selectedRoute = route
        }
    }
}