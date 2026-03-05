package com.boardgamegeek.ui.plays

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamePlaysActivity : AppCompatActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var heroImageUrl = ""
    private var thumbnailUrl = ""
    private var arePlayersCustomSorted = false

    @ColorInt
    private var iconColor = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        viewModel.setGame(gameId)

        setContent {
            AppTheme {
                val plays by viewModel.plays.collectAsState()
                GamePlaysScaffold(
                    subtitle = gameName,
                    playCount = plays.sumOf { it.quantity },
                    onBack = {
                        GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl)
                        finish()
                    },
                ) { paddingValues ->
                    PlaysScreen(
                        viewModel = viewModel,
                        emptyStringResId = R.string.empty_plays_game,
                        showGameName = false,
                        gameId = gameId,
                        gameName = gameName,
                        heroImageUrl = heroImageUrl,
                        arePlayersCustomSorted = arePlayersCustomSorted,
                        iconColor = iconColor,
                        contentPadding = paddingValues,
                    )
                }
            }
        }
    }

    private fun readIntent() {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
        iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun start(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String,
            arePlayersCustomSorted: Boolean,
            @ColorInt iconColor: Int
        ) {
            context.startActivity(createIntent(context, gameId, gameName, heroImageUrl, thumbnailUrl, arePlayersCustomSorted, iconColor))
        }

        fun createIntent(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String = heroImageUrl,
            arePlayersCustomSorted: Boolean = false,
            @ColorInt iconColor: Int = Color.TRANSPARENT,
        ): Intent {
            return context.intentFor<GamePlaysActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_HERO_IMAGE_URL to heroImageUrl,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_CUSTOM_PLAYER_SORT to arePlayersCustomSorted,
                KEY_ICON_COLOR to iconColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamePlaysScaffold(
    subtitle: String,
    playCount: Int,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.title_plays) + if (subtitle.isNotBlank()) " - $subtitle" else "")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                actions = {
                    Text(text = playCount.toString())
                }
            )
        },
        content = content,
    )
}
