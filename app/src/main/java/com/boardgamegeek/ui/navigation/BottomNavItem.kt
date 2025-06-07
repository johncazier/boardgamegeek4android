package com.boardgamegeek.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.boardgamegeek.R

sealed class BottomNavItem(
    val route: String, // A unique route string for navigation (even if not using Compose Navigation yet)
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Collection : BottomNavItem("collection", R.string.title_collection, Icons.Filled.CollectionsBookmark)
    object Hotness : BottomNavItem("hotness", R.string.title_hotness, Icons.Filled.Whatshot)
    object TopGames : BottomNavItem("top_games", R.string.title_top_games, Icons.AutoMirrored.Filled.TrendingUp)
    object GeekLists : BottomNavItem("geeklists", R.string.title_geeklists, Icons.AutoMirrored.Filled.ListAlt)
}