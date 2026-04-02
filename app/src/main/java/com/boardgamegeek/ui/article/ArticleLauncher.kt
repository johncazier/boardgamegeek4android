package com.boardgamegeek.ui.article

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.ArticleRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber

object ArticleLauncher {
    fun start(
        context: Context,
        threadId: Int,
        threadSubject: String?,
        forumId: Int,
        forumTitle: String?,
        objectId: Int,
        objectName: String?,
        objectType: Forum.Type?,
        article: Article?,
    ) {
        if (article == null || article.id == BggContract.INVALID_ID) {
            Timber.w("Invalid article ID")
            return
        }
        context.startActivity(
            MainActivity.createIntent(
                context = context,
                route = ArticleRoute(
                    threadId = threadId,
                    threadSubject = threadSubject.orEmpty(),
                    forumId = forumId,
                    forumTitle = forumTitle.orEmpty(),
                    objectId = objectId,
                    objectName = objectName.orEmpty(),
                    objectType = objectType?.name ?: Forum.Type.REGION.name,
                    articleId = article.id,
                    articleUsername = article.username,
                    articleLink = article.link,
                    articlePostTicks = article.postTicks,
                    articleEditTicks = article.editTicks,
                    articleBody = article.body,
                    articleNumberOfEdits = article.numberOfEdits,
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleRouteScreen(route: ArticleRoute) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val article = remember(route) {
        Article(
            id = route.articleId,
            username = route.articleUsername,
            link = route.articleLink,
            postTicks = route.articlePostTicks,
            editTicks = route.articleEditTicks,
            body = route.articleBody,
            numberOfEdits = route.articleNumberOfEdits,
        )
    }

    LaunchedEffect(route.articleId) {
        if (route.articleId != BggContract.INVALID_ID) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
                param(FirebaseAnalytics.Param.ITEM_ID, route.articleId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, route.threadSubject)
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { ArticleTitle(route.threadSubject, route.forumTitle, route.objectName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { context.link(article.link) }) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = stringResource(R.string.menu_view_in_browser),
                            )
                        }
                        IconButton(onClick = { context.shareArticle(route, article) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.menu_share),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            ArticleScreen(article = article, paddingValues = paddingValues)
        }
    }
}

private fun Context.shareArticle(route: ArticleRoute, article: Article) {
    val description = if (route.objectName.isEmpty()) {
        getString(R.string.share_thread_article_text, route.threadSubject, route.forumTitle)
    } else {
        getString(R.string.share_thread_article_object_text, route.threadSubject, route.forumTitle, route.objectName)
    }
    val message = """
        $description

        ${article.link}
    """.trimIndent()
    (this as? android.app.Activity)?.share(getString(R.string.share_thread_subject), message, R.string.title_share)
    FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SHARE) {
        param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
        param(
            FirebaseAnalytics.Param.ITEM_NAME,
            if (route.objectName.isEmpty()) {
                "${route.forumTitle} | ${route.threadSubject}"
            } else {
                "${route.objectName} | ${route.forumTitle} | ${route.threadSubject}"
            },
        )
        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
    }
}

@Composable
private fun ArticleTitle(threadSubject: String, forumTitle: String, objectName: String) {
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
