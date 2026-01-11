package com.boardgamegeek.ui

import android.text.format.DateUtils
import android.webkit.WebView
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.model.GeekListComment
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.coroutines.launch

@Composable
fun GeekListItemScreen(
    geekListItem: GeekListItem,
    geekListTitle: String,
    order: Int,
    paddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val markupConverter = remember { XmlApiMarkupConverter(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Hero Image
        if (geekListItem.heroImageUrls?.isNotEmpty() == true) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(geekListItem.heroImageUrls.firstOrNull())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.title_description)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(stringResource(R.string.title_comments)) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> DescriptionTab(geekListItem, geekListTitle, order, markupConverter)
                1 -> CommentsTab(geekListItem.comments, markupConverter)
            }
        }
    }
}

@Composable
fun DescriptionTab(
    geekListItem: GeekListItem,
    title: String,
    order: Int,
    markupConverter: XmlApiMarkupConverter
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(text = order.toString(), style = MaterialTheme.typography.displayMedium)
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = geekListItem.objectTypeDescription(LocalContext.current), style = MaterialTheme.typography.bodyMedium)
        Text(text = geekListItem.username, style = MaterialTheme.typography.bodyMedium)
        Text(text = "${geekListItem.numberOfThumbs} thumbs", style = MaterialTheme.typography.bodyMedium)

        val htmlBody = remember(geekListItem.body) { markupConverter.toHtml(geekListItem.body) }
        
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(0) // Transparent background
                }
            },
            update = { webView ->
                webView.setWebViewText(htmlBody)
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        val postDateStr = remember(geekListItem.postDateTime) {
             DateUtils.getRelativeTimeSpanString(geekListItem.postDateTime).toString()
        }
        Text(text = "Posted: $postDateStr", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

        if (geekListItem.editDateTime != geekListItem.postDateTime) {
             val editDateStr = remember(geekListItem.editDateTime) {
                 DateUtils.getRelativeTimeSpanString(geekListItem.editDateTime).toString()
             }
             Text(text = "Edited: $editDateStr", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CommentsTab(
    comments: List<GeekListComment>,
    markupConverter: XmlApiMarkupConverter
) {
    if (comments.isEmpty()) {
        Text(
            text = "No comments",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(comments) { comment ->
                CommentItem(comment, markupConverter)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun CommentItem(comment: GeekListComment, markupConverter: XmlApiMarkupConverter) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = comment.username, style = MaterialTheme.typography.titleMedium)
        Text(text = "${comment.numberOfThumbs} thumbs", style = MaterialTheme.typography.bodySmall)
        
        val htmlContent = remember(comment.content) { markupConverter.toHtml(comment.content) }
        AndroidView(
            factory = { context ->
                TextView(context)
            },
            update = { textView ->
                textView.setTextMaybeHtml(htmlContent)
            },
            modifier = Modifier.fillMaxWidth()
        )

        val postDateStr = remember(comment.postDate) {
             DateUtils.getRelativeTimeSpanString(comment.postDate).toString()
        }
        Text(text = postDateStr, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}
