package com.boardgamegeek.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data class CollectionRoute(
    val initialViewId: Int = 0,
    val changingGamePlayId: Long = -1L,
    val isCreatingShortcut: Boolean = false,
) : AppRoute

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
data class LoginRoute(
    val username: String? = null,
) : AppRoute

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

@Serializable
data class GeekBuddyAnalysisRoute(
    val gameId: Int,
    val gameName: String = "",
) : AppRoute

@Serializable
data class PersonRoute(
    val personId: Int,
    val personName: String = "",
    val personType: String = "DESIGNER",
) : AppRoute

@Serializable
data class ForumRoute(
    val forumId: Int,
    val forumTitle: String = "",
    val objectId: Int = 0,
    val objectName: String = "",
    val objectType: String = "REGION",
) : AppRoute

@Serializable
data class ThreadRoute(
    val threadId: Int,
    val threadSubject: String = "",
    val forumId: Int = 0,
    val forumTitle: String = "",
    val objectId: Int = 0,
    val objectName: String = "",
    val objectType: String = "REGION",
) : AppRoute

@Serializable
data class ArticleRoute(
    val threadId: Int,
    val threadSubject: String = "",
    val forumId: Int = 0,
    val forumTitle: String = "",
    val objectId: Int = 0,
    val objectName: String = "",
    val objectType: String = "REGION",
    val articleId: Int,
    val articleUsername: String = "",
    val articleLink: String = "",
    val articlePostTicks: Long = 0,
    val articleEditTicks: Long = 0,
    val articleBody: String = "",
    val articleNumberOfEdits: Int = 0,
) : AppRoute

@Serializable
data class ImageRoute(
    val imageUrl: String,
) : AppRoute

@Serializable
data object DesignersRoute : AppRoute

@Serializable
data object ArtistsRoute : AppRoute

@Serializable
data object PublishersRoute : AppRoute

@Serializable
data object CategoriesRoute : AppRoute

@Serializable
data object MechanicsRoute : AppRoute

@Serializable
data class GeekListRoute(
    val geekListId: Int,
    val geekListTitle: String = "",
) : AppRoute

@Serializable
data class GeekListCommentRoute(
    val postDate: Long = 0L,
    val editDate: Long = 0L,
    val numberOfThumbs: Int = 0,
    val username: String = "",
    val content: String = "",
)

@Serializable
data class GeekListItemRoute(
    val geekListId: Int,
    val geekListTitle: String = "",
    val order: Int = 0,
    val itemId: Long = 0L,
    val objectId: Int = 0,
    val objectName: String = "",
    val objectType: String = "",
    val subtype: String = "",
    val imageId: Int = 0,
    val username: String = "",
    val body: String = "",
    val numberOfThumbs: Int = 0,
    val postDateTime: Long = 0L,
    val editDateTime: Long = 0L,
    val comments: List<GeekListCommentRoute> = emptyList(),
    val thumbnailUrls: List<String> = emptyList(),
    val heroImageUrls: List<String> = emptyList(),
) : AppRoute

@Serializable
data class MechanicRoute(
    val mechanicId: Int,
    val mechanicName: String = "",
) : AppRoute

@Serializable
data class CategoryRoute(
    val categoryId: Int,
    val categoryName: String = "",
) : AppRoute

@Serializable
data class BuddyRoute(
    val username: String? = null,
    val playerName: String? = null,
) : AppRoute

@Serializable
data class BuddyCollectionRoute(
    val buddyName: String,
) : AppRoute

@Serializable
data class BuddyPlaysRoute(
    val buddyName: String,
) : AppRoute

@Serializable
data class PlayersRoute(
    val sortType: String = "NAME",
) : AppRoute

@Serializable
data object LocationsRoute : AppRoute

@Serializable
data object PlayStatsRoute : AppRoute

@Serializable
data object PlaysRoute : AppRoute

@Serializable
data class CommentsRoute(
    val gameId: Int,
    val gameName: String = "",
    val sortType: Int = 0,
) : AppRoute

@Serializable
data class GameDetailRoute(
    val title: String = "",
    val gameId: Int,
    val gameName: String = "",
    val producerType: String = "UNKNOWN",
) : AppRoute

@Serializable
data class GameColorsRoute(
    val gameId: Int,
    val gameName: String = "",
    val iconColor: Int = 0,
) : AppRoute

@Serializable
data class PlayerColorsRoute(
    val buddyName: String? = null,
    val playerName: String? = null,
) : AppRoute

@Serializable
data class GamePlaysRoute(
    val gameId: Int,
    val gameName: String = "",
    val heroImageUrl: String = "",
    val thumbnailUrl: String = "",
    val arePlayersCustomSorted: Boolean = false,
    val iconColor: Int = 0,
) : AppRoute

@Serializable
data class PlayerPlaysRoute(
    val playerName: String = "",
) : AppRoute

@Serializable
data class LocationRoute(
    val locationName: String = "",
) : AppRoute

@Serializable
data class LogPlayRoute(
    val internalId: Long = -1L,
    val gameId: Int,
    val gameName: String = "",
    val heroImageUrl: String = "",
    val customPlayerSort: Boolean = false,
    val isRequestingToEndPlay: Boolean = false,
    val isRequestingRematch: Boolean = false,
    val isChangingGame: Boolean = false,
) : AppRoute

@Serializable
data class LogPlayerRoute(
    val requestId: String,
    val gameId: Int,
    val gameName: String = "",
    val heroImageUrl: String = "",
    val isRequestingToEndPlay: Boolean = false,
    val usedColors: List<String> = emptyList(),
    val autoPosition: Int = -1,
    val playerPosition: Int = -1,
    val isNewPlayer: Boolean = false,
    val player: LogPlayerPayload = LogPlayerPayload(),
) : AppRoute

@Serializable
data class LogPlayerPayload(
    val name: String = "",
    val username: String = "",
    val startingPosition: String = "",
    val color: String = "",
    val score: String = "",
    val rating: Double = 0.0,
    val userId: Int? = -1,
    val isNew: Boolean = false,
    val isWin: Boolean = false,
    val playInternalId: Long = -1L,
    val uiId: Long = 0L,
    val internalId: Long = -1L,
)

@Serializable
data class PlayRoute(
    val internalId: Long,
) : AppRoute

@Serializable
data class GamePlayStatsRoute(
    val gameId: Int,
    val gameName: String = "",
    val headerColor: Int = 0,
) : AppRoute

@Serializable
data class GameCollectionItemRoute(
    val internalId: Long,
    val gameId: Int = 0,
    val gameName: String = "",
    val collectionId: Int = 0,
    val collectionName: String = "",
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val gameYearPublished: Int = 0,
    val collectionYearPublished: Int = 0,
) : AppRoute
