package com.boardgamegeek.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.ui.graphics.vector.ImageVector
import com.boardgamegeek.R
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
        BottomBarItem(
            route = BottomNavItem.Collection.route,
            titleRes = R.string.title_collection,
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
        ),
        BottomBarItem(
            route = BottomNavItem.Hotness.route,
            titleRes = BottomNavItem.Hotness.titleRes,
            icon = BottomNavItem.Hotness.icon,
        ),
        BottomBarItem(
            route = BottomNavItem.TopGames.route,
            titleRes = BottomNavItem.TopGames.titleRes,
            icon = BottomNavItem.TopGames.icon,
        ),
        BottomBarItem(
            route = BottomNavItem.GeekLists.route,
            titleRes = BottomNavItem.GeekLists.titleRes,
            icon = BottomNavItem.GeekLists.icon,
        ),
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

private data class BottomBarItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
)

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
