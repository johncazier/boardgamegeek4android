package com.boardgamegeek.ui.collectiondetails

import android.content.DialogInterface
import android.graphics.Color
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.ui.widget.CollectionShelf
import com.boardgamegeek.ui.ArtistsActivity
import com.boardgamegeek.ui.CategoriesActivity
import com.boardgamegeek.ui.DesignersActivity
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.GameCollectionItemActivity
import com.boardgamegeek.ui.LogPlayActivity
import com.boardgamegeek.ui.MechanicsActivity
import com.boardgamegeek.ui.NewPlayActivity
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
    val recentlyViewed by viewModel.recentlyViewedItems.observeAsState(emptyList())
    val friendlessFavorites by viewModel.friendlessFavoriteItems.observeAsState(emptyList())
    val friendless by viewModel.friendless.observeAsState(0)
    val underrated by viewModel.underratedItems.observeAsState(emptyList())
    val hawt by viewModel.hawtItems.observeAsState(emptyList())

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
    val growthRate by viewModel.growthRate.observeAsState(0)
    val utilization by viewModel.utilization.observeAsState(0.0)
    val games by viewModel.own.observeAsState()
    val expansions by viewModel.expansions.observeAsState()
    val accessories by viewModel.accessories.observeAsState()
    val recentlyAcquired by viewModel.recentlyAcquired.observeAsState()
    val hawt by viewModel.hawtItems.observeAsState(emptyList())

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
    val activity = context as FragmentActivity
    val syncStatuses = viewModel.syncCollectionStatuses.observeAsState(emptySet()).value.orEmpty()
    val playerCountType by viewModel.playerCountType.observeAsState(CollectionDetailsViewModel.PlayerCountType.All)

    val friendlessShouldPlay by viewModel.friendlessShouldPlayGames.observeAsState()
    val wantToPlay by viewModel.wantToPlayItems.observeAsState()
    val recentlyPlayed by viewModel.recentlyPlayedGames.observeAsState()
    val shelfOfOpportunity by viewModel.shelfOfOpportunityItems.observeAsState()
    val shelfOfNewOpportunity by viewModel.shelfOfNewOpportunityItems.observeAsState()

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
            onMenuClick = playMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.rating) }

        if (syncStatuses.contains(CollectionStatus.WantToPlay)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_want_to_play),
                items = wantToPlay?.first.orEmpty(),
                count = wantToPlay?.second,
                menuRes = R.menu.collection_shelf_want_to_play,
                onMenuClick = playMenuHandler(context, viewModel, activity),
            ) { item -> ratingBadge(context, item.averageRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.title_recently_played),
            items = recentlyPlayed?.first.orEmpty(),
            count = recentlyPlayed?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_shelf_of_opportunity),
            helpText = stringResource(R.string.info_shelf_of_opportunity),
            items = shelfOfOpportunity?.first.orEmpty(),
            count = shelfOfOpportunity?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_shelf_of_new_opportunity),
            helpText = stringResource(R.string.info_shelf_of_new_opportunity),
            items = shelfOfNewOpportunity?.first.orEmpty(),
            count = shelfOfNewOpportunity?.second,
            menuRes = R.menu.collection_shelf_play,
            onMenuClick = playMenuHandler(context, viewModel, activity),
        ) { item ->
            item.acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL) to Color.WHITE
        }
    }
}

@Composable
private fun CollectionAcquireTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val syncStatuses = viewModel.syncCollectionStatuses.observeAsState(emptySet()).value.orEmpty()
    val acquireStats by viewModel.collectionAcquireStats.observeAsState()
    val preordered by viewModel.preordered.observeAsState()
    val wishlist by viewModel.wishlist.observeAsState()
    val wantToBuy by viewModel.wantToBuy.observeAsState()
    val wantInTrade by viewModel.wantInTrade.observeAsState()
    val favoriteUnowned by viewModel.favoriteUnownedItems.observeAsState(emptyList())
    val playedUnowned by viewModel.playedButUnownedItems.observeAsState(emptyList())
    val hawtUnowned by viewModel.hawtUnownedItems.observeAsState(emptyList())

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
                onMenuClick = acquireMenuHandler(context, viewModel, activity),
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
                onMenuClick = acquireMenuHandler(context, viewModel, activity),
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
                onMenuClick = acquireMenuHandler(context, viewModel, activity),
            ) { item -> ratingBadge(context, item.averageRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.collection_status_want_in_trade),
            items = wantInTrade?.first.orEmpty(),
            count = wantInTrade?.second,
            menuRes = R.menu.collection_shelf_want_in_trade,
            onMenuClick = acquireMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.averageRating) }

        CollectionShelfView(
            header = stringResource(R.string.title_favorite_unowned),
            items = favoriteUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.rating) }

        CollectionShelfView(
            header = stringResource(R.string.title_played_unowned),
            items = playedUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, activity),
        ) { item -> context.getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE }

        CollectionShelfView(
            header = stringResource(R.string.title_hawt_unowned),
            items = hawtUnowned,
            menuRes = R.menu.collection_shelf_acquire,
            onMenuClick = acquireMenuHandler(context, viewModel, activity),
        ) { item -> ratingBadge(context, item.averageRating) }
    }
}

@Composable
private fun CollectionDivestTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val syncStatuses = viewModel.syncCollectionStatuses.observeAsState(emptySet()).value.orEmpty()
    val own by viewModel.own.observeAsState()
    val regretFactor by viewModel.regretFactor.observeAsState(0)
    val forTrade by viewModel.forTrade.observeAsState()
    val forTradeWithoutCondition by viewModel.forTradeWithoutCondition.observeAsState()
    val previouslyOwned by viewModel.previouslyOwned.observeAsState()
    val whyOwn by viewModel.whyOwnItems.observeAsState()

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
                onMenuClick = divestMenuHandler(context, viewModel, activity),
            ) { item -> ratingBadge(context, item.geekRating) }

            CollectionShelfView(
                header = stringResource(R.string.title_for_trade_without_condition),
                items = forTradeWithoutCondition?.first.orEmpty(),
                count = forTradeWithoutCondition?.second,
                menuRes = R.menu.collection_shelf_divest_for_trade_without_condition,
                onMenuClick = divestMenuHandler(context, viewModel, activity),
            ) { item -> ratingBadge(context, item.geekRating) }
        }

        if (syncStatuses.contains(CollectionStatus.PreviouslyOwned)) {
            CollectionShelfView(
                header = stringResource(R.string.collection_status_prev_owned),
                items = previouslyOwned?.first.orEmpty(),
                count = previouslyOwned?.second,
                menuRes = R.menu.collection_shelf,
                onMenuClick = divestMenuHandler(context, viewModel, activity),
            ) { item -> ratingBadge(context, item.geekRating) }
        }

        CollectionShelfView(
            header = stringResource(R.string.title_why_own),
            helpText = stringResource(R.string.info_why_own),
            items = whyOwn?.first.orEmpty(),
            count = whyOwn?.second,
            menuRes = R.menu.collection_shelf_offer_trade,
            onMenuClick = divestMenuHandler(context, viewModel, activity),
        ) { item ->
            val dateFormat = DateFormat.getDateFormat(context)
            (item.lastPlayDate?.let { dateFormat.format(it) } ?: "") to Color.WHITE
        }
    }
}

@Composable
private fun CollectionAnalyzeTab(viewModel: CollectionDetailsViewModel) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val analyzeStats by viewModel.collectionAnalyzeStats.observeAsState()
    val ratable by viewModel.ratableItems.observeAsState()
    val commentable by viewModel.commentableItems.observeAsState()

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
            onMenuClick = analyzeMenuHandler(context, activity),
        ) { item -> context.getQuantityText(R.plurals.plays_suffix, item.numberOfPlays, item.numberOfPlays) to Color.WHITE }

        CollectionShelfView(
            header = stringResource(R.string.title_games_to_comment),
            items = commentable?.first.orEmpty(),
            count = commentable?.second,
            menuRes = R.menu.collection_shelf_comment,
            onMenuClick = analyzeMenuHandler(context, activity),
        ) { item -> ratingBadge(context, item.rating) }
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
    onMenuClick: ((CollectionItem, MenuItem) -> Boolean)?,
    count: Int? = null,
    helpText: String? = null,
    badge: ((CollectionItem) -> Pair<CharSequence, Int>)? = null,
) {
    val context = LocalContext.current
    val adapter = remember(menuRes, onMenuClick, badge) {
        CollectionShelf.CollectionItemAdapter(menuRes, onMenuClick, badge)
    }
    val headerText = count?.let { "$header - $it" } ?: header

    AndroidView(
        factory = { ctx ->
            CollectionShelf(ctx).apply {
                setAdapter(adapter)
            }
        },
        update = { view ->
            view.findViewById<RecyclerView>(R.id.recyclerView).apply {
                isNestedScrollingEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            view.findViewById<TextView>(R.id.headerView).text = headerText
            view.findViewById<ImageView>(R.id.infoView).apply {
                isVisible = !helpText.isNullOrBlank()
                setOnClickListener {
                    helpText?.let { context.showClickableAlertDialog(ResourcesCompat.ID_NULL, it) }
                }
            }
            adapter.items = items
            view.bindList(items)
        }
    )
}

private fun browseMenuHandler(context: android.content.Context) =
    { item: CollectionItem, menuItem: MenuItem ->
        when (menuItem.itemId) {
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
    activity: FragmentActivity
) = { item: CollectionItem, menuItem: MenuItem ->
    when (menuItem.itemId) {
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
                    activity.createThemedBuilder()
                        .setMessage(context.getString(R.string.are_you_sure_log_quick_play, item.gameName))
                        .setPositiveButton(R.string.title_log_play) { _, _ ->
                            viewModel.logQuickPlay(item.gameId, item.gameName)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                }
                LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(context, item.gameId, item.gameName)
            }
            true
        }
        R.id.menu_remove_want_to_play -> {
            activity.createThemedBuilder()
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
    activity: FragmentActivity
) = { item: CollectionItem, menuItem: MenuItem ->
    when (menuItem.itemId) {
        R.id.menu_acquire -> {
            val dialog = CollectionDetailPrivateInfoDialogFragment.newInstance(
                item.internalId,
                item.collectionName,
                item.pricePaidCurrency,
                item.pricePaid,
                item.quantity,
                item.acquisitionDate,
                item.acquiredFrom,
            )
            activity.showAndSurvive(dialog)
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
            confirmRemoveStatus(activity, item, R.string.collection_status_preordered, CollectionStatus.Preordered, viewModel)
            true
        }
        R.id.menu_remove_wishlist -> {
            confirmRemoveStatus(activity, item, R.string.collection_status_wishlist, CollectionStatus.Wishlist, viewModel)
            true
        }
        R.id.menu_remove_want_to_buy -> {
            confirmRemoveStatus(activity, item, R.string.collection_status_want_to_buy, CollectionStatus.WantToBuy, viewModel)
            true
        }
        R.id.menu_remove_want_in_trade -> {
            confirmRemoveStatus(activity, item, R.string.collection_status_want_in_trade, CollectionStatus.WantInTrade, viewModel)
            true
        }
        else -> false
    }
}

private fun divestMenuHandler(
    context: android.content.Context,
    viewModel: CollectionDetailsViewModel,
    activity: FragmentActivity
) = { item: CollectionItem, menuItem: MenuItem ->
    when (menuItem.itemId) {
        R.id.menu_remove_for_trade -> {
            activity.createThemedBuilder()
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
            CollectionDetailsConditionDialogFragment.show(
                activity.supportFragmentManager,
                item.gameName,
                item.internalId,
                item.conditionText
            )
            true
        }
        R.id.menu_offer_trade -> {
            viewModel.addStatus(item.internalId, CollectionStatus.ForTrade)
            true
        }
        R.id.menu_trade -> {
            activity.createThemedBuilder()
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
    activity: FragmentActivity
) = { item: CollectionItem, menuItem: MenuItem ->
    when (menuItem.itemId) {
        R.id.menu_view_game -> {
            GameActivity.start(context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
            true
        }
        R.id.menu_view_item -> {
            GameCollectionItemActivity.start(context, item)
            true
        }
        R.id.menu_rate_item -> {
            val fragment = CollectionDetailsRatingNumberPadDialogFragment.newInstance(item.internalId, item.gameName)
            activity.showAndSurvive(fragment)
            true
        }
        R.id.menu_comment_item -> {
            CollectionDetailsCommentDialogFragment.show(
                activity.supportFragmentManager,
                R.string.comment,
                item.gameName,
                item.internalId,
                item.comment
            )
            true
        }
        else -> false
    }
}

private fun confirmRemoveStatus(
    activity: FragmentActivity,
    item: CollectionItem,
    statusResId: Int,
    status: CollectionStatus,
    viewModel: CollectionDetailsViewModel
) {
    activity.createThemedBuilder()
        .setTitle(item.collectionName)
        .setMessage(activity.getString(R.string.msg_remove_status, activity.getString(statusResId)))
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
