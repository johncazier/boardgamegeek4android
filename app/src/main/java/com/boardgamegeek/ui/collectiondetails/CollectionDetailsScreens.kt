package com.boardgamegeek.ui.collectiondetails

import android.content.DialogInterface
import android.graphics.Color
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.MenuRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.core.content.res.ResourcesCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.ArtistsActivity
import com.boardgamegeek.ui.CategoriesActivity
import com.boardgamegeek.ui.DesignersActivity
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.GameCollectionItemActivity
import com.boardgamegeek.ui.logplay.LogPlayActivity
import com.boardgamegeek.ui.mechanics.MechanicsActivity
import com.boardgamegeek.ui.PublishersActivity
import com.boardgamegeek.ui.startActivity
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun CollectionDetailsScreen(
    viewModel: CollectionDetailsViewModel,
    paddingValues: PaddingValues
) {
    val pages = listOf(
        stringResource(R.string.title_browse),
        stringResource(R.string.title_own),
        stringResource(R.string.title_play),
        stringResource(R.string.title_acquire),
        stringResource(R.string.title_divest),
        stringResource(R.string.title_analyze),
        stringResource(R.string.title_credits),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 0.dp
        ) {
            pages.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> CollectionBrowseTab(viewModel)
                1 -> CollectionOwnTab(viewModel)
                2 -> CollectionPlayTab(viewModel)
                3 -> CollectionAcquireTab(viewModel)
                4 -> CollectionDivestTab(viewModel)
                5 -> CollectionAnalyzeTab(viewModel)
                6 -> CollectionCreditsTab()
            }
        }
    }
}

@Composable
private fun CollectionBrowseTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val recentlyViewed by viewModel.recentlyViewedItems.collectAsStateWithLifecycle(emptyList())
    val friendlessFavorites by viewModel.friendlessFavoriteItems.collectAsStateWithLifecycle(emptyList())
    val friendless by viewModel.friendless.collectAsStateWithLifecycle(0)
    val underrated by viewModel.underratedItems.collectAsStateWithLifecycle(emptyList())
    val hawt by viewModel.hawtItems.collectAsStateWithLifecycle(emptyList())

    val padding = dimensionResource(R.dimen.padding_extra)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = padding)
    ) {
        CollectionShelfView(
            header = stringResource(R.string.title_recently_viewed),
            items = recentlyViewed,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_friendless_favorites),
            helpText = stringResource(R.string.play_stat_friendless_info),
            items = friendlessFavorites,
            count = friendless,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.rating) }

        CollectionShelfView(
            header = stringResource(R.string.title_hidden_gems),
            helpText = stringResource(R.string.info_hidden_gems),
            items = underrated,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.rating) }

        CollectionShelfView(
            header = stringResource(R.string.title_hawt),
            helpText = stringResource(R.string.info_hawt),
            items = hawt,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.rating) }
    }
}

@Composable
private fun CollectionOwnTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val growthRate by viewModel.growthRate.collectAsStateWithLifecycle(0)
    val utilization by viewModel.utilization.collectAsStateWithLifecycle(0.0)
    val games by viewModel.own.collectAsStateWithLifecycle()
    val expansions by viewModel.expansions.collectAsStateWithLifecycle()
    val accessories by viewModel.accessories.collectAsStateWithLifecycle()
    val recentlyAcquired by viewModel.recentlyAcquired.collectAsStateWithLifecycle()
    val hawt by viewModel.hawtItems.collectAsStateWithLifecycle(emptyList())

    val padding = dimensionResource(R.dimen.padding_extra)
    val horizontalMargin = dimensionResource(R.dimen.material_margin_horizontal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = padding)
    ) {
        Text(
            text = stringResource(R.string.msg_growth_rate, growthRate),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = horizontalMargin, vertical = padding)
        )
        Text(
            text = stringResource(R.string.msg_utilization, utilization.asPercentage()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = horizontalMargin)
        )

        CollectionShelfView(
            header = stringResource(R.string.title_owned_games),
            items = games?.first.orEmpty(),
            count = games?.second,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_owned_expansions),
            items = expansions?.first.orEmpty(),
            count = expansions?.second,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_owned_accessories),
            items = accessories?.first.orEmpty(),
            count = accessories?.second,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_recently_acquired),
            helpText = stringResource(R.string.info_recently_acquired),
            items = recentlyAcquired?.first.orEmpty(),
            count = recentlyAcquired?.second,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> item.acquiredFrom to Color.WHITE }

        CollectionShelfView(
            header = stringResource(R.string.title_hawt),
            helpText = stringResource(R.string.info_hawt),
            items = hawt,
            menuRes = R.menu.collection_shelf,
            onMenuClick = browseMenuHandler(context),
        ) { item -> ratingBadge(context, item.averageRating) }
    }
}

@Composable
private fun CollectionPlayTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val syncStatuses by viewModel.syncCollectionStatuses.collectAsStateWithLifecycle()
    val playerCountType by viewModel.playerCountType.collectAsStateWithLifecycle()

    val friendlessShouldPlay by viewModel.friendlessShouldPlayGames.collectAsStateWithLifecycle()
    val wantToPlay by viewModel.wantToPlayItems.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedGames.collectAsStateWithLifecycle()
    val shelfOfOpportunity by viewModel.shelfOfOpportunityItems.collectAsStateWithLifecycle()
    val shelfOfNewOpportunity by viewModel.shelfOfNewOpportunityItems.collectAsStateWithLifecycle()

    var playerCount by rememberSaveable { mutableStateOf<Int?>(null) }

    val padding = dimensionResource(R.dimen.padding_extra)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = padding)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = padding)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            PlayerCountTypeChip(
                label = stringResource(R.string.all),
                selected = playerCountType == CollectionDetailsViewModel.PlayerCountType.All,
                onClick = {
                    viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.All)
                }
            )
            PlayerCountTypeChip(
                label = stringResource(R.string.supports),
                selected = playerCountType == CollectionDetailsViewModel.PlayerCountType.Supports,
                onClick = {
                    viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.Supports)
                }
            )
            PlayerCountTypeChip(
                label = stringResource(R.string.good),
                selected = playerCountType == CollectionDetailsViewModel.PlayerCountType.GoodWith,
                onClick = {
                    viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.GoodWith)
                }
            )
            PlayerCountTypeChip(
                label = stringResource(R.string.best),
                selected = playerCountType == CollectionDetailsViewModel.PlayerCountType.BestWith,
                onClick = {
                    viewModel.filterPlayerCountType(CollectionDetailsViewModel.PlayerCountType.BestWith)
                }
            )
        }

        if (playerCountType != CollectionDetailsViewModel.PlayerCountType.All) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
            Row(
                modifier = Modifier
                    .padding(horizontal = padding)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
            ) {
                listOf(1, 2, 3, 4, 5, 6).forEach { count ->
                    FilterChip(
                        selected = playerCount == count,
                        onClick = {
                            playerCount = count
                            viewModel.filterPlayerCount(count)
                        },
                        label = {
                            Text(
                                text = when (count) {
                                    1 -> stringResource(R.string.one_player)
                                    2 -> stringResource(R.string.two_players)
                                    3 -> stringResource(R.string.three_players)
                                    4 -> stringResource(R.string.four_players)
                                    5 -> stringResource(R.string.five_players)
                                    else -> stringResource(R.string.six_players)
                                }
                            )
                        }
                    )
                }
            }
        }

        CollectionShelfView(
            header = stringResource(R.string.title_friendless_play),
            helpText = stringResource(R.string.info_friendless_play),
            items = friendlessShouldPlay?.first.orEmpty(),
            count = friendlessShouldPlay?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, context),
        ) { item -> ratingBadge(context, item.rating) }

        if (syncStatuses.contains(CollectionStatus.WantToPlay)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_want_to_play),
                items = wantToPlay?.first.orEmpty(),
                count = wantToPlay?.second,
                menuRes = R.menu.collection_shelf_want_to_play,
                onMenuClick = playMenuHandler(context, viewModel, context),
            ) { item -> ratingBadge(context, item.averageRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.title_recently_played),
            items = recentlyPlayed?.first.orEmpty(),
            count = recentlyPlayed?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_shelf_of_opportunity),
            helpText = stringResource(R.string.info_shelf_of_opportunity),
            items = shelfOfOpportunity?.first.orEmpty(),
            count = shelfOfOpportunity?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, context),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_shelf_of_new_opportunity),
            helpText = stringResource(R.string.info_shelf_of_new_opportunity),
            items = shelfOfNewOpportunity?.first.orEmpty(),
            count = shelfOfNewOpportunity?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, context),
        ) { item ->
            item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
        }
    }
}

@Composable
private fun CollectionAcquireTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val syncStatuses by viewModel.syncCollectionStatuses.collectAsStateWithLifecycle()
    val acquireStats by viewModel.collectionAcquireStats.collectAsStateWithLifecycle()
    val preordered by viewModel.preordered.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val wantToBuy by viewModel.wantToBuy.collectAsStateWithLifecycle()
    val wantInTrade by viewModel.wantInTrade.collectAsStateWithLifecycle()
    val favoriteUnowned by viewModel.favoriteUnownedItems.collectAsStateWithLifecycle(emptyList())
    val playedUnowned by viewModel.playedButUnownedItems.collectAsStateWithLifecycle(emptyList())
    val hawtUnowned by viewModel.hawtUnownedItems.collectAsStateWithLifecycle(emptyList())
    val acquiredFrom by viewModel.acquiredFrom.collectAsStateWithLifecycle()

    var itemToAcquire by remember { mutableStateOf<CollectionItem?>(null) }

    val padding = dimensionResource(R.dimen.padding_extra)
    val horizontalMargin = dimensionResource(R.dimen.material_margin_horizontal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = padding)
    ) {
        acquireStats?.let {
            if (it.incomingCount > 0) {
                Text(
                    text = stringResource(
                        R.string.msg_collection_details_acquire,
                        it.incomingCount,
                        it.futureGrowthRate.asPercentage()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = horizontalMargin, vertical = padding)
                )
            }
        }

        if (syncStatuses.contains(CollectionStatus.Preordered)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_preordered),
                items = preordered?.first.orEmpty(),
                count = preordered?.second,
                menuRes = R.menu.collection_shelf_preordered,
                onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
            ) { item ->
                item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
            }
        }

        if (syncStatuses.contains(CollectionStatus.Wishlist)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_wishlist),
                items = wishlist?.first.orEmpty(),
                count = wishlist?.second,
                menuRes = R.menu.collection_shelf_wishlist,
                onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
            ) { item ->
                item.wishListPriority.asWishListPriority(context) to item.wishListPriority.toDouble().toColor(BggColors.fiveStageColors)
            }
        }

        if (syncStatuses.contains(CollectionStatus.WantToBuy)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_want_to_buy),
                items = wantToBuy?.first.orEmpty(),
                count = wantToBuy?.second,
                menuRes = R.menu.collection_shelf_want_to_buy,
                onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
            ) { item -> ratingBadge(context, item.averageRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.collection_status_want_in_trade),
            items = wantInTrade?.first.orEmpty(),
            count = wantInTrade?.second,
            menuRes = R.menu.collection_shelf_want_in_trade,
            onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_favorite_unowned),
            items = favoriteUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
        ) { item -> ratingBadge(context, item.rating) }

        CollectionShelfView(
            header = stringResource(R.string.title_played_unowned),
            items = playedUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
        ) { item -> context.getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE }

        CollectionShelfView(
            header = stringResource(R.string.title_hawt_unowned),
            items = hawtUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, context) { itemToAcquire = it },
        ) { item -> ratingBadge(context, item.averageRating) }
    }

    itemToAcquire?.let { item ->
        AcquireCollectionItemDialog(
            item = item,
            acquiredFromOptions = acquiredFrom,
            onDismiss = { itemToAcquire = null },
            onConfirm = { priceCurrency, pricePaid, quantity, acquisitionDate, acquiredFromValue ->
                viewModel.markedAsAcquired(
                    internalId = item.internalId,
                    priceCurrency = priceCurrency,
                    pricePaid = pricePaid,
                    quantity = quantity,
                    acquisitionDate = acquisitionDate,
                    acquiredFrom = acquiredFromValue,
                )
                itemToAcquire = null
            }
        )
    }
}

@Composable
private fun CollectionDivestTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val syncStatuses by viewModel.syncCollectionStatuses.collectAsStateWithLifecycle()
    val own by viewModel.own.collectAsStateWithLifecycle()
    val regretFactor by viewModel.regretFactor.collectAsStateWithLifecycle(0)
    val forTrade by viewModel.forTrade.collectAsStateWithLifecycle()
    val forTradeWithoutCondition by viewModel.forTradeWithoutCondition.collectAsStateWithLifecycle()
    val previouslyOwned by viewModel.previouslyOwned.collectAsStateWithLifecycle()
    val whyOwn by viewModel.whyOwnItems.collectAsStateWithLifecycle()
    var itemForConditionEdit by remember { mutableStateOf<CollectionItem?>(null) }

    val padding = dimensionResource(R.dimen.padding_extra)
    val horizontalMargin = dimensionResource(R.dimen.material_margin_horizontal)

    val ownCount = own?.second ?: 0
    val previouslyOwnedCount = previouslyOwned?.second
    val forTradeCount = forTrade?.second
    val divestSummary = calculateDivestSummary(
        context = context,
        ownCount = ownCount,
        previouslyOwnedCount = previouslyOwnedCount,
        forTradeCount = forTradeCount
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = padding)
    ) {
        divestSummary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = horizontalMargin, vertical = padding)
            )
        }

        if (regretFactor > 0) {
            Text(
                text = stringResource(R.string.msg_regret_factor, regretFactor),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = horizontalMargin)
            )
        }

        if (syncStatuses.contains(CollectionStatus.ForTrade)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_for_trade),
                items = forTrade?.first.orEmpty(),
                count = forTrade?.second,
                menuRes = R.menu.collection_shelf_for_trade,
                onMenuClick = divestMenuHandler(context, viewModel, context) { itemForConditionEdit = it },
            ) { item -> ratingBadge(context, item.geekRating) }

            CollectionShelfView(
                header = stringResource(R.string.title_for_trade_without_condition),
                items = forTradeWithoutCondition?.first.orEmpty(),
                count = forTradeWithoutCondition?.second,
                menuRes = R.menu.collection_shelf_divest_for_trade_without_condition,
                onMenuClick = divestMenuHandler(context, viewModel, context) { itemForConditionEdit = it },
            ) { item -> ratingBadge(context, item.geekRating) }
        }

        if (syncStatuses.contains(CollectionStatus.PreviouslyOwned)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_prev_owned),
                items = previouslyOwned?.first.orEmpty(),
                count = previouslyOwned?.second,
                menuRes = R.menu.collection_shelf,
                onMenuClick = divestMenuHandler(context, viewModel, context) { itemForConditionEdit = it },
            ) { item -> ratingBadge(context, item.geekRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.title_why_own),
            helpText = stringResource(R.string.info_why_own),
            items = whyOwn?.first.orEmpty(),
            count = whyOwn?.second,
            menuRes = R.menu.collection_shelf_offer_trade,
            onMenuClick = divestMenuHandler(context, viewModel, context) { itemForConditionEdit = it },
        ) { item ->
            val dateFormat = DateFormat.getDateFormat(context)
            (item.lastPlayDate?.let { dateFormat.format(it) } ?: "") to Color.WHITE
        }
    }

    itemForConditionEdit?.let { item ->
        EditTextDialog(
            title = stringResource(R.string.menu_add_condition_text),
            subtitle = item.gameName,
            initialValue = item.conditionText,
            onDismiss = { itemForConditionEdit = null },
            onConfirm = {
                viewModel.updateCondition(item.internalId, it)
                itemForConditionEdit = null
            }
        )
    }
}

@Composable
private fun CollectionAnalyzeTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val analyzeStats by viewModel.collectionAnalyzeStats.collectAsStateWithLifecycle()
    val ratable by viewModel.ratableItems.collectAsStateWithLifecycle()
    val commentable by viewModel.commentableItems.collectAsStateWithLifecycle()
    var itemForRating by remember { mutableStateOf<CollectionItem?>(null) }
    var itemForComment by remember { mutableStateOf<CollectionItem?>(null) }

    val padding = dimensionResource(R.dimen.padding_extra)
    val horizontalMargin = dimensionResource(R.dimen.material_margin_horizontal)

    val summary = analyzeStats?.let {
        val displayFormat = DecimalFormat("0.00")
        stringResource(
            R.string.collection_analyze_summary,
            it.averagePersonalRating.asPersonalRating(context),
            it.averageAverageRating.asBoundedRating(context, format = displayFormat),
            it.correlationCoefficient.asScore(context, format = displayFormat)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = padding)
    ) {
        summary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = horizontalMargin, vertical = padding)
            )
        }

        CollectionShelfView(
            header = stringResource(R.string.title_games_to_rate),
            items = ratable?.first.orEmpty(),
            count = ratable?.second,
            menuRes = R.menu.collection_shelf_rate,
            onMenuClick = analyzeMenuHandler(context, onShowRatingDialog = { itemForRating = it }, onShowCommentDialog = { itemForComment = it }),
        ) { item -> context.getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE }

        CollectionShelfView(
            header = stringResource(R.string.title_games_to_comment),
            items = commentable?.first.orEmpty(),
            count = commentable?.second,
            menuRes = R.menu.collection_shelf_comment,
            onMenuClick = analyzeMenuHandler(context, onShowRatingDialog = { itemForRating = it }, onShowCommentDialog = { itemForComment = it }),
        ) { item -> ratingBadge(context, item.rating) }
    }

    itemForRating?.let { item ->
        RatingDialog(
            gameName = item.gameName,
            initialValue = item.rating.takeIf { it > 0.0 }?.toString().orEmpty(),
            onDismiss = { itemForRating = null },
            onConfirm = { rating ->
                viewModel.updateRating(item.internalId, rating)
                itemForRating = null
            }
        )
    }

    itemForComment?.let { item ->
        EditTextDialog(
            title = stringResource(R.string.comment),
            subtitle = item.gameName,
            initialValue = item.comment,
            onDismiss = { itemForComment = null },
            onConfirm = {
                viewModel.updateComment(item.internalId, it)
                itemForComment = null
            }
        )
    }
}

@Composable
private fun CollectionCreditsTab() {
    val padding = dimensionResource(R.dimen.padding_standard)

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(padding)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(padding)) {
            CreditButton(
                text = stringResource(R.string.title_designers),
                icon = Icons.Filled.Edit,
                onClick = { context.startActivity<DesignersActivity>() }
            )
            CreditButton(
                text = stringResource(R.string.title_artists),
                icon = Icons.Filled.Brush,
                onClick = { context.startActivity<ArtistsActivity>() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(padding)) {
            CreditButton(
                text = stringResource(R.string.title_publishers),
                icon = Icons.Filled.ImportContacts,
                onClick = { context.startActivity<PublishersActivity>() }
            )
            CreditButton(
                text = stringResource(R.string.title_mechanics),
                icon = Icons.Filled.Settings,
                onClick = { context.startActivity<MechanicsActivity>() }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(padding)) {
            CreditButton(
                text = stringResource(R.string.title_categories),
                icon = Icons.Filled.Category,
                onClick = { context.startActivity<CategoriesActivity>() }
            )
        }
    }
}

@Composable
private fun CreditButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors()) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_small)))
        Text(text = text)
    }
}

@Composable
private fun PlayerCountTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun CollectionShelfView(
    header: String,
    items: List<CollectionItem>,
    menuRes: Int,
    onMenuClick: ((CollectionItem, Int) -> Boolean)?,
    count: Int? = null,
    helpText: String? = null,
    badge: ((CollectionItem) -> Pair<CharSequence, Int>)? = null,
) {
    val context = LocalContext.current
    if (items.isEmpty()) return

    val headerText = count?.let { "$header - $it" } ?: header
    val horizontalPadding = dimensionResource(R.dimen.activity_horizontal_margin)
    val spacing = dimensionResource(R.dimen.padding_small)
    val menuOptions = remember(menuRes) { shelfMenuOptions(menuRes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_half))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (!helpText.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        context.showClickableAlertDialog(ResourcesCompat.ID_NULL, helpText)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.information)
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(items, key = { it.internalId }) { item ->
                CollectionShelfItemCard(
                    item = item,
                    menuOptions = menuOptions,
                    badge = badge?.invoke(item),
                    onClick = {
                        GameActivity.start(
                            context = context,
                            gameId = item.gameId,
                            gameName = item.gameName,
                            thumbnailUrl = item.thumbnailUrl,
                            heroImageUrl = item.heroImageUrl
                        )
                    },
                    onLongClick = {
                        GameActivity.start(
                            context = context,
                            gameId = item.gameId,
                            gameName = item.gameName,
                            thumbnailUrl = item.thumbnailUrl,
                            heroImageUrl = item.heroImageUrl
                        )
                    },
                    onMenuClick = { menuItemId ->
                        onMenuClick?.invoke(item, menuItemId) ?: false
                    }
                )
            }
        }
    }
}

private data class ShelfMenuOption(
    val id: Int,
    val titleRes: Int
)

private fun shelfMenuOptions(@MenuRes menuRes: Int): List<ShelfMenuOption> = when (menuRes) {
    R.menu.collection_shelf -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
    )
    R.menu.collection_shelf_play -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_play_game, R.string.menu_log_play),
    )
    R.menu.collection_shelf_want_to_play -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_play_game, R.string.menu_log_play),
        ShelfMenuOption(R.id.menu_remove_want_to_play, R.string.menu_remove_want_to_play),
    )
    R.menu.collection_shelf_acquire -> listOf(
        ShelfMenuOption(R.id.menu_acquire, R.string.title_acquire),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
    )
    R.menu.collection_shelf_preordered -> listOf(
        ShelfMenuOption(R.id.menu_acquire, R.string.title_acquire),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_preordered, R.string.menu_remove_preordered),
    )
    R.menu.collection_shelf_wishlist -> listOf(
        ShelfMenuOption(R.id.menu_acquire, R.string.title_acquire),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_wishlist, R.string.menu_remove_wishlist),
    )
    R.menu.collection_shelf_want_to_buy -> listOf(
        ShelfMenuOption(R.id.menu_acquire, R.string.title_acquire),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_want_to_buy, R.string.menu_remove_want_to_buy),
    )
    R.menu.collection_shelf_want_in_trade -> listOf(
        ShelfMenuOption(R.id.menu_acquire, R.string.title_acquire),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_want_in_trade, R.string.menu_remove_want_in_trade),
    )
    R.menu.collection_shelf_for_trade -> listOf(
        ShelfMenuOption(R.id.menu_trade, R.string.title_trade),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_for_trade, R.string.menu_remove_for_trade),
    )
    R.menu.collection_shelf_divest_for_trade_without_condition -> listOf(
        ShelfMenuOption(R.id.menu_add_condition_text, R.string.menu_add_condition_text),
        ShelfMenuOption(R.id.menu_trade, R.string.title_trade),
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_remove_for_trade, R.string.menu_remove_for_trade),
    )
    R.menu.collection_shelf_offer_trade -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_offer_trade, R.string.offer_for_trade),
    )
    R.menu.collection_shelf_rate -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_rate_item, R.string.menu_rate_item),
    )
    R.menu.collection_shelf_comment -> listOf(
        ShelfMenuOption(R.id.menu_view_game, R.string.menu_view_game),
        ShelfMenuOption(R.id.menu_view_item, R.string.menu_view_item),
        ShelfMenuOption(R.id.menu_comment_item, R.string.menu_comment_item),
    )
    else -> emptyList()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionShelfItemCard(
    item: CollectionItem,
    menuOptions: List<ShelfMenuOption>,
    badge: Pair<CharSequence, Int>?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: (Int) -> Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val cardWidth = dimensionResource(R.dimen.card_width)

    Card(
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.gameName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.thumbnail_image_empty),
                    error = painterResource(id = R.drawable.thumbnail_image_empty)
                )
                badge?.let { (text, colorInt) ->
                    Surface(
                        color = androidx.compose.ui.graphics.Color(colorInt),
                        contentColor = androidx.compose.ui.graphics.Color(colorInt.getTextColor()),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(dimensionResource(R.dimen.padding_half))
                    ) {
                        Text(
                            text = text.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = item.gameName,
                style = MaterialTheme.typography.bodyMedium,
                minLines = 2,
                maxLines = 2,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_half),
                    vertical = 2.dp
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimensionResource(R.dimen.padding_half), end = dimensionResource(R.dimen.padding_half)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.yearPublished.asYear(LocalContext.current),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (menuOptions.isNotEmpty()) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { expanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            menuOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.titleRes)) },
                                    onClick = {
                                        expanded = false
                                        onMenuClick(option.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    subtitle: String?,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))) {
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun RatingDialog(
    gameName: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val parsed = value.toDoubleOrNull()
    val isValid = parsed != null && parsed in 1.0..10.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.rating)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))) {
                Text(text = gameName, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { parsed?.let(onConfirm) }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquireCollectionItemDialog(
    item: CollectionItem,
    acquiredFromOptions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String?, Double?, Int?, Long?, String?) -> Unit,
) {
    var selectedCurrency by remember(item.internalId) { mutableStateOf(item.pricePaidCurrency.orEmpty()) }
    var price by remember(item.internalId) {
        mutableStateOf(
            if (item.pricePaid == 0.0) "" else DecimalFormat("0.00").format(item.pricePaid)
        )
    }
    var quantity by remember(item.internalId) { mutableStateOf(item.quantity.toString()) }
    var acquiredFrom by remember(item.internalId) { mutableStateOf(item.acquiredFrom.orEmpty()) }
    var acquisitionDate by remember(item.internalId) { mutableStateOf(item.acquisitionDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var acquiredFromExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currencyOptions = remember(context) { listOf(*context.resources.getStringArray(R.array.currency)) }

    if (showDatePicker) {
        val initialDate = if (acquisitionDate == 0L) null else acquisitionDate.fromLocalToUtc()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        acquisitionDate = datePickerState.selectedDateMillis?.fromLocalToUtc() ?: 0L
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.title_buy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))) {
                Text(text = item.collectionName, style = MaterialTheme.typography.bodyMedium)

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.price)) },
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedCurrency = option
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.price)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quantity)) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = acquisitionDate.formatDateTime(LocalContext.current, 0, DateUtils.FORMAT_SHOW_DATE).toString()
                            .ifBlank { stringResource(R.string.acquisition_date) },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.acquisition_date))
                    }
                    if (acquisitionDate != 0L) {
                        TextButton(onClick = { acquisitionDate = 0L }) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = acquiredFromExpanded,
                    onExpandedChange = { acquiredFromExpanded = !acquiredFromExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = acquiredFrom,
                        onValueChange = {
                            acquiredFrom = it
                            acquiredFromExpanded = true
                        },
                        label = { Text(stringResource(R.string.acquired_from)) },
                    )
                    ExposedDropdownMenu(
                        expanded = acquiredFromExpanded && acquiredFromOptions.isNotEmpty(),
                        onDismissRequest = { acquiredFromExpanded = false }
                    ) {
                        acquiredFromOptions
                            .filter { option -> acquiredFrom.isBlank() || option.contains(acquiredFrom, ignoreCase = true) }
                            .take(8)
                            .forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        acquiredFrom = option
                                        acquiredFromExpanded = false
                                    }
                                )
                            }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedCurrency.ifBlank { null },
                        price.toDoubleOrNull(),
                        quantity.toIntOrNull(),
                        acquisitionDate,
                        acquiredFrom.trim().ifBlank { null },
                    )
                }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private fun browseMenuHandler(context: android.content.Context) =
    { item: CollectionItem, menuItemId: Int ->
        when (menuItemId) {
            R.id.menu_view_game -> {
                GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                true
            }
            R.id.menu_view_item -> {
                GameCollectionItemActivity.start(context, item)
                true
            }
            else -> false
        }
    }

private fun playMenuHandler(
    context: android.content.Context,
    viewModel: CollectionDetailsViewModel,
    dialogContext: android.content.Context
) = { item: CollectionItem, menuItemId: Int ->
    when (menuItemId) {
        R.id.menu_view_game -> {
            GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
            true
        }
        R.id.menu_view_item -> {
            GameCollectionItemActivity.start(context, item)
            true
        }
        R.id.menu_play_game -> {
            when (context.preferences().logPlayPreference()) {
                LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                    context,
                    item.gameId,
                    item.gameName,
                    item.robustHeroImageUrl,
                    item.arePlayersCustomSorted
                )
                LOG_PLAY_TYPE_QUICK -> {
                    dialogContext.createThemedBuilder()
                        .setMessage(context.getString(R.string.are_you_sure_log_quick_play, item.gameName))
                        .setPositiveButton(R.string.title_log_play) { _, _ ->
                            viewModel.logQuickPlay(item.gameId, item.gameName)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                }
                else -> LogPlayActivity.logPlay(
                    context,
                    item.gameId,
                    item.gameName,
                    item.robustHeroImageUrl,
                    item.arePlayersCustomSorted
                )
            }
            true
        }
        R.id.menu_remove_want_to_play -> {
            dialogContext.createThemedBuilder()
                .setTitle(item.collectionName)
                .setMessage(context.getString(R.string.msg_remove_status, context.getString(R.string.collection_status_want_to_play)))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
                    viewModel.removeStatus(item.internalId, CollectionStatus.WantToPlay)
                }
                .create()
                .show()
            true
        }
        else -> false
    }
}

private fun acquireMenuHandler(
    context: android.content.Context,
    viewModel: CollectionDetailsViewModel,
    dialogContext: android.content.Context,
    onShowAcquireDialog: (CollectionItem) -> Unit,
) = { item: CollectionItem, menuItemId: Int ->
    when (menuItemId) {
        R.id.menu_acquire -> {
            onShowAcquireDialog(item)
            true
        }
        R.id.menu_view_game -> {
            GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
            true
        }
        R.id.menu_view_item -> {
            GameCollectionItemActivity.start(context, item)
            true
        }
        R.id.menu_remove_preordered -> {
            confirmRemoveStatus(dialogContext, item, R.string.collection_status_preordered, CollectionStatus.Preordered, viewModel)
            true
        }
        R.id.menu_remove_wishlist -> {
            confirmRemoveStatus(dialogContext, item, R.string.collection_status_wishlist, CollectionStatus.Wishlist, viewModel)
            true
        }
        R.id.menu_remove_want_to_buy -> {
            confirmRemoveStatus(dialogContext, item, R.string.collection_status_want_to_buy, CollectionStatus.WantToBuy, viewModel)
            true
        }
        R.id.menu_remove_want_in_trade -> {
            confirmRemoveStatus(dialogContext, item, R.string.collection_status_want_in_trade, CollectionStatus.WantInTrade, viewModel)
            true
        }
        else -> false
    }
}

private fun divestMenuHandler(
    context: android.content.Context,
    viewModel: CollectionDetailsViewModel,
    dialogContext: android.content.Context,
    onShowConditionDialog: (CollectionItem) -> Unit,
) = { item: CollectionItem, menuItemId: Int ->
    when (menuItemId) {
        R.id.menu_remove_for_trade -> {
            dialogContext.createThemedBuilder()
                .setTitle(item.collectionName)
                .setMessage(context.getString(R.string.msg_remove_status, context.getString(R.string.collection_status_for_trade)))
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
                    viewModel.removeStatus(item.internalId, CollectionStatus.ForTrade)
                }
                .create()
                .show()
            true
        }
        R.id.menu_add_condition_text -> {
            onShowConditionDialog(item)
            true
        }
        R.id.menu_offer_trade -> {
            viewModel.addStatus(item.internalId, CollectionStatus.ForTrade)
            true
        }
        R.id.menu_trade -> {
            dialogContext.createThemedBuilder()
                .setTitle(item.collectionName)
                .setMessage(R.string.msg_confirm_trade)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.title_trade) { _: DialogInterface?, _: Int ->
                    viewModel.markAsTraded(item.internalId)
                }
                .create()
                .show()
            true
        }
        R.id.menu_view_game -> {
            GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
            true
        }
        R.id.menu_view_item -> {
            GameCollectionItemActivity.start(context, item)
            true
        }
        else -> false
    }
}

private fun analyzeMenuHandler(
    context: android.content.Context,
    onShowRatingDialog: (CollectionItem) -> Unit,
    onShowCommentDialog: (CollectionItem) -> Unit,
) = { item: CollectionItem, menuItemId: Int ->
    when (menuItemId) {
        R.id.menu_view_game -> {
            GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
            true
        }
        R.id.menu_view_item -> {
            GameCollectionItemActivity.start(context, item)
            true
        }
        R.id.menu_rate_item -> {
            onShowRatingDialog(item)
            true
        }
        R.id.menu_comment_item -> {
            onShowCommentDialog(item)
            true
        }
        else -> false
    }
}

private fun confirmRemoveStatus(
    context: android.content.Context,
    item: CollectionItem,
    statusResId: Int,
    status: CollectionStatus,
    viewModel: CollectionDetailsViewModel
) {
    context.createThemedBuilder()
        .setTitle(item.collectionName)
        .setMessage(context.getString(R.string.msg_remove_status, context.getString(statusResId)))
        .setCancelable(true)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.remove) { _: DialogInterface?, _: Int ->
            viewModel.removeStatus(item.internalId, status)
        }
        .create()
        .show()
}

private fun ratingBadge(context: android.content.Context, rating: Double): Pair<String, Int> {
    val text = rating.asPersonalRating(context, ResourcesCompat.ID_NULL)
    val color = rating.toColor(BggColors.ratingColors)
    return text to color
}

private fun calculateDivestSummary(
    context: android.content.Context,
    ownCount: Int,
    previouslyOwnedCount: Int?,
    forTradeCount: Int?
): String? {
    if (ownCount <= 0) return null
    return when {
        previouslyOwnedCount != null && forTradeCount != null -> {
            val previouslyOwnedRatio = previouslyOwnedCount.toDouble() / ownCount
            val forTradeRatio = forTradeCount.toDouble() / ownCount
            context.getString(
                R.string.msg_collection_details_divest,
                previouslyOwnedRatio.asPercentage(),
                forTradeRatio.asPercentage()
            )
        }
        previouslyOwnedCount != null -> {
            val previouslyOwnedRatio = previouslyOwnedCount.toDouble() / ownCount
            context.getString(
                R.string.msg_collection_details_divest_previously_owned,
                previouslyOwnedRatio.asPercentage()
            )
        }
        forTradeCount != null -> {
            val forTradeRatio = forTradeCount.toDouble() / ownCount
            context.getString(
                R.string.msg_collection_details_divest_for_trade,
                forTradeRatio.asPercentage()
            )
        }
        else -> null
    }
}
