package com.boardgamegeek.ui.forum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.toFormattedString
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Thread
import com.boardgamegeek.ui.thread.ThreadActivity

@Composable
fun ForumScreen(
    viewModel: ForumViewModel,
    forumId: Int,
    forumTitle: String,
    objectId: Int,
    objectName: String,
    objectType: Forum.Type,
) {
    val context = LocalContext.current
    val lazyPagingItems = viewModel.threads.collectAsLazyPagingItems()

    LaunchedEffect(forumId) {
        viewModel.setForumId(forumId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is LoadState.Error -> {
                Text(
                    text = state.error.localizedMessage ?: stringResource(R.string.empty_forum),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LoadState.NotLoading -> {
                if (lazyPagingItems.itemCount == 0) {
                    Text(
                        text = stringResource(R.string.empty_forum),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.threadId }
                        ) { index ->
                            val thread = lazyPagingItems[index]
                            if (thread != null) {
                                ForumThreadRow(
                                    thread = thread,
                                    onClick = {
                                        ThreadActivity.start(
                                            context,
                                            thread.threadId,
                                            thread.subject,
                                            forumId,
                                            forumTitle,
                                            objectId,
                                            objectName,
                                            objectType
                                        )
                                    }
                                )
                                HorizontalDivider()
                            }
                        }

                        lazyPagingItems.loadState.apply {
                            if (append is LoadState.Loading) {
                                item {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                            if (append is LoadState.Error) {
                                val e = lazyPagingItems.loadState.append as LoadState.Error
                                item {
                                    Text(
                                        text = e.error.localizedMessage ?: "",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumThreadRow(
    thread: Thread,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = thread.subject,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_account_circle_18),
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(text = thread.author, style = MaterialTheme.typography.labelSmall)
            Text(text = "|", modifier = Modifier.padding(horizontal = 8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_outline_forum_18),
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = ((thread.numberOfArticles) - 1).toFormattedString(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(text = "|", modifier = Modifier.padding(horizontal = 8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_outline_schedule_18),
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = thread.lastPostDate.formatTimestamp(context, isForumTimestamp = true).toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
