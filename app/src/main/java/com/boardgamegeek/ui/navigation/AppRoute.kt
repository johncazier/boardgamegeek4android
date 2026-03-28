package com.boardgamegeek.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object CollectionRoute : AppRoute

@Serializable
data object HotnessRoute : AppRoute

@Serializable
data object TopGamesRoute : AppRoute

@Serializable
data object GeekListsRoute : AppRoute

@Serializable
data object CollectionDetailsRoute : AppRoute

@Serializable
data object PlaysSummaryRoute : AppRoute

@Serializable
data object BuddiesRoute : AppRoute

@Serializable
data object ForumsRoute : AppRoute

@Serializable
data object SyncRoute : AppRoute

@Serializable
data object DataRoute : AppRoute

@Serializable
data object SettingsRoute : AppRoute

@Serializable
data class SearchRoute(
    val query: String = "",
) : AppRoute

@Serializable
data class GameRoute(
    val gameId: Int,
    val gameName: String = "",
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
) : AppRoute
