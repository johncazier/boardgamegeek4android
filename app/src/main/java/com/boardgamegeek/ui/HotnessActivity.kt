package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.shareGame
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HotnessActivity : ComponentActivity() {

    private val viewModel: HotnessViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.title_hotness)) }, // Example title
                            actions = {
                                IconButton(onClick = {
                                    startActivity<SearchResultsActivity>()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.menu_search)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { paddingValues ->
                    HotnessScreen(
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                        onGameClick = { gameId, gameName, thumbnailUrl ->
                            GameActivity.start(this, gameId, gameName, thumbnailUrl ?: "")
                        },
                        onLogPlayForm = { gameId, gameName, thumbnailUrl ->
                            LogPlayActivity.logPlay(this, gameId, gameName, thumbnailUrl ?: "")
                        },
                        onLogPlayWizard = { gameId, gameName ->
                            NewPlayActivity.start(this, gameId, gameName)
                        },
                        onComposeLogPlay = { gameId, gameName, thumbnailUrl ->
                            ComposeLogPlayActivity.start(this, gameId, gameName, thumbnailUrl ?: "")
                        },
                        onShareGame = { gameId, gameName, shareMethod ->
                            this.shareGame(gameId, gameName, shareMethod)
                        },
                        onShareGames = { gamesToShare, shareMethod ->
                            this.shareGames(gamesToShare, shareMethod)
                        },
                        onLinkBgg = { gameId ->
                            this.linkBgg(gameId)
                        }
                    )
                }
            }
        }
    }
}