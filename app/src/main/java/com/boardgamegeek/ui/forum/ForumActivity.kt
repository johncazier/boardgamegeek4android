package com.boardgamegeek.ui.forum

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.ForumRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object ForumActivity {
    fun start(
        context: Context,
        forumId: Int,
        forumTitle: String,
        objectId: Int,
        objectName: String,
        objectType: Forum.Type,
    ) {
        context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType))
    }

    fun startUp(
        context: Context,
        forumId: Int,
        forumTitle: String,
        objectId: Int,
        objectName: String,
        objectType: Forum.Type,
    ) {
        context.startActivity(
            createIntent(
                context = context,
                forumId = forumId,
                forumTitle = forumTitle,
                objectId = objectId,
                objectName = objectName,
                objectType = objectType,
                replaceBackStack = true,
            ).clearTop(),
        )
    }

    private fun createIntent(
        context: Context,
        forumId: Int,
        forumTitle: String,
        objectId: Int,
        objectName: String,
        objectType: Forum.Type,
        replaceBackStack: Boolean = false,
    ) = MainActivity.createIntent(
        context = context,
        route = ForumRoute(
            forumId = forumId,
            forumTitle = forumTitle,
            objectId = objectId,
            objectName = objectName,
            objectType = objectType.name,
        ),
        replaceBackStack = replaceBackStack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumRouteScreen(
    route: ForumRoute,
    viewModel: ForumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val objectType = remember(route.objectType) { route.objectType.asForumType() }

    LaunchedEffect(route.forumId) {
        if (route.forumId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forum")
                param(FirebaseAnalytics.Param.ITEM_ID, route.forumId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.forumTitle)
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { ForumTitle(objectName = route.objectName, forumTitle = route.forumTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { context.linkToBgg("forum/${route.forumId}") }) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = stringResource(R.string.menu_view),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                ForumScreen(
                    viewModel = viewModel,
                    forumId = route.forumId,
                    forumTitle = route.forumTitle,
                    objectId = route.objectId,
                    objectName = route.objectName,
                    objectType = objectType,
                )
            }
        }
    }
}

private fun String.asForumType(): Forum.Type = Forum.Type.entries.firstOrNull { it.name == this } ?: Forum.Type.REGION

@Composable
private fun ForumTitle(objectName: String, forumTitle: String) {
    if (objectName.isNotBlank()) {
        Column {
            Text(text = objectName)
            Text(text = forumTitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    } else {
        Text(text = forumTitle)
    }
}
