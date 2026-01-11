package com.boardgamegeek.ui.geeklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.model.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.geeklistitem.GeekListItemActivity
import com.boardgamegeek.ui.components.BggHtmlText
import com.boardgamegeek.ui.components.HtmlText
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.coroutines.launch

@Composable
fun GeekListScreen(
    viewModel: GeekListViewModel,
    paddingValues: PaddingValues
) {
    val geekListResource by viewModel.geekList.collectAsState(initial = RefreshableResource.refreshing())
    val pages = listOf(
        stringResource(R.string.title_description),
        stringResource(R.string.title_items),
        stringResource(R.string.title_comments)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
            pages.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> GeekListDescription(geekListResource = geekListResource)
                1 -> GeekListItems(geekListResource = geekListResource)
                2 -> GeekListComments(geekListResource = geekListResource)
            }
        }
    }
}

@Composable
fun GeekListDescription(geekListResource: RefreshableResource<GeekList>) {
    val context = LocalContext.current
    val markupConverter = remember { XmlApiMarkupConverter(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        geekListResource.let { (status, data, _) ->
            if (status == Status.REFRESHING) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (data != null) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                ) {
                    // Header
                    Text(text = data.username)
                    Text(text = pluralStringResource(id = R.plurals.num_items, data.numberOfItems, data.numberOfItems))
                    Text(text = pluralStringResource(id = R.plurals.num_thumbs, data.numberOfThumbs, data.numberOfThumbs))
                    Row {
                        Text(text = stringResource(id = R.string.posted))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = data.postTicks.formatTimestamp(context).toString())
                    }
                    if (data.editTicks > 0) {
                        Row {
                            Text(text = stringResource(id = R.string.edited))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = data.editTicks.formatTimestamp(context).toString())
                        }
                    }

                    HtmlText(html = markupConverter.toHtml(data.description))
                }
            }
        }
    }
}

@Composable
fun GeekListItems(geekListResource: RefreshableResource<GeekList>) {
    Box(modifier = Modifier.fillMaxSize()) {
        geekListResource.let { (status, data, message) ->
            when (status) {
                Status.REFRESHING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                Status.ERROR -> {
                    Text(
                        text = message ?: stringResource(id = R.string.empty_geeklist),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    if (data?.items.isNullOrEmpty()) {
                        Text(
                            text = stringResource(id = R.string.empty_geeklist),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(data.items) { index, item ->
                                GeekListItemRow(
                                    geekList = data,
                                    geekListItem = item,
                                    order = index + 1
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeekListItemRow(geekList: GeekList, geekListItem: GeekListItem, order: Int) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (geekListItem.objectId != BggContract.INVALID_ID) {
                    GeekListItemActivity.start(context, geekList, geekListItem, order)
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = order.toString())
        Spacer(modifier = Modifier.width(16.dp))
        AsyncImage(
            model = geekListItem.thumbnailUrls?.firstOrNull(),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = geekListItem.objectName)
            if (geekListItem.username != geekList.username) {
                Text(text = geekListItem.username)
            }
        }
    }
}

@Composable
fun GeekListComments(geekListResource: RefreshableResource<GeekList>) {
    Box(modifier = Modifier.fillMaxSize()) {
        geekListResource.let { (status, data, message) ->
            when (status) {
                Status.REFRESHING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                Status.ERROR -> {
                    Text(
                        text = message ?: stringResource(id = R.string.no_comments),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    if (data?.comments.isNullOrEmpty()) {
                        Text(
                            text = stringResource(id = R.string.no_comments),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(data.comments) { comment ->
                                GeekListCommentRow(comment = comment)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeekListCommentRow(comment: GeekListComment) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text(text = comment.username)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = comment.numberOfThumbs.toString())
        }
        Row {
            Text(text = comment.postDate.formatTimestamp(context).toString())
            if (comment.editDate != comment.postDate) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = comment.editDate.formatTimestamp(context).toString())
            }
        }
        BggHtmlText(text = comment.content)
    }
}
