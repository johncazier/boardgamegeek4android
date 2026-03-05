package com.boardgamegeek.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_FORM
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_QUICK
import com.boardgamegeek.extensions.LOG_PLAY_TYPE_WIZARD
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.logPlayPreference
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.shareGame
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.ImageActivity
import com.boardgamegeek.ui.LogPlayActivity
import com.boardgamegeek.ui.NewPlayActivity
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment
import com.boardgamegeek.ui.game.GameViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    gameId: Int,
    initialGameName: String,
    initialHeroUrl: String,
    initialThumbnailUrl: String,
    initialImageUrl: String,
    initialArePlayersCustomSorted: Boolean,
    initialIsFavorite: Boolean,
    initialIsUserMenuEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val game by viewModel.game.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val syncCollectionPref by viewModel.syncCollectionPreference.collectAsStateWithLifecycle()
    val syncPlaysPref by viewModel.syncPlaysPreference.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val gameName = game?.name ?: initialGameName
    val heroUrl = game?.heroImageUrl ?: initialHeroUrl
    val thumbnailUrl = game?.thumbnailUrl ?: initialThumbnailUrl
    val imageUrl = game?.imageUrl ?: initialImageUrl
    val arePlayersCustomSorted = game?.customPlayerSort ?: initialArePlayersCustomSorted
    val isFavorite = game?.isFavorite ?: initialIsFavorite
    val isUserMenuEnabled = (game?.maxUsers ?: if (initialIsUserMenuEnabled) 1 else 0) > 0
    val iconColor = game?.iconColor ?: Color.Transparent.value.toInt()
    val (fabContainerColor, fabContentColor) = rememberGameFabColors(iconColor)

    val isSignedIn = !username.isNullOrBlank()
    val shouldShowCollection = !syncCollectionPref.isNullOrEmpty()
    val shouldShowPlays = syncPlaysPref == true

    val tabs = remember(
        isSignedIn,
        shouldShowCollection,
        shouldShowPlays,
        isFavorite,
        gameId,
        gameName,
        heroUrl,
        thumbnailUrl,
        imageUrl,
        arePlayersCustomSorted
    ) {
        buildGameTabs(
            gameId = gameId,
            gameName = gameName,
            isFavorite = isFavorite,
            isSignedIn = isSignedIn,
            shouldShowCollection = shouldShowCollection,
            shouldShowPlays = shouldShowPlays,
            heroUrl = heroUrl,
            thumbnailUrl = thumbnailUrl,
            imageUrl = imageUrl,
            arePlayersCustomSorted = arePlayersCustomSorted,
            viewModel = viewModel,
            activity = activity,
        )
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(tabs.size) {
        if (tabs.isNotEmpty() && pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(tabs.lastIndex)
        }
    }

    LaunchedEffect(errorEvent) {
        errorEvent?.getContentIfNotHandled()?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameHeroImage(
                heroUrl = heroUrl,
                thumbnailUrl = thumbnailUrl,
                onPaletteLoaded = { palette -> viewModel.updateGameColors(palette) }
            )

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        text = { Text(stringResource(tab.titleResId)) },
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                tabs.getOrNull(page)?.content?.invoke()
            }
        }

        tabs.getOrNull(pagerState.currentPage)?.fabIconRes?.let { fabRes ->
            FloatingActionButton(
                onClick = { tabs.getOrNull(pagerState.currentPage)?.onFabClick?.invoke() },
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(painter = painterResource(fabRes), contentDescription = null)
            }
        }

    }
}

@Composable
private fun GameHeroImage(
    heroUrl: String,
    thumbnailUrl: String,
    onPaletteLoaded: (Palette?) -> Unit,
) {
    val imageModel = if (heroUrl.isNotBlank()) heroUrl else thumbnailUrl
    AsyncImage(
        model = imageModel,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentScale = ContentScale.Crop,
        onSuccess = { result ->
            val image = result.result.image
            val bitmap = (image as? coil3.BitmapImage)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette -> onPaletteLoaded(palette) }
            } else {
                onPaletteLoaded(null)
            }
        }
    )
}

@Composable
fun GameOverflowMenuAction(
    gameId: Int,
    gameName: String,
    heroUrl: String,
    thumbnailUrl: String,
    imageUrl: String,
    arePlayersCustomSorted: Boolean,
    isFavorite: Boolean,
    isUserMenuEnabled: Boolean,
    viewModel: GameViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val imageToView = heroUrl.ifBlank { imageUrl.ifBlank { thumbnailUrl } }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null)
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_share)) },
            onClick = {
                expanded = false
                activity.shareGame(gameId, gameName, "Game", FirebaseAnalytics.getInstance(context))
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite)) },
            onClick = {
                expanded = false
                viewModel.updateFavorite(!isFavorite)
            }
        )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_create_shortcut)) },
                onClick = {
                    expanded = false
                    viewModel.createShortcut()
                }
            )
            DropdownMenuItem(
                text = { Text("${stringResource(R.string.menu_log_play)} (${stringResource(R.string.menu_log_play_quick_short)})") },
                onClick = {
                    expanded = false
                    viewModel.logQuickPlay(gameId, gameName)
                }
            )
            DropdownMenuItem(
                text = { Text("${stringResource(R.string.menu_log_play)} (${stringResource(R.string.menu_log_play_short)})") },
                onClick = {
                    expanded = false
                    LogPlayActivity.logPlay(
                        context,
                    gameId,
                    gameName,
                    heroUrl.ifBlank { thumbnailUrl.ifBlank { imageUrl } },
                    arePlayersCustomSorted
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_view_image)) },
            onClick = {
                expanded = false
                ImageActivity.start(context, imageToView)
            },
            enabled = imageToView.isNotBlank()
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_users)) },
            onClick = {
                expanded = false
                GameUsersDialogFragment.launch(activity)
            },
            enabled = isUserMenuEnabled
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_view)) },
            onClick = {
                expanded = false
                activity.linkToBgg("boardgame", gameId)
            }
        )
    }
}

private data class GameTab(
    val titleResId: Int,
    @DrawableRes val fabIconRes: Int? = null,
    val onFabClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit
)

private fun buildGameTabs(
    gameId: Int,
    gameName: String,
    isFavorite: Boolean,
    isSignedIn: Boolean,
    shouldShowCollection: Boolean,
    shouldShowPlays: Boolean,
    heroUrl: String,
    thumbnailUrl: String,
    imageUrl: String,
    arePlayersCustomSorted: Boolean,
    viewModel: GameViewModel,
    activity: FragmentActivity,
): List<GameTab> {
    val favoriteIcon = if (isFavorite) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24

    val tabs = mutableListOf<GameTab>()
    tabs += GameTab(
        titleResId = R.string.title_info,
        fabIconRes = R.drawable.ic_baseline_event_available_24,
        onFabClick = { logPlay(activity, viewModel, gameId, gameName, heroUrl, thumbnailUrl, imageUrl, arePlayersCustomSorted) },
        content = { GameInfoTab(viewModel) }
    )
    tabs += GameTab(
        titleResId = R.string.title_credits,
        fabIconRes = favoriteIcon,
        onFabClick = { viewModel.updateFavorite(!isFavorite) },
        content = { GameCreditsTab(viewModel) }
    )
    tabs += GameTab(
        titleResId = R.string.title_descr,
        fabIconRes = favoriteIcon,
        onFabClick = { viewModel.updateFavorite(!isFavorite) },
        content = { GameDescriptionTab(viewModel) }
    )
    if (isSignedIn && shouldShowPlays) {
        tabs += GameTab(
            titleResId = R.string.title_plays,
            fabIconRes = R.drawable.ic_baseline_event_available_24,
            onFabClick = { logPlay(activity, viewModel, gameId, gameName, heroUrl, thumbnailUrl, imageUrl, arePlayersCustomSorted) },
            content = { GamePlaysTab(viewModel) }
        )
    }
    if (isSignedIn && shouldShowCollection) {
        tabs += GameTab(
            titleResId = R.string.title_my_games,
            fabIconRes = R.drawable.ic_baseline_add_24,
            onFabClick = { activity.showAndSurvive(CollectionStatusDialogFragment()) },
            content = { GameCollectionTab(viewModel) }
        )
    }
    tabs += GameTab(
        titleResId = R.string.title_linked_items,
        content = { GameLinkedItemsTab(viewModel) }
    )
    tabs += GameTab(
        titleResId = R.string.title_forums,
        content = { GameForumsTab(gameId = gameId, gameName = gameName) }
    )
    tabs += GameTab(
        titleResId = R.string.links,
        content = { GameLinksTab(viewModel) }
    )
    return tabs
}

private fun logPlay(
    activity: FragmentActivity,
    viewModel: GameViewModel,
    gameId: Int,
    gameName: String,
    heroUrl: String,
    thumbnailUrl: String,
    imageUrl: String,
    arePlayersCustomSorted: Boolean,
) {
    when (activity.preferences().logPlayPreference()) {
        LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
            activity,
            gameId,
            gameName,
            heroUrl.ifBlank { thumbnailUrl.ifBlank { imageUrl } },
            arePlayersCustomSorted
        )
        LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName)
        LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(activity, gameId, gameName)
    }
}
