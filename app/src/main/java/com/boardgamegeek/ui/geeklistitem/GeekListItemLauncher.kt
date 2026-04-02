package com.boardgamegeek.ui.geeklistitem

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.link
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.GeekListCommentRoute
import com.boardgamegeek.ui.navigation.GeekListItemRoute
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

@Composable
fun GeekListItemRouteScreen(route: GeekListItemRoute) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val geekListItem = route.toGeekListItem()

    LaunchedEffect(route.objectId, route.objectName) {
        if (route.objectId != BggContract.INVALID_ID) {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekListItem")
                param(FirebaseAnalytics.Param.ITEM_ID, route.objectId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.objectName)
            }
        }
    }

    AppScreen(
        topBarTitle = geekListItem.objectName,
        currentScreenRouteFromActivity = "",
        onSearchClick = {
            navigator.navigate(SearchRoute())
        },
        topBarActions = {
            IconButton(onClick = {
                if (geekListItem.isBoardGame) {
                    if (geekListItem.objectId != BggContract.INVALID_ID && geekListItem.objectName.isNotBlank()) {
                        navigator.navigate(GameRoute(geekListItem.objectId, geekListItem.objectName))
                    }
                } else if (geekListItem.objectUrl.isNotBlank()) {
                    context.link(geekListItem.objectUrl)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(R.string.menu_view),
                )
            }
        },
    ) { paddingValues ->
        GeekListItemScreen(
            geekListItem = geekListItem,
            geekListTitle = route.geekListTitle,
            order = route.order,
            paddingValues = paddingValues,
        )
    }
}

fun GeekListItem.toRoute(
    geekListId: Int,
    geekListTitle: String,
    order: Int,
) = GeekListItemRoute(
    geekListId = geekListId,
    geekListTitle = geekListTitle,
    order = order,
    itemId = id,
    objectId = objectId,
    objectName = objectName,
    objectType = routeObjectType(),
    subtype = routeSubtype(),
    imageId = imageId,
    username = username,
    body = body,
    numberOfThumbs = numberOfThumbs,
    postDateTime = postDateTime,
    editDateTime = editDateTime,
    comments = comments.map { comment ->
        GeekListCommentRoute(
            postDate = comment.postDate,
            editDate = comment.editDate,
            numberOfThumbs = comment.numberOfThumbs,
            username = comment.username,
            content = comment.content,
        )
    },
    thumbnailUrls = thumbnailUrls.orEmpty(),
    heroImageUrls = heroImageUrls.orEmpty(),
)

private fun GeekListItemRoute.toGeekListItem() = GeekListItem(
    id = itemId,
    objectId = objectId,
    objectName = objectName,
    objectType = objectType,
    subtype = subtype,
    imageId = imageId,
    username = username,
    body = body,
    numberOfThumbs = numberOfThumbs,
    postDateTime = postDateTime,
    editDateTime = editDateTime,
    comments = comments.map { comment ->
        GeekListComment(
            postDate = comment.postDate,
            editDate = comment.editDate,
            numberOfThumbs = comment.numberOfThumbs,
            username = comment.username,
            content = comment.content,
        )
    },
    thumbnailUrls = thumbnailUrls,
    heroImageUrls = heroImageUrls,
)

private fun GeekListItem.routeObjectType(): String {
    return when {
        objectUrl.contains("/thing/") -> "thing"
        objectUrl.contains("/company/") -> "company"
        objectUrl.contains("/person/") -> "person"
        objectUrl.contains("/family/") -> "family"
        objectUrl.contains("/filepage/") -> "filepage"
        objectUrl.contains("/geeklist/") -> "geeklist"
        else -> ""
    }
}

private fun GeekListItem.routeSubtype(): String {
    val match = Regex("boardgameaccessory|boardgamepublisher|boardgamedesigner|boardgamefamily|boardgame").find(objectUrl)
    return match?.value.orEmpty()
}
