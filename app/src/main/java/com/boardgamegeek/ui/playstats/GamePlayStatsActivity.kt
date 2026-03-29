package com.boardgamegeek.ui.playstats

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.GamePlayStatsRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

object GamePlayStatsActivity {
    fun start(context: Context, gameId: Int, gameName: String, @ColorInt headerColor: Int) {
        context.startActivity(
            MainActivity.createIntent(
                context = context,
                route = GamePlayStatsRoute(
                    gameId = gameId,
                    gameName = gameName,
                    headerColor = headerColor,
                ),
            ),
        )
    }
}

@Composable
fun GamePlayStatsRouteScreen(
    route: GamePlayStatsRoute,
    viewModel: GamePlayStatsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(route.gameId, route.gameName) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GamePlayStats")
            param(FirebaseAnalytics.Param.ITEM_ID, route.gameId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.gameName)
        }
    }

    AppTheme {
        GamePlayStatsScaffold(
            gameName = route.gameName,
            onNavigateUp = { navigator.popBackStackOrFinish(context) },
        ) { padding ->
            GamePlayStatsScreen(
                viewModel = viewModel,
                gameId = route.gameId,
                headerColor = route.headerColor.takeUnless { it == 0 } ?: Color.TRANSPARENT,
                contentPadding = padding,
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
                    Column {
                        Text(
                            text = stringResource(R.string.title_play_stats),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (gameName.isNotBlank()) {
                            Text(
                                text = gameName,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                    actionIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        content = content,
    )
}
