package com.boardgamegeek.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.ArticleActivity
import com.boardgamegeek.ui.components.BggHtmlText
import com.boardgamegeek.util.XmlApi2TagHandler
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel,
    threadId: Int,
    forumId: Int,
    forumTitle: String,
    objectId: Int,
    objectName: String,
    objectType: Forum.Type,
    listState: LazyListState,
    scrollCommands: Flow<ThreadScrollCommand>,
    onLatestArticleSeen: (Int) -> Unit,
    onArticleCountChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val tagHandler = remember { XmlApi2TagHandler() }
    val articlesState by viewModel.articles.collectAsStateWithLifecycle()

    LaunchedEffect(threadId) {
        viewModel.setThreadId(threadId)
    }

    val articles = articlesState?.data?.articles.orEmpty()

    LaunchedEffect(articles.size) {
        onArticleCountChanged(articles.size)
    }

    LaunchedEffect(listState, articles) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .map { index -> articles.getOrNull(index)?.id }
            .filterNotNull()
            .collectLatest { onLatestArticleSeen(it) }
    }

    LaunchedEffect(scrollCommands, articles, listState) {
        scrollCommands.collectLatest { command ->
            val currentIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            when (command) {
                ThreadScrollCommand.ScrollToBottom -> {
                    val targetIndex = (articles.size - 1).coerceAtLeast(0)
                    if (articles.isNotEmpty()) {
                        scrollToIndex(listState, currentIndex, targetIndex)
                    }
                }
                is ThreadScrollCommand.ScrollToLatest -> {
                    val targetIndex = articles.indexOfFirst { it.id == command.articleId }
                    if (targetIndex >= 0) {
                        scrollToIndex(listState, currentIndex, targetIndex)
                    }
                }
            }
        }
    }

    when (val result = articlesState) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            when (result.status) {
                Status.REFRESHING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Status.ERROR -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = result.message.ifBlank { stringResource(R.string.empty_thread) })
                    }
                }
                Status.SUCCESS -> {
                    if (articles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.empty_thread))
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(articles, key = { it.id }) { article ->
                                ThreadArticleRow(
                                    article = article,
                                    forumId = forumId,
                                    forumTitle = forumTitle,
                                    objectId = objectId,
                                    objectName = objectName,
                                    objectType = objectType,
                                    threadId = result.data?.threadId ?: threadId,
                                    threadSubject = result.data?.subject.orEmpty(),
                                    tagHandler = tagHandler,
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadArticleRow(
    article: Article,
    forumId: Int,
    forumTitle: String,
    objectId: Int,
    objectName: String,
    objectType: Forum.Type,
    threadId: Int,
    threadSubject: String,
    tagHandler: XmlApi2TagHandler,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            if (article.postTicks > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.info_background))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_account_circle_18),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = article.username,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_schedule_18),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = article.postTicks.formatTimestamp(context, isForumTimestamp = true).toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (article.editTicks != article.postTicks) {
                                Text(text = "|", modifier = Modifier.padding(horizontal = 8.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_outline_edit_18),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = article.editTicks.formatTimestamp(context, isForumTimestamp = true).toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            ArticleActivity.start(
                                context,
                                threadId,
                                threadSubject,
                                forumId,
                                forumTitle,
                                objectId,
                                objectName,
                                objectType,
                                article
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_launch_24),
                            contentDescription = stringResource(R.string.view)
                        )
                    }
                }
            }
            BggHtmlText(
                text = article.body.trim(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                tagHandler = tagHandler
            )
        }
    }
}

sealed class ThreadScrollCommand {
    data object ScrollToBottom : ThreadScrollCommand()
    data class ScrollToLatest(val articleId: Int) : ThreadScrollCommand()
}

private suspend fun scrollToIndex(listState: LazyListState, currentIndex: Int, targetIndex: Int) {
    val difference = abs(currentIndex - targetIndex)
    if (difference <= SMOOTH_SCROLL_THRESHOLD) {
        listState.animateScrollToItem(targetIndex)
    } else {
        listState.scrollToItem(targetIndex)
    }
}

private const val SMOOTH_SCROLL_THRESHOLD = 10
