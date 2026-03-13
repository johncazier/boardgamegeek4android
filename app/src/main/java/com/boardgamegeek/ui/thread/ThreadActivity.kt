package com.boardgamegeek.ui.thread

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.extensions.share
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.forum.ForumActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class ThreadActivity : ComponentActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle: String = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION
    private val viewModel by viewModels<ThreadViewModel>()
    private var latestArticleId by mutableStateOf(INVALID_ARTICLE_ID)
    private var articleCount by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (objectName.isBlank()) {
            title = forumTitle
        } else {
            title = "$threadSubject - $forumTitle"
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
                param(FirebaseAnalytics.Param.ITEM_ID, threadId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }

        setContent {
            AppTheme {
                val scrollCommands = remember { MutableSharedFlow<ThreadScrollCommand>(extraBufferCapacity = 1) }
                val listState = rememberLazyListState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { ThreadTitle(threadSubject = threadSubject, forumTitle = forumTitle, objectName = objectName) },
                            navigationIcon = {
                                IconButton(onClick = ::navigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                if (latestArticleId != INVALID_ARTICLE_ID && articleCount > 0) {
                                    IconButton(onClick = { scrollCommands.tryEmit(ThreadScrollCommand.ScrollToLatest(latestArticleId)) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_south_24),
                                            contentDescription = stringResource(R.string.menu_scroll_to_last_read)
                                        )
                                    }
                                }
                                if (articleCount > 0) {
                                    IconButton(onClick = { scrollCommands.tryEmit(ThreadScrollCommand.ScrollToBottom) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_vertical_align_bottom_24),
                                            contentDescription = stringResource(R.string.menu_scroll_to_bottom)
                                        )
                                    }
                                }
                                IconButton(onClick = { linkToBgg("thread", threadId) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_baseline_open_in_browser_24),
                                        contentDescription = stringResource(R.string.menu_view_in_browser)
                                    )
                                }
                                IconButton(onClick = ::shareThread) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_baseline_share_24),
                                        contentDescription = stringResource(R.string.menu_share)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ThreadScreen(
                            viewModel = viewModel,
                            threadId = threadId,
                            forumId = forumId,
                            forumTitle = forumTitle,
                            objectId = objectId,
                            objectName = objectName,
                            objectType = objectType,
                            listState = listState,
                            scrollCommands = scrollCommands,
                            onLatestArticleSeen = ::updateLatestArticle,
                            onArticleCountChanged = { articleCount = it },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        latestArticleId = getThreadKey(threadId)?.let { key ->
            preferences()[key, INVALID_ARTICLE_ID] ?: INVALID_ARTICLE_ID
        } ?: INVALID_ARTICLE_ID
    }

    override fun onPause() {
        super.onPause()
        if (latestArticleId != INVALID_ARTICLE_ID) {
            getThreadKey(threadId)?.let { key ->
                preferences()[key] = latestArticleId
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
    }

    private fun updateLatestArticle(articleId: Int) {
        if (articleId > latestArticleId) {
            latestArticleId = articleId
        }
    }

    private fun navigateUp() {
        ForumActivity.startUp(this, forumId, forumTitle, objectId, objectName, objectType)
        finish()
    }

    private fun shareThread() {
        val description = if (objectName.isBlank())
            String.format(getString(R.string.share_thread_text), threadSubject, forumTitle)
        else
            String.format(getString(R.string.share_thread_game_text), threadSubject, forumTitle, objectName)
        val link = createBggUri("thread", threadId).toString()
        share(
            getString(R.string.share_thread_subject), """
            $description
            
            $link
            """.trimIndent(), R.string.title_share
        )
    }

    private fun getThreadKey(threadId: Int): String? {
        if (threadId == BggContract.INVALID_ID) return null
        return "THREAD-$threadId"
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val KEY_THREAD_SUBJECT = "THREAD_SUBJECT"

        fun start(
            context: Context,
            threadId: Int,
            threadSubject: String,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ) {
            context.startActivity(createIntent(context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType))
        }

        fun startUp(
            context: Context,
            threadId: Int,
            threadSubject: String,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ) {
            context.startActivity(createIntent(context, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType).clearTop())
        }

        private fun createIntent(
            context: Context,
            threadId: Int,
            threadSubject: String,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ): Intent {
            return context.intentFor<ThreadActivity>(
                KEY_THREAD_ID to threadId,
                KEY_THREAD_SUBJECT to threadSubject,
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
            )
        }
    }
}

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
