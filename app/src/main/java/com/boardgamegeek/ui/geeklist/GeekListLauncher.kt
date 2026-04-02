package com.boardgamegeek.ui.geeklist

import android.app.Activity
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.SearchRoute
import com.boardgamegeek.ui.navigation.GeekListRoute
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

object GeekListLauncher {
    fun start(context: Context, id: Int, title: String) {
        context.startActivity(createIntent(context, id, title))
    }

    private fun createIntent(context: Context, id: Int, title: String) = MainActivity.createIntent(
        context = context,
        route = GeekListRoute(
            geekListId = id,
            geekListTitle = title,
        ),
    )
}

@Composable
fun GeekListRouteScreen(
    route: GeekListRoute,
    viewModel: GeekListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(route.geekListId, route.geekListTitle) {
        if (route.geekListId != BggContract.INVALID_ID) {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                param(FirebaseAnalytics.Param.ITEM_ID, route.geekListId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.geekListTitle)
            }
        }
        viewModel.setId(route.geekListId)
    }

    val geekListResource by viewModel.geekList.collectAsState(initial = RefreshableResource.refreshing())
    var currentGeekListTitle by remember(route.geekListTitle) { mutableStateOf(route.geekListTitle) }

    if (geekListResource.status == Status.SUCCESS) {
        geekListResource.data?.let {
            currentGeekListTitle = it.title
        }
    }

    AppScreen(
        topBarTitle = currentGeekListTitle,
        currentScreenRouteFromActivity = "",
        onSearchClick = {
            navigator.navigate(SearchRoute())
        },
        topBarActions = {
            IconButton(onClick = {
                context.linkToBgg("geeklist", route.geekListId)
            }) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = context.getString(R.string.menu_view),
                )
            }
            IconButton(onClick = {
                val description = context.getString(R.string.share_geeklist_text, currentGeekListTitle)
                val uri = createBggUri("geeklist", route.geekListId)
                (context as? Activity)?.share(
                    context.getString(R.string.share_geeklist_subject),
                    "$description\n\n$uri",
                )

                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                    param(FirebaseAnalytics.Param.ITEM_ID, route.geekListId.toString())
                    param(FirebaseAnalytics.Param.ITEM_NAME, currentGeekListTitle)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.menu_share),
                )
            }
        },
    ) { paddingValues ->
        GeekListScreen(
            viewModel = viewModel,
            paddingValues = paddingValues,
        )
    }
}
