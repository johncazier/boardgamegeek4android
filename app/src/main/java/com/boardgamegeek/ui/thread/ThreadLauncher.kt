package com.boardgamegeek.ui.thread

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.ThreadRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.flow.MutableSharedFlow

object ThreadLauncher {
    fun start(
        context: Context,
        threadId: Int,
        threadSubject: String,
        forumId: Int,
        forumTitle: String,
        objectId: Int,
        objectName: String,
        objectType: Forum.Type,
    ) {
        context.startActivity(createIntent(context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType))
    }

    private fun createIntent(
        context: Context,
        threadId: Int,
        threadSubject: String,
        forumId: Int,
        forumTitle: String,
        objectId: Int,
        objectName: String,
        objectType: Forum.Type,
        replaceBackStack: Boolean = false,
    ) = MainActivity.createIntent(
        context = context,
        route = ThreadRoute(
            threadId = threadId,
            threadSubject = threadSubject,
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
fun ThreadRouteScreen(
    route: ThreadRoute,
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val scrollCommands = remember { MutableSharedFlow<ThreadScrollCommand>(extraBufferCapacity = 1) }
    val listState = rememberLazyListState()
    var latestArticleId by rememberSaveable { mutableIntStateOf(BggContract.INVALID_ID) }
    var articleCount by rememberSaveable { mutableIntStateOf(0) }
    val objectType = remember(route.objectType) { route.objectType.asForumType() }

    LaunchedEffect(route.threadId) {
        if (route.threadId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
                param(FirebaseAnalytics.Param.ITEM_ID, route.threadId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.threadSubject)
            }
        }
    }

    LifecycleResumeEffect(route.threadId) {
        latestArticleId = context.getThreadKey(route.threadId)?.let { key ->
            context.preferences()[key, BggContract.INVALID_ID] ?: BggContract.INVALID_ID
        } ?: BggContract.INVALID_ID

        onPauseOrDispose {
            if (latestArticleId != BggContract.INVALID_ID) {
                context.getThreadKey(route.threadId)?.let { key ->
                    context.preferences()[key] = latestArticleId
                }
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        ThreadTitle(
                            threadSubject = route.threadSubject,
                            forumTitle = route.forumTitle,
                            objectName = route.objectName,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        if (latestArticleId != BggContract.INVALID_ID && articleCount > 0) {
                            IconButton(onClick = { scrollCommands.tryEmit(ThreadScrollCommand.ScrollToLatest(latestArticleId)) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_south_24),
                                    contentDescription = stringResource(R.string.menu_scroll_to_last_read),
                                )
                            }
                        }
                        if (articleCount > 0) {
                            IconButton(onClick = { scrollCommands.tryEmit(ThreadScrollCommand.ScrollToBottom) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_vertical_align_bottom_24),
                                    contentDescription = stringResource(R.string.menu_scroll_to_bottom),
                                )
                            }
                        }
                        IconButton(onClick = { context.linkToBgg("thread", route.threadId) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_open_in_browser_24),
                                contentDescription = stringResource(R.string.menu_view_in_browser),
                            )
                        }
                        IconButton(onClick = { context.shareThread(route, createBggUri("thread", route.threadId).toString()) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_share_24),
                                contentDescription = stringResource(R.string.menu_share),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                ThreadScreen(
                    viewModel = viewModel,
                    threadId = route.threadId,
                    forumId = route.forumId,
                    forumTitle = route.forumTitle,
                    objectId = route.objectId,
                    objectName = route.objectName,
                    objectType = objectType,
                    listState = listState,
                    scrollCommands = scrollCommands,
                    onLatestArticleSeen = { articleId ->
                        if (articleId > latestArticleId) {
                            latestArticleId = articleId
                        }
                    },
                    onArticleCountChanged = { articleCount = it },
                )
            }
        }
    }
}

private fun Context.shareThread(route: ThreadRoute, link: String) {
    val description = if (route.objectName.isBlank()) {
        getString(R.string.share_thread_text, route.threadSubject, route.forumTitle)
    } else {
        getString(R.string.share_thread_game_text, route.threadSubject, route.forumTitle, route.objectName)
    }
    (this as? android.app.Activity)?.share(
        getString(R.string.share_thread_subject),
        """
        $description

        $link
        """.trimIndent(),
        R.string.title_share,
    )
}

private fun Context.getThreadKey(threadId: Int): String? {
    if (threadId == BggContract.INVALID_ID) return null
    return "THREAD-$threadId"
}

private fun String.asForumType(): Forum.Type = Forum.Type.entries.firstOrNull { it.name == this } ?: Forum.Type.REGION

@Composable
private fun ThreadTitle(threadSubject: String, forumTitle: String, objectName: String) {
    if (objectName.isBlank()) {
        Column {
            Text(text = forumTitle)
            Text(text = threadSubject, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    } else {
        Column {
            Text(text = "$threadSubject - $forumTitle")
            Text(text = objectName, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
