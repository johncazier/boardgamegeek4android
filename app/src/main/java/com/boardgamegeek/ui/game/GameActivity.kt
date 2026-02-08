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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.SearchResultsActivity
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment
import com.boardgamegeek.ui.game.GameViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
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

        viewModel.game.observe(this) {
            it?.let { game ->
                changeName(game.name)
                changeImage(game.heroImageUrl, game.thumbnailUrl)
                isFavorite = game.isFavorite
                isUserMenuEnabled = game.maxUsers > 0
                thumbnailUrl = game.thumbnailUrl
                imageUrl = game.imageUrl
                arePlayersCustomSorted = game.customPlayerSort
            }
        }

        viewModel.loggedPlayResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                notifyLoggedPlay(it)
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
            val snackbarHostState = remember { SnackbarHostState() }
            val gameState by viewModel.game.observeAsState()
            val title = gameState?.name ?: gameName
            val heroUrl = gameState?.heroImageUrl ?: heroImageUrl
            val thumbUrl = gameState?.thumbnailUrl ?: thumbnailUrl
            val image = gameState?.imageUrl ?: imageUrl
            val arePlayersSorted = gameState?.customPlayerSort ?: arePlayersCustomSorted
            val favorite = gameState?.isFavorite ?: isFavorite
            val userMenuEnabled = (gameState?.maxUsers ?: if (isUserMenuEnabled) 1 else 0) > 0

            AppScreen(
                topBarTitle = title,
                currentScreenRouteFromActivity = "",
                snackbarHostState = snackbarHostState,
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                topBarActions = {
                    IconButton(onClick = { linkToBgg("boardgame", gameId) }) {
                        Icon(
                            imageVector = Icons.Filled.OpenInBrowser,
                            contentDescription = stringResource(R.string.menu_view)
                        )
                    }
                    GameOverflowMenuAction(
                        gameId = gameId,
                        gameName = title,
                        heroUrl = heroUrl,
                        thumbnailUrl = thumbUrl,
                        imageUrl = image,
                        arePlayersCustomSorted = arePlayersSorted,
                        isFavorite = favorite,
                        isUserMenuEnabled = userMenuEnabled,
                        viewModel = viewModel,
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    GameScreen(
                        viewModel = viewModel,
                        gameId = gameId,
                        initialGameName = gameName,
                        initialHeroUrl = heroImageUrl,
                        initialThumbnailUrl = thumbnailUrl,
                        initialImageUrl = imageUrl,
                        initialArePlayersCustomSorted = arePlayersCustomSorted,
                        initialIsFavorite = isFavorite,
                        initialIsUserMenuEnabled = isUserMenuEnabled,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
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
            return context.intentFor<GameActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_HERO_IMAGE_URL to heroImageUrl,
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
