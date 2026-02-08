package com.boardgamegeek.ui.playstats

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameActivity.Companion.startUp
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@AndroidEntryPoint
class GamePlayStatsActivity : ComponentActivity() {
    private val viewModel: GamePlayStatsViewModel by viewModels()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""

    @ColorInt
    private var headerColor = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readIntent()

        if (savedInstanceState == null) {
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GamePlayStats")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        setContent {
            AppTheme {
                GamePlayStatsScaffold(
                    gameName = gameName,
                    onNavigateUp = {
                        startUp(this, gameId, gameName)
                        finish()
                    },
                ) { padding ->
                    GamePlayStatsScreen(
                        viewModel = viewModel,
                        gameId = gameId,
                        headerColor = headerColor,
                        contentPadding = padding,
                    )
                }
            }
        }
    }

    private fun readIntent() {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        headerColor = intent.getIntExtra(KEY_HEADER_COLOR, ContextCompat.getColor(this, R.color.accent))
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HEADER_COLOR = "HEADER_COLOR"

        fun start(context: Context, gameId: Int, gameName: String, @ColorInt headerColor: Int) {
            context.startActivity<GamePlayStatsActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_HEADER_COLOR to headerColor,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GamePlayStatsScaffold(
    gameName: String,
    onNavigateUp: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(
                            text = stringResource(R.string.title_play_stats),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (gameName.isNotBlank()) {
                            Text(
                                text = gameName,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        content = content
    )
}
