package com.boardgamegeek.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.ui.components.BggHtmlText
import com.boardgamegeek.util.XmlApiMarkupConverter

@Composable
fun CommentsScreen(
    viewModel: GameCommentsViewModel,
) {
    val context = LocalContext.current
    val lazyPagingItems = viewModel.comments.collectAsLazyPagingItems()
    val markupConverter = remember(context) { XmlApiMarkupConverter(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is LoadState.Error -> {
                Text(
                    text = state.error.localizedMessage ?: stringResource(R.string.empty_comments),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LoadState.NotLoading -> {
                if (lazyPagingItems.itemCount == 0) {
                    Text(
                        text = stringResource(R.string.empty_comments),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index -> "game-comment-$index" }
                        ) { index ->
                            val comment = lazyPagingItems[index]
                            if (comment != null) {
                                GameCommentRow(comment = comment, markupConverter = markupConverter)
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
private fun GameCommentRow(
    comment: GameComment,
    markupConverter: XmlApiMarkupConverter,
) {
    val context = LocalContext.current
    val ratingText = comment.rating.asPersonalRating(context)
    val ratingColorInt = comment.rating.toColor(BggColors.ratingColors)
    val ratingColor = Color(ratingColorInt)
    val ratingContentColor = if (ratingColorInt == android.graphics.Color.TRANSPARENT) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(ratingColorInt.getTextColor())
    }
    val html = remember(comment.comment) { markupConverter.toHtml(comment.comment) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ratingText,
                color = ratingContentColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(ratingColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (html.isNotBlank()) {
            BggHtmlText(
                text = html,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
