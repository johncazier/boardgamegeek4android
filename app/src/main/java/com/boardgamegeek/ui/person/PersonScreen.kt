package com.boardgamegeek.ui.person

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.PersonStats
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.forums.ForumsScreen
import com.boardgamegeek.ui.forums.ForumsViewModel
import com.boardgamegeek.ui.game.GameLauncher
import com.boardgamegeek.ui.linkedcollection.LinkedCollectionScreen
import java.text.DecimalFormat

@Composable
fun PersonScreen(
    personType: PersonType,
    personId: Int,
    personName: String,
    viewModel: PersonViewModel,
    forumsViewModel: ForumsViewModel,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val tabs = listOf(
        stringResource(R.string.title_description),
        stringResource(R.string.title_stats),
        stringResource(R.string.title_collection),
        stringResource(R.string.title_forums),
    )
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val details by viewModel.details.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val sort by viewModel.collectionSort.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> {
                val state = details
                when {
                    state == null || state.status == Status.REFRESHING -> CenteredProgress()
                    state.status == Status.ERROR && state.data == null -> CenteredMessage(
                        state.message ?: context.getString(R.string.empty_person)
                    )
                    state.data == null -> CenteredMessage(context.getString(R.string.empty_person))
                    state.data.description.isBlank() -> CenteredMessage(
                        context.getString(
                            R.string.empty_person_description,
                            personType.asTitle(context).lowercase()
                        )
                    )
                    else -> {
                        AndroidView(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            factory = { TextView(it) },
                            update = { textView ->
                                textView.setTextMaybeHtml(state.data.description)
                            },
                        )
                    }
                }
            }

            1 -> PersonStatsTab(stats = stats, emptyMessage = stringResource(R.string.empty_person_stats))

            2 -> {
                val emptyMessage = stringResource(
                    R.string.empty_linked_collection,
                    personType.asTitle(context).lowercase(),
                )
                Column(modifier = Modifier.fillMaxSize()) {
                    SortTabs(sortType = sort, onSortSelected = viewModel::sort)
                    LinkedCollectionScreen(
                        collection = collection,
                        emptyMessage = emptyMessage,
                        isRefreshing = details?.status == Status.REFRESHING,
                        onRefresh = viewModel::refresh,
                        onItemClick = { item ->
                            GameLauncher.start(
                                context = context,
                                gameId = item.gameId,
                                gameName = item.gameName,
                                thumbnailUrl = item.thumbnailUrl,
                                heroImageUrl = item.heroImageUrl,
                            )
                        },
                        paddingValues = PaddingValues(),
                    )
                }
            }

            else -> {
                ForumsScreen(
                    viewModel = forumsViewModel,
                    forumType = personType.toForumType(),
                    objectId = personId,
                    objectName = personName,
                    paddingValues = PaddingValues(),
                )
            }
        }
    }
}

@Composable
private fun PersonStatsTab(stats: PersonStats?, emptyMessage: String) {
    val context = LocalContext.current
    if (stats == null) {
        CenteredMessage(emptyMessage)
        return
    }
    val ratingText = stats.averageRating.asBoundedRating(context, DecimalFormat("#0.0"), R.string.unrated)
    val ratingColor = stats.averageRating.toColor(com.boardgamegeek.extensions.BggColors.ratingColors)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (stats.averageRating > 0.0) {
            Text(
                text = stringResource(R.string.average_rating) + ": $ratingText",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color(ratingColor),
            )
        }
        Text(stringResource(R.string.whitmore_score) + ": ${stats.whitmoreScore}")
        if (stats.whitmoreScore != stats.whitmoreScoreWithExpansions) {
            Text(stringResource(R.string.whitmore_score_with_expansions) + ": ${stats.whitmoreScoreWithExpansions}")
        }
        Text(stringResource(R.string.play_count) + ": ${stats.playCount}")
        Text(stringResource(R.string.h_index) + ": ${stats.hIndex.description}")
        Text(stringResource(R.string.g_index) + ": ${stats.gIndex.description}")
        Text(stringResource(R.string.pearson) + ": ${DecimalFormat("0.00").format(stats.pearson)}")
    }
}

@Composable
private fun SortTabs(
    sortType: CollectionItem.SortType,
    onSortSelected: (CollectionItem.SortType) -> Unit,
) {
    val tabs = remember {
        listOf(CollectionItem.SortType.RATING to R.string.rating, CollectionItem.SortType.NAME to R.string.name)
    }
    ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == sortType }.coerceAtLeast(0)) {
        tabs.forEachIndexed { index, (type, titleRes) ->
            Tab(
                selected = tabs[index].first == sortType,
                onClick = { onSortSelected(type) },
                text = { Text(stringResource(titleRes)) },
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message)
    }
}

private fun PersonType.toForumType() = when (this) {
    PersonType.ARTIST -> Forum.Type.ARTIST
    PersonType.DESIGNER -> Forum.Type.DESIGNER
    PersonType.PUBLISHER -> Forum.Type.PUBLISHER
}

private fun PersonType.asTitle(context: android.content.Context): String {
    val res = when (this) {
        PersonType.ARTIST -> R.string.title_artist
        PersonType.DESIGNER -> R.string.title_designer
        PersonType.PUBLISHER -> R.string.title_publisher
    }
    return context.getString(res)
}
