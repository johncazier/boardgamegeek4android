@file:OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class
)

package com.boardgamegeek.ui.game

import android.text.format.DateUtils
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asAge
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.asPlayCount
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asRange
import com.boardgamegeek.extensions.asScore
import com.boardgamegeek.extensions.asWishListPriority
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.extensions.getSpannedText
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.isToday
import com.boardgamegeek.extensions.isKnownColor
import com.boardgamegeek.extensions.LINK_AMAZON_COM
import com.boardgamegeek.extensions.LINK_AMAZON_DE
import com.boardgamegeek.extensions.LINK_AMAZON_UK
import com.boardgamegeek.extensions.linkAmazon
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.linkCamelCamelCamel
import com.boardgamegeek.extensions.linkEbay
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.extensions.toDescription
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GameAgePoll
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.GameFamily
import com.boardgamegeek.model.GameLanguagePoll
import com.boardgamegeek.model.GamePlayerPollResults
import com.boardgamegeek.model.GameSubtype
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.comments.CommentsActivity
import com.boardgamegeek.ui.forum.ForumActivity
import com.boardgamegeek.ui.GameCollectionItemActivity
import com.boardgamegeek.ui.GameColorsActivity
import com.boardgamegeek.ui.GameDetailActivity
import com.boardgamegeek.ui.plays.GamePlaysActivity
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.play.PlayActivity
import com.boardgamegeek.ui.dialog.GameAgePollDialogFragment
import com.boardgamegeek.ui.dialog.GameLanguagePollDialogFragment
import com.boardgamegeek.ui.dialog.GameRanksDialogFragment
import com.boardgamegeek.ui.dialog.GameSuggestedPlayerCountPollDialogFragment
import com.boardgamegeek.ui.forums.ForumsViewModel
import com.boardgamegeek.ui.game.GameViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.ui.playstats.GamePlayStatsActivity
import java.text.DecimalFormat
import java.text.NumberFormat

@Composable
fun GameInfoTab(viewModel: GameViewModel) {
    val context = LocalContext.current
    val game by viewModel.game.collectAsStateWithLifecycle()
    val subtypes by viewModel.subtypes.collectAsStateWithLifecycle()
    val families by viewModel.families.collectAsStateWithLifecycle()
    val languagePoll by viewModel.languagePoll.collectAsStateWithLifecycle()
    val agePoll by viewModel.agePoll.collectAsStateWithLifecycle()
    val playerPoll by viewModel.playerPoll.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.gameIsRefreshing.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshGame() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (game == null) {
            EmptyMessage(message = stringResource(R.string.empty_game))
        } else {
            GameInfoContent(
                game = game!!,
                subtypes = subtypes,
                families = families,
                languagePoll = languagePoll,
                agePoll = agePoll,
                playerPoll = playerPoll,
            )
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun GameInfoContent(
    game: Game,
    subtypes: List<GameSubtype>,
    families: List<GameFamily>,
    languagePoll: GameLanguagePoll?,
    agePoll: GameAgePoll?,
    playerPoll: List<GamePlayerPollResults>,
) {
    val context = LocalContext.current
    val gameIconTint = rememberGameIconTint(game.iconColor)
    val scoreFormat = remember { DecimalFormat("#,##0.00") }
    val rankSeparator = " \u2022 "

    val rankText = subtypes.joinToString(rankSeparator) { it.describe(context) }
    val familyText = families.joinToString(rankSeparator) { it.describe(context) }
    val playerRange = context.getQuantityText(
        R.plurals.player_range_suffix,
        game.minPlayers,
        (game.minPlayers to game.maxPlayers).asRange()
    )

    val communityText = remember(playerPoll) {
        val bestCounts = playerPoll.filter { it.calculatedRecommendation == GamePlayerPollResults.BEST }.toSet()
        val goodCounts = playerPoll.filter {
            it.calculatedRecommendation == GamePlayerPollResults.BEST ||
                it.calculatedRecommendation == GamePlayerPollResults.RECOMMENDED
        }.toSet()
        val best = context.getSpannedText(R.string.best_prefix, bestCounts.toList().asRange())
        val good = context.getSpannedText(R.string.recommended_prefix, goodCounts.toList().asRange())
        when {
            bestCounts.isNotEmpty() && goodCounts.isNotEmpty() && bestCounts != goodCounts ->
                context.getString(R.string.ampersand, best, good)
            bestCounts.isNotEmpty() -> best.toString()
            goodCounts.isNotEmpty() -> good.toString()
            else -> ""
        }
    }

    val ratingText = game.rating.asBoundedRating(context, DecimalFormat("#0.0"), R.string.unrated)
    val ratingColor = Color(game.rating.toColor(BggColors.ratingColors))
    val ratingVotes = context.getQuantityText(R.plurals.ratings_suffix, game.numberOfRatings, game.numberOfRatings)
    val commentVotes = context.getQuantityText(R.plurals.comments_suffix, game.numberOfComments, game.numberOfComments)
    val ratingDetail = stringResource(R.string.ampersand, ratingVotes, commentVotes)

    val weightText = game.averageWeight.toDescription(context, R.array.game_weight, R.string.unknown_weight)
    val weightScore = if (game.averageWeight == Game.UNWEIGHTED) "" else game.averageWeight.asScore(context, format = scoreFormat)
    val weightVotes = context.getQuantityText(
        R.plurals.votes_suffix,
        game.numberOfUsersWeighting,
        game.numberOfUsersWeighting
    )
    val weightColorInt = game.averageWeight.toColor(BggColors.fiveStageColors)
    val weightColor = Color(weightColorInt)
    val weightTextColor = Color(weightColorInt.getTextColor())

    val languageScore = languagePoll?.calculateScore() ?: 0.0
    val languageVotes = languagePoll?.totalVotes ?: 0
    val languageText = languageScore.toDescription(context, R.array.language_poll, R.string.unknown_language)
    val languageScoreText = if (languageScore == 0.0) "" else languageScore.asScore(context, format = scoreFormat)
    val languageVotesText = context.getQuantityText(R.plurals.votes_suffix, languageVotes, languageVotes)
    val languageColorInt = languageScore.toColor(BggColors.fiveStageColors)
    val languageColor = Color(languageColorInt)
    val languageTextColor = Color(languageColorInt.getTextColor())

    val ageVotes = agePoll?.totalVotes ?: 0
    val ageCommunityText = agePoll?.modalValue?.takeIf { it.isNotBlank() }?.let {
        context.getSpannedText(R.string.age_community, it).toString()
    }.orEmpty()

    val activity = context as FragmentActivity
    val listState = rememberLazyListState()

    LaunchedEffect(rankText, familyText) {
        if ((rankText.isNotBlank() || familyText.isNotBlank()) && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        item(key = "rank_row") {
            if (rankText.isNotBlank() || familyText.isNotBlank()) {
                InfoRow(
                    iconRes = R.drawable.ic_baseline_emoji_events_24,
                    title = rankText,
                    subtitle = familyText.takeIf { it.isNotBlank() },
                    iconTint = gameIconTint,
                    onClick = { activity.showAndSurvive(GameRanksDialogFragment()) }
                )
                HorizontalDivider()
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
        item {
            InfoRow(
                iconRes = R.drawable.ic_baseline_star_rate_24,
                title = ratingText,
                subtitle = ratingDetail,
                iconTint = gameIconTint,
                trailing = {
                    ValueBadge(text = ratingText, background = ratingColor)
                },
                onClick = {
                    if (game.numberOfRatings > 0 || game.numberOfComments > 0) {
                        CommentsActivity.startRating(context, game.id, game.name)
                    }
                }
            )
            HorizontalDivider()
        }
        item {
            InfoRow(
                iconRes = R.drawable.ic_baseline_calendar_today_24,
                title = game.yearPublished.asYear(context),
                subtitle = stringResource(R.string.year_published),
                iconTint = gameIconTint
            )
            HorizontalDivider()
        }
        item {
            val playTime = context.getQuantityText(
                R.plurals.mins_suffix,
                game.minPlayingTime,
                (game.minPlayingTime to game.maxPlayingTime).asRange()
            )
            InfoRow(
                iconRes = R.drawable.ic_outline_timer_24,
                title = playTime,
                subtitle = stringResource(R.string.title_play_time),
                iconTint = gameIconTint
            )
            HorizontalDivider()
        }
        item {
            val subtitle = listOfNotNull(
                stringResource(R.string.player_range),
                communityText.takeIf { it.isNotBlank() }
            ).joinToString(" \u2022 ")
            InfoRow(
                iconRes = R.drawable.ic_baseline_group_24,
                title = playerRange,
                subtitle = subtitle,
                iconTint = gameIconTint,
                onClick = {
                    if (game.suggestedPlayerCountPollVoteTotal > 0) {
                        activity.showAndSurvive(GameSuggestedPlayerCountPollDialogFragment().apply {
                            setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
                        })
                    }
                }
            )
            HorizontalDivider()
        }
        item {
            val subtitle = listOfNotNull(
                stringResource(R.string.player_ages),
                ageCommunityText.takeIf { it.isNotBlank() }
            ).joinToString(" \u2022 ")
            InfoRow(
                iconRes = R.drawable.ic_baseline_face_24,
                title = game.minimumAge.asAge(context),
                subtitle = subtitle,
                iconTint = gameIconTint,
                onClick = {
                    if (ageVotes > 0) {
                        activity.showAndSurvive(GameAgePollDialogFragment().apply {
                            setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
                        })
                    }
                }
            )
            HorizontalDivider()
        }
        item {
            InfoRow(
                iconRes = R.drawable.ic_baseline_scale_24,
                title = weightText,
                subtitle = listOfNotNull(weightScore.takeIf { it.isNotBlank() }, weightVotes.takeIf { it.isNotBlank() }).joinToString(" \u2022 "),
                iconTint = gameIconTint,
                trailing = {
                    if (weightScore.isNotBlank()) {
                        ValueBadge(text = weightScore, background = weightColor, textColor = weightTextColor)
                    }
                }
            )
            HorizontalDivider()
        }
        item {
            InfoRow(
                iconRes = R.drawable.ic_baseline_language_24,
                title = languageText,
                subtitle = listOfNotNull(languageScoreText.takeIf { it.isNotBlank() }, languageVotesText.takeIf { it.isNotBlank() }).joinToString(" \u2022 "),
                iconTint = gameIconTint,
                trailing = {
                    if (languageScoreText.isNotBlank()) {
                        ValueBadge(text = languageScoreText, background = languageColor, textColor = languageTextColor)
                    }
                },
                onClick = {
                    if (languageVotes > 0) {
                        activity.showAndSurvive(GameLanguagePollDialogFragment().apply {
                            setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog)
                        })
                    }
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            GameFooter(gameId = game.id, updated = game.updated)
        }
    }
}

private fun List<GamePlayerPollResults>.asRange(comma: String = ", ", dash: String = " - "): String {
    return this.sortedBy { it.playerNumber }.fold(mutableListOf<MutableList<GamePlayerPollResults>>()) { accumulator, element ->
        val current = element.playerNumber
        val last = accumulator.lastOrNull()?.lastOrNull()?.playerNumber ?: Int.MAX_VALUE
        if (accumulator.isEmpty() || last != current - 1) {
            accumulator += mutableListOf(element)
        } else {
            accumulator.last() += element
        }
        accumulator
    }.joinToString(comma) {
        if (it.size == 1) {
            it.first().playerCount
        } else if (it.last().playerCount.endsWith('+')) {
            it.first().playerCount + "+"
        } else {
            it.first().playerCount + dash + it.last().playerCount
        }
    }
}

@Composable
fun GameCreditsTab(viewModel: GameViewModel) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val designers by viewModel.designers.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val publishers by viewModel.publishers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val mechanics by viewModel.mechanics.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.gameIsRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDesignerImages(4)
        viewModel.refreshArtistImages(4)
        viewModel.refreshPublisherImages(4)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing == true,
        onRefresh = { viewModel.refreshGame() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (game == null) {
            EmptyMessage(message = stringResource(R.string.empty_game))
        } else {
            val gameIconTint = rememberGameIconTint(game!!.iconColor)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    CreditsSection(
                        titleRes = R.string.designers,
                        items = designers,
                        iconRes = R.drawable.ic_baseline_edit_24,
                        type = GameViewModel.ProducerType.DESIGNER,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item { CreditsHorizontalDivider() }
                item {
                    CreditsSection(
                        titleRes = R.string.artists,
                        items = artists,
                        iconRes = R.drawable.ic_baseline_brush_24,
                        type = GameViewModel.ProducerType.ARTIST,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item { CreditsHorizontalDivider() }
                item {
                    CreditsSection(
                        titleRes = R.string.publishers,
                        items = publishers,
                        iconRes = R.drawable.ic_baseline_import_contacts_24,
                        type = GameViewModel.ProducerType.PUBLISHER,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item { CreditsHorizontalDivider() }
                item {
                    CreditsSection(
                        titleRes = R.string.categories,
                        items = categories,
                        iconRes = R.drawable.ic_baseline_category_24,
                        type = GameViewModel.ProducerType.CATEGORY,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item { CreditsHorizontalDivider() }
                item {
                    CreditsSection(
                        titleRes = R.string.mechanics,
                        items = mechanics,
                        iconRes = R.drawable.ic_baseline_settings_24,
                        type = GameViewModel.ProducerType.MECHANIC,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    GameFooter(gameId = game!!.id, updated = game!!.updated)
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun GameDescriptionTab(viewModel: GameViewModel) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.gameIsRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing == true,
        onRefresh = { viewModel.refreshGame() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (game == null) {
            EmptyMessage(message = stringResource(R.string.empty_game))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    HtmlText(text = game!!.description)
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    GameFooter(gameId = game!!.id, updated = game!!.updated)
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun GameCollectionTab(viewModel: GameViewModel) {
    val context = LocalContext.current
    val items by viewModel.collectionItems.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.itemsAreRefreshing.collectAsStateWithLifecycle()
    val xmlConverter = remember { XmlApiMarkupConverter(context) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing == true,
        onRefresh = { viewModel.refreshItems() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        when {
            items.isEmpty() -> {
                EmptyMessage(message = stringResource(R.string.empty_game_collection))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(items, key = { it.collectionId }) { item ->
                        CollectionItemRow(item = item, xmlConverter = xmlConverter)
                        HorizontalDivider()
                    }
                    item {
                        val syncTimestamp = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
                        Spacer(modifier = Modifier.height(8.dp))
                        TimestampFooter(timestamp = syncTimestamp)
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun GamePlaysTab(viewModel: GameViewModel) {

    val game by viewModel.game.collectAsStateWithLifecycle()
    val plays by viewModel.plays.collectAsStateWithLifecycle()
    val playColors by viewModel.playColors.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.playsAreRefreshing.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing == true,
        onRefresh = { viewModel.refreshPlays() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (game == null) {
            EmptyMessage(message = stringResource(R.string.empty_game))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    PlaysSummarySection(game = game!!, plays = plays)
                    HorizontalDivider()
                }
                item {
                    InProgressSection(plays = plays)
                    HorizontalDivider()
                }
                item {
                    LastPlaySection(plays = plays)
                    HorizontalDivider()
                }
                item {
                    StatsSection(game = game!!, plays = plays)
                    HorizontalDivider()
                }
                item {
                    ColorsSection(game = game!!, colors = playColors)
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun GameLinkedItemsTab(viewModel: GameViewModel) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val expansions by viewModel.expansions.collectAsStateWithLifecycle()
    val baseGames by viewModel.baseGames.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.gameIsRefreshing.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing == true,
        onRefresh = { viewModel.refreshGame() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (game == null) {
            EmptyMessage(message = stringResource(R.string.empty_game))
        } else {
            val gameIconTint = rememberGameIconTint(game!!.iconColor)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    LinkedItemsSection(
                        titleRes = R.string.expansions,
                        items = expansions,
                        iconRes = R.drawable.ic_baseline_flip_to_back_24,
                        type = GameViewModel.ProducerType.EXPANSION,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item { CreditsHorizontalDivider() }
                item {
                    LinkedItemsSection(
                        titleRes = R.string.base_games,
                        items = baseGames,
                        iconRes = R.drawable.ic_baseline_flip_to_front_24,
                        type = GameViewModel.ProducerType.BASE_GAME,
                        gameId = game!!.id,
                        gameName = game!!.name,
                        iconColor = gameIconTint
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    GameFooter(gameId = game!!.id, updated = game!!.updated)
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing == true,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun GameLinksTab(viewModel: GameViewModel) {
    val context = LocalContext.current
    val game by viewModel.game.collectAsStateWithLifecycle()
    val iconColor = rememberGameIconTint(game?.iconColor ?: 0)

    if (game == null) {
        EmptyMessage(message = stringResource(R.string.empty_game))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        item {
            LinkRow(
                iconRes = R.drawable.ic_baseline_person_24,
                title = stringResource(R.string.link_geekbuddy_analysis),
                iconTint = iconColor,
                onClick = { context.linkToBgg("geekbuddy/analyze/thing", game!!.id) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_baseline_open_in_browser_24,
                title = stringResource(R.string.link_bgg),
                iconTint = iconColor,
                onClick = { context.linkBgg(game!!.id) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_baseline_local_atm_24,
                title = stringResource(R.string.link_camel),
                iconTint = iconColor,
                onClick = { context.linkCamelCamelCamel(game!!.name) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_action_amazon,
                title = stringResource(R.string.link_amazon),
                iconTint = iconColor,
                onClick = { context.linkAmazon(game!!.name, LINK_AMAZON_COM) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_action_amazon,
                title = stringResource(R.string.link_amazon_uk),
                iconTint = iconColor,
                onClick = { context.linkAmazon(game!!.name, LINK_AMAZON_UK) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_action_amazon,
                title = stringResource(R.string.link_amazon_de),
                iconTint = iconColor,
                onClick = { context.linkAmazon(game!!.name, LINK_AMAZON_DE) }
            )
            HorizontalDivider()
        }
        item {
            LinkRow(
                iconRes = R.drawable.ic_baseline_gavel_24,
                title = stringResource(R.string.link_ebay),
                iconTint = iconColor,
                onClick = { context.linkEbay(game!!.name) }
            )
        }
    }
}

@Composable
fun GameForumsTab(gameId: Int, gameName: String) {
    val context = LocalContext.current
    val viewModel: ForumsViewModel = viewModel()
    val forumsState by viewModel.forums.collectAsStateWithLifecycle()
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    LaunchedEffect(gameId) {
        viewModel.setGameId(gameId)
    }

    when (val result = forumsState) {
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
                    EmptyMessage(message = result.message ?: stringResource(R.string.empty_forums))
                }
                Status.SUCCESS -> {
                    val forums = result.data.orEmpty()
                    if (forums.isEmpty()) {
                        EmptyMessage(message = stringResource(R.string.empty_forums))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                                ForumActivity.start(
                                                    context,
                                                    forum.id,
                                                    forum.title,
                                                    gameId,
                                                    gameName,
                                                    com.boardgamegeek.model.Forum.Type.GAME
                                                )
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(text = forum.title, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_outline_forum_18),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = numberFormat.format(forum.numberOfThreads.toLong()),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.ic_outline_schedule_18),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = forum.lastPostDateTime.formatTimestamp(context, isForumTimestamp = true).toString(),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
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

@Composable
private fun InfoRow(
    @DrawableRes iconRes: Int,
    title: CharSequence,
    subtitle: CharSequence? = null,
    iconTint: Color,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        leadingContent = {
            Icon(painter = painterResource(iconRes), contentDescription = null, tint = iconTint)
        },
        headlineContent = { Text(title.toString(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { if (!subtitle.isNullOrBlank()) Text(subtitle.toString()) },
        trailingContent = trailing,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ValueBadge(
    text: String,
    background: Color,
    textColor: Color = Color(background.red, background.green, background.blue, 1f).let {
        if ((background.red * 255 * 0.299 + background.green * 255 * 0.587 + background.blue * 255 * 0.114) / 255f < 0.5f) Color.White else Color.Black
    },
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CreditsSection(
    titleRes: Int,
    items: List<GameDetail>,
    @DrawableRes iconRes: Int,
    type: GameViewModel.ProducerType,
    gameId: Int,
    gameName: String,
    iconColor: Color,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val sectionTitle = stringResource(titleRes)

    Text(
        text = sectionTitle,
        style = MaterialTheme.typography.titleSmall,
        color = iconColor
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val limit = 4
        if (items.size <= limit) {
            items.forEach { producer ->
                ProducerChip(
                    producer = producer,
                    iconRes = iconRes,
                    type = type
                )
            }
        } else {
            items.take(limit - 1).forEach { producer ->
                ProducerChip(
                    producer = producer,
                    iconRes = iconRes,
                    type = type
                )
            }
            AssistChip(
                onClick = {
                    GameDetailActivity.start(
                        context,
                        sectionTitle,
                        gameId,
                        gameName,
                        type
                    )
                },
                label = { Text(stringResource(R.string.more_suffix, items.size - limit + 1)) },
                leadingIcon = { Icon(painter = painterResource(iconRes), contentDescription = null) }
            )
        }
    }
}

@Composable
private fun LinkedItemsSection(
    titleRes: Int,
    items: List<GameDetail>,
    @DrawableRes iconRes: Int,
    type: GameViewModel.ProducerType,
    gameId: Int,
    gameName: String,
    iconColor: Color,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val sectionTitle = stringResource(titleRes)
    Text(
        text = sectionTitle,
        style = MaterialTheme.typography.titleSmall,
        color = iconColor
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val limit = 4
        if (items.size <= limit) {
            items.forEach { producer ->
                LinkedItemChip(producer = producer)
            }
        } else {
            items.take(limit - 1).forEach { producer ->
                LinkedItemChip(producer = producer)
            }
            AssistChip(
                onClick = {
                    GameDetailActivity.start(
                        context,
                        sectionTitle,
                        gameId,
                        gameName,
                        type
                    )
                },
                label = { Text(stringResource(R.string.more_suffix, items.size - limit + 1)) },
                leadingIcon = { Icon(painter = painterResource(iconRes), contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ProducerChip(
    producer: GameDetail,
    @DrawableRes iconRes: Int,
    type: GameViewModel.ProducerType,
) {
    val context = LocalContext.current
    AssistChip(
        onClick = {
            when (type) {
                GameViewModel.ProducerType.ARTIST -> PersonActivity.startForArtist(context, producer.id, producer.name)
                GameViewModel.ProducerType.DESIGNER -> PersonActivity.startForDesigner(context, producer.id, producer.name)
                GameViewModel.ProducerType.PUBLISHER -> PersonActivity.startForPublisher(context, producer.id, producer.name)
                else -> {}
            }
        },
        label = { Text(producer.name) },
        leadingIcon = {
            if (producer.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = producer.thumbnailUrl.ensureHttpsScheme(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(painter = painterResource(iconRes), contentDescription = null)
            }
        }
    )
}

@Composable
private fun LinkedItemChip(producer: GameDetail) {
    val context = LocalContext.current
    AssistChip(
        onClick = { GameActivity.start(context, producer.id, producer.name) },
        label = { Text(producer.name) }
    )
}

@Composable
private fun CreditsHorizontalDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun HtmlText(text: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                textSize = 16f
                setTextColor(textColor)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.setTextMaybeHtml(text)
        }
    )
}

@Composable
private fun CollectionItemRow(item: CollectionItem, xmlConverter: XmlApiMarkupConverter) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.internalId != BggContract.INVALID_ID.toLong()) {
                GameCollectionItemActivity.start(context, item)
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.thumbnailUrl.ensureHttpsScheme(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.gameName, style = MaterialTheme.typography.titleMedium)
                val description = if (item.collectionName.isNotBlank() && item.collectionName != item.gameName ||
                    item.collectionYearPublished != CollectionItem.YEAR_UNKNOWN && item.collectionYearPublished != item.gameYearPublished
                ) {
                    if (item.collectionYearPublished == CollectionItem.YEAR_UNKNOWN) {
                        item.collectionName
                    } else {
                        "${item.collectionName} (${item.collectionYearPublished.asYear(context)})"
                    }
                } else ""
                if (description.isNotBlank()) {
                    Text(text = description, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (item.rating in 1.0..10.0) {
                val ratingColor = item.rating.toColor(BggColors.ratingColors)
                Surface(
                    color = Color(ratingColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.rating.asPersonalRating(context),
                        color = Color(ratingColor.getTextColor()),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        val statuses = describeStatuses(item, context).formatList()
        if (statuses.isNotBlank()) {
            Text(text = statuses, style = MaterialTheme.typography.bodySmall)
        }
        if (item.comment.isNotBlank()) {
            HtmlText(text = xmlConverter.toHtml(item.comment))
        }
        if (item.hasPrivateInfo()) {
            Text(text = item.getPrivateInfo(context).toString(), style = MaterialTheme.typography.bodySmall)
        }
        if (item.privateComment.isNotBlank()) {
            HtmlText(text = xmlConverter.toHtml(item.privateComment))
        }
    }
}

private fun describeStatuses(item: CollectionItem, ctx: android.content.Context): List<String> {
    val statuses = mutableListOf<String>()
    if (item.own) statuses.add(ctx.getString(R.string.collection_status_own))
    if (item.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
    if (item.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
    if (item.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
    if (item.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
    if (item.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
    if (item.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
    if (item.wishList) statuses.add(item.wishListPriority.asWishListPriority(ctx))
    if (statuses.isEmpty()) {
        if (item.numberOfPlays > 0) {
            statuses.add(ctx.getString(R.string.played))
        } else {
            if (item.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
            if (item.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
        }
    }
    return statuses
}

@Composable
private fun TimestampFooter(timestamp: Long) {
    val context = LocalContext.current
    val formatted = if (timestamp <= 0) {
        stringResource(R.string.needs_updating)
    } else {
        stringResource(R.string.synced_prefix, timestamp.formatTimestamp(context))
    }
    Text(
        text = formatted,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun GameFooter(gameId: Int, updated: Long) {
    val context = LocalContext.current
    val syncedText = if (updated <= 0) {
        stringResource(R.string.needs_updating)
    } else {
        stringResource(R.string.synced_prefix, updated.formatTimestamp(context))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = syncedText, style = MaterialTheme.typography.bodySmall)
        Text(text = gameId.toString(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkRow(
    @DrawableRes iconRes: Int,
    title: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlaysSummarySection(game: Game, plays: List<Play>) {
    val context = LocalContext.current
    val playCount = plays.sumOf { it.quantity }
    val (count, description, color) = playCount.asPlayCount(context)

    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(color), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = count.toString(), color = Color(color.getTextColor()))
            }
        },
        headlineContent = {
            Text(text = context.getQuantityText(R.plurals.play_title_suffix, playCount, playCount).toString())
        },
        supportingContent = { if (description.isNotBlank()) Text(text = description) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                GamePlaysActivity.start(
                    context,
                    game.id,
                    game.name,
                    game.heroImageUrl,
                    game.thumbnailUrl,
                    game.customPlayerSort,
                    game.iconColor
                )
            }
    )
}

@Composable
private fun InProgressSection(plays: List<Play>) {
    val inProgressPlays = plays.filter { it.dirtyTimestamp > 0 }
    if (inProgressPlays.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = stringResource(R.string.title_in_progress), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        inProgressPlays.take(3).forEach { play ->
            InProgressRow(play = play)
            HorizontalDivider()
        }
    }
}

@Composable
private fun InProgressRow(play: Play) {
    val context = LocalContext.current
    val timeText by produceState(initialValue = "", play.startTime, play.dateInMillis) {
        while (true) {
            value = when {
                play.startTime > 0 -> context.getSpannedText(
                    R.string.playing_for_prefix,
                    DateUtils.formatElapsedTime((System.currentTimeMillis() - play.startTime) / 1000)
                ).toString()
                play.dateInMillis.isToday() -> context.getSpannedText(R.string.playing_prefix, play.dateForDisplay(context)).toString()
                else -> context.getSpannedText(R.string.playing_since_prefix, play.dateForDisplay(context)).toString()
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { PlayActivity.start(context, play.internalId) }
            .padding(vertical = 8.dp)
    ) {
        Text(text = timeText, style = MaterialTheme.typography.bodyMedium)
        Text(text = play.describe(context), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LastPlaySection(plays: List<Play>) {
    val context = LocalContext.current
    val lastPlay = plays.filter { it.dirtyTimestamp == 0L }.maxByOrNull { it.dateInMillis } ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { PlayActivity.start(context, lastPlay.internalId) }
            .padding(vertical = 8.dp)
    ) {
        Text(text = context.getSpannedText(R.string.last_played_prefix, lastPlay.dateForDisplay(context)).toString())
        Text(text = lastPlay.describe(context), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatsSection(game: Game, plays: List<Play>) {
    val context = LocalContext.current
    if (plays.isEmpty()) return
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_pie_chart_24),
                contentDescription = null,
                tint = rememberGameIconTint(game.iconColor)
            )
        },
        headlineContent = { Text(text = stringResource(R.string.title_play_stats)) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { GamePlayStatsActivity.start(context, game.id, game.name, game.iconColor) }
    )
}

@Composable
private fun ColorsSection(game: Game, colors: List<String>) {
    val context = LocalContext.current
    if (colors.isEmpty()) return
    val knownColors = colors.all { it.isKnownColor() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { GameColorsActivity.start(context, game.id, game.name, game.iconColor) }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = context.getQuantityText(R.plurals.colors_suffix, colors.size, colors.size).toString(),
            style = MaterialTheme.typography.titleSmall
        )
        if (knownColors) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { colorName ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(colorName.asColorRgb()), CircleShape)
                    )
                }
            }
        }
    }
}
