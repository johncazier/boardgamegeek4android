package com.boardgamegeek.ui.article

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.share
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.thread.ThreadActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
class ArticleActivity : ComponentActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION
    private var article = Article()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (article.id == BggContract.INVALID_ID) {
            Timber.w("Invalid article ID")
            finish()
            return
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
                param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { ArticleTitle(threadSubject, forumTitle, objectName) },
                            navigationIcon = {
                                IconButton(onClick = ::navigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { link(article.link) }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = stringResource(R.string.menu_view_in_browser)
                                    )
                                }
                                IconButton(onClick = ::shareArticle) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.menu_share)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    ArticleScreen(article = article, paddingValues = paddingValues)
                }
            }
        }
    }

    private fun readIntent() {
        threadId = intent.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID)
        threadSubject = intent.getStringExtra(KEY_THREAD_SUBJECT).orEmpty()
        forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
        forumTitle = intent.getStringExtra(KEY_FORUM_TITLE).orEmpty()
        objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
        objectName = intent.getStringExtra(KEY_OBJECT_NAME).orEmpty()
        objectType = intent.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        article = intent.getParcelableCompat(KEY_ARTICLE) ?: Article()
    }

    private fun navigateUp() {
        ThreadActivity.startUp(this, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType)
        finish()
    }

    private fun shareArticle() {
        val description = if (objectName.isEmpty())
            String.format(getString(R.string.share_thread_article_text), threadSubject, forumTitle)
        else
            String.format(getString(R.string.share_thread_article_object_text), threadSubject, forumTitle, objectName)
        val message = """
            $description

            ${article.link}""".trimIndent()
        share(getString(R.string.share_thread_subject), message, R.string.title_share)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
            param(
                FirebaseAnalytics.Param.ITEM_NAME,
                if (objectName.isEmpty()) "$forumTitle | $threadSubject" else "$objectName | $forumTitle | $threadSubject"
            )
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
        }
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val KEY_THREAD_SUBJECT = "THREAD_SUBJECT"
        private const val KEY_ARTICLE = "ARTICLE"

        fun start(
            context: Context,
            threadId: Int,
            threadSubject: String?,
            forumId: Int,
            forumTitle: String?,
            objectId: Int,
            objectName: String?,
            objectType: Forum.Type?,
            article: Article?
        ) {
            context.startActivity<ArticleActivity>(
                KEY_THREAD_ID to threadId,
                KEY_THREAD_SUBJECT to threadSubject,
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
                KEY_ARTICLE to article,
            )
        }
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
