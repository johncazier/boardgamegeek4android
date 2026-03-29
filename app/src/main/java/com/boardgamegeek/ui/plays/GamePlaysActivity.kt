package com.boardgamegeek.ui.plays

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.GamePlaysRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme

object GamePlaysActivity {
    fun start(
        context: Context,
        gameId: Int,
        gameName: String,
        heroImageUrl: String,
        thumbnailUrl: String,
        arePlayersCustomSorted: Boolean,
        @ColorInt iconColor: Int,
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
        return MainActivity.createIntent(
            context = context,
            route = GamePlaysRoute(
                gameId = gameId,
                gameName = gameName,
                heroImageUrl = heroImageUrl,
                thumbnailUrl = thumbnailUrl,
                arePlayersCustomSorted = arePlayersCustomSorted,
                iconColor = iconColor,
            ),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GamePlaysScaffold(
    subtitle: String,
    playCount: Int,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    androidx.compose.material3.Text(text = stringResource(R.string.title_plays) + if (subtitle.isNotBlank()) " - $subtitle" else "")
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.menu_back),
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.Text(text = playCount.toString())
                },
            )
        },
        content = content,
    )
}

@Composable
fun GamePlaysRouteScreen(
    route: GamePlaysRoute,
    viewModel: PlaysViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(route.gameId) {
        viewModel.setGame(route.gameId)
    }

    AppTheme {
        val plays by viewModel.plays.collectAsState()
        GamePlaysScaffold(
            subtitle = route.gameName,
            playCount = plays.sumOf { it.quantity },
            onBack = { navigator.popBackStackOrFinish(context) },
        ) { paddingValues ->
            PlaysScreen(
                viewModel = viewModel,
                emptyStringResId = R.string.empty_plays_game,
                showGameName = false,
                gameId = route.gameId,
                gameName = route.gameName,
                heroImageUrl = route.heroImageUrl,
                arePlayersCustomSorted = route.arePlayersCustomSorted,
                iconColor = route.iconColor,
                contentPadding = paddingValues,
            )
        }
    }
}
