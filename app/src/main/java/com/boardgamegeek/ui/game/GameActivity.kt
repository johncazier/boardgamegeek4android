package com.boardgamegeek.ui.game

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.game.GameViewModel
import com.boardgamegeek.ui.navigation.GameRoute
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GameActivity : AppCompatActivity(), CollectionStatusDialogFragment.Listener {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var heroImageUrl = ""
    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var arePlayersCustomSorted = false
    private var isFavorite: Boolean = false
    private var isUserMenuEnabled = false
    private val viewModel by viewModels<GameViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        if (gameId == BggContract.INVALID_ID) {
            Timber.w("Received an invalid game ID.")
            finish()
            return
        }

        changeName(intent.getStringExtra(KEY_GAME_NAME).orEmpty())
        changeImage(intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty(), intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty())
        viewModel.setId(gameId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.game.collect { game ->
                        game?.let {
                            changeName(it.name)
                            changeImage(it.heroImageUrl, it.thumbnailUrl)
                            isFavorite = it.isFavorite
                            isUserMenuEnabled = it.maxUsers > 0
                            thumbnailUrl = it.thumbnailUrl
                            imageUrl = it.imageUrl
                            arePlayersCustomSorted = it.customPlayerSort
                        }
                    }
                }
                launch {
                    viewModel.loggedPlayResult.collect { event ->
                        event?.getContentIfNotHandled()?.let { notifyLoggedPlay(it) }
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        setContent {
            GameRouteScreen(
                route = GameRoute(
                    gameId = gameId,
                    gameName = gameName,
                    thumbnailUrl = thumbnailUrl,
                    heroImageUrl = heroImageUrl,
                ),
                viewModel = viewModel,
            )
        }
    }

    private fun changeName(gameName: String) {
        if (gameName != this.gameName) {
            this.gameName = gameName
            intent.putExtra(KEY_GAME_NAME, gameName)
        }
    }

    private fun changeImage(heroImageUrl: String, thumbnailUrl: String) {
        if (this.heroImageUrl != heroImageUrl ||
            this.thumbnailUrl != thumbnailUrl
        ) {
            this.heroImageUrl = heroImageUrl
            this.thumbnailUrl = thumbnailUrl
        }
    }

    override fun onSelectStatuses(selectedStatuses: List<String>, wishlistPriority: Int) {
        viewModel.addCollectionItem(selectedStatuses, wishlistPriority)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_FROM_SHORTCUT = "FROM_SHORTCUT"

        fun start(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = "") {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent)
        }

        fun startUp(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = thumbnailUrl) {
            val intent = createIntent(context, gameId, gameName, thumbnailUrl, heroImageUrl) ?: return
            context.startActivity(intent.clearTask().clearTop())
        }

        fun createIntent(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", heroImageUrl: String = ""): Intent? {
            if (gameId == BggContract.INVALID_ID) return null
            return MainActivity.createIntent(
                context,
                GameRoute(
                    gameId = gameId,
                    gameName = gameName,
                    thumbnailUrl = thumbnailUrl,
                    heroImageUrl = heroImageUrl,
                ),
            )
        }

        fun createShortcutInfo(context: Context, gameId: Int, gameName: String, bitmap: Bitmap? = null): ShortcutInfoCompat? {
            if (gameId != BggContract.INVALID_ID &&
                gameName.isNotBlank() &&
                ShortcutManagerCompat.isRequestPinShortcutSupported(context)
            ) {
                val intent = createIntent(context, gameId, gameName)
                intent?.let {
                    intent.action = Intent.ACTION_VIEW
                    intent.putExtra(KEY_FROM_SHORTCUT, true).clearTop().newTask()
                    val builder = ShortcutInfoCompat.Builder(context, "game-$gameId")
                        .setShortLabel(gameName.toShortLabel())
                        .setLongLabel(gameName.toLongLabel())
                        .setIntent(intent)
                    if (bitmap != null) {
                        builder.setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
                    } else {
                        builder.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_foreground))
                    }
                    return builder.build()
                }
            }
            return null
        }
    }
}

@Composable
fun GameRouteScreen(
    route: GameRoute,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val gameState by viewModel.game.collectAsStateWithLifecycle()

    LaunchedEffect(route.gameId) {
        viewModel.setId(route.gameId)
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Game")
            param(FirebaseAnalytics.Param.ITEM_ID, route.gameId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.gameName)
        }
    }

    val title = gameState?.name ?: route.gameName
    val heroUrl = gameState?.heroImageUrl ?: route.heroImageUrl
    val thumbUrl = gameState?.thumbnailUrl ?: route.thumbnailUrl
    val image = gameState?.imageUrl.orEmpty()
    val arePlayersSorted = gameState?.customPlayerSort ?: false
    val favorite = gameState?.isFavorite ?: false
    val userMenuEnabled = (gameState?.maxUsers ?: 0) > 0

    AppScreen(
        topBarTitle = title,
        currentScreenRouteFromActivity = "",
        snackbarHostState = snackbarHostState,
        topBarActions = {
            IconButton(onClick = { context.linkToBgg("boardgame", route.gameId) }) {
                Icon(
                    imageVector = Icons.Filled.OpenInBrowser,
                    contentDescription = stringResource(R.string.menu_view),
                )
            }
            GameOverflowMenuAction(
                gameId = route.gameId,
                gameName = title,
                heroUrl = heroUrl,
                thumbnailUrl = thumbUrl,
                imageUrl = image,
                arePlayersCustomSorted = arePlayersSorted,
                isFavorite = favorite,
                isUserMenuEnabled = userMenuEnabled,
                viewModel = viewModel,
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            GameScreen(
                viewModel = viewModel,
                gameId = route.gameId,
                initialGameName = route.gameName,
                initialHeroUrl = route.heroImageUrl,
                initialThumbnailUrl = route.thumbnailUrl,
                initialImageUrl = image,
                initialArePlayersCustomSorted = arePlayersSorted,
                initialIsFavorite = favorite,
                initialIsUserMenuEnabled = userMenuEnabled,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}
