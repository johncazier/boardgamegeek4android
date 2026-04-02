package com.boardgamegeek.ui.plays

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.ui.navigation.BuddyPlaysRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SimplePlaysScaffold(
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
fun BuddyPlaysRouteScreen(
    route: BuddyPlaysRoute,
    viewModel: PlaysViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current

    LaunchedEffect(route.buddyName) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyPlays")
            param(FirebaseAnalytics.Param.ITEM_ID, route.buddyName)
        }
        viewModel.setUsername(route.buddyName)
    }

    AppTheme {
        val plays by viewModel.plays.collectAsState()
        SimplePlaysScaffold(
            subtitle = route.buddyName,
            playCount = plays.sumOf { it.quantity },
            onBack = { navigator.popBackStackOrFinish(context) },
        ) { paddingValues ->
            PlaysScreen(
                viewModel = viewModel,
                emptyStringResId = R.string.empty_plays_buddy,
                showGameName = true,
                gameId = com.boardgamegeek.provider.BggContract.INVALID_ID,
                gameName = "",
                heroImageUrl = "",
                arePlayersCustomSorted = false,
                iconColor = android.graphics.Color.TRANSPARENT,
                contentPadding = paddingValues,
            )
        }
    }
}
