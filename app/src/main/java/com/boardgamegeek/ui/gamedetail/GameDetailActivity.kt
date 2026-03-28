package com.boardgamegeek.ui.gamedetail

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.game.GameViewModel
import com.boardgamegeek.ui.game.GameViewModel.ProducerType
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class GameDetailActivity : ComponentActivity() {
    private var title: String = ""
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var type: ProducerType = ProducerType.UNKNOWN

    private val viewModel by viewModels<GameViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameDetail$title")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        viewModel.setId(gameId)
        viewModel.setProducerType(type)
        when (type) {
            ProducerType.DESIGNER -> viewModel.refreshDesignerImages()
            ProducerType.ARTIST -> viewModel.refreshArtistImages()
            ProducerType.PUBLISHER -> viewModel.refreshPublisherImages()
            else -> {}
        }

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { GameDetailTitle(gameName, title) },
                            navigationIcon = {
                                IconButton(onClick = ::navigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    GameDetailScreen(viewModel = viewModel, paddingValues = paddingValues)
                }
            }
        }
    }

    private fun readIntent() {
        title = intent.getStringExtra(KEY_TITLE).orEmpty()
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        type = intent.getSerializableCompat(KEY_TYPE) ?: ProducerType.UNKNOWN
    }

    private fun navigateUp() {
        when (gameId) {
            BggContract.INVALID_ID -> finish()
            else -> GameActivity.startUp(this, gameId, gameName)
        }
        finish()
    }

    companion object {
        private const val KEY_TITLE = "TITLE"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_TYPE = "TYPE"

        fun start(context: Context, title: String, gameId: Int, gameName: String, type: ProducerType) {
            context.startActivity<GameDetailActivity>(
                KEY_TITLE to title,
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_TYPE to type,
            )
        }
    }
}

@Composable
private fun GameDetailTitle(title: String, subtitle: String) {
    Column {
        Text(text = title)
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
