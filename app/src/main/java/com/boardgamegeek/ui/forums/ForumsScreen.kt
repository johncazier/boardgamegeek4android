package com.boardgamegeek.ui.forums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.forum.ForumLauncher
import java.text.NumberFormat

@Composable
fun ForumsScreen(
    viewModel: ForumsViewModel,
    forumType: Forum.Type,
    objectId: Int,
    objectName: String,
    paddingValues: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val forumsState by viewModel.forums.collectAsStateWithLifecycle()
    val numberFormat = NumberFormat.getNumberInstance()

    LaunchedEffect(forumType, objectId) {
        when (forumType) {
            Forum.Type.GAME -> viewModel.setGameId(objectId)
            Forum.Type.REGION -> viewModel.setRegion()
            Forum.Type.ARTIST,
            Forum.Type.DESIGNER -> viewModel.setPersonId(objectId)
            Forum.Type.PUBLISHER -> viewModel.setCompanyId(objectId)
        }
    }

    when (val result = forumsState) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            when (result.status) {
                Status.REFRESHING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                Status.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = result.message ?: stringResource(R.string.empty_forums))
                    }
                }
                Status.SUCCESS -> {
                    val forums = result.data.orEmpty()
                    if (forums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.empty_forums))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(forums) { forum ->
                                if (forum.isHeader) {
                                    Text(
                                        text = forum.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                ForumLauncher.start(
                                                    context,
                                                    forum.id,
                                                    forum.title,
                                                    objectId,
                                                    objectName,
                                                    forumType,
                                                )
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(text = forum.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = numberFormat.format(forum.numberOfThreads),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = forum.lastPostDateTime.formatTimestamp(context, isForumTimestamp = true).toString(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
