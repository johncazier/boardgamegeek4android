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
