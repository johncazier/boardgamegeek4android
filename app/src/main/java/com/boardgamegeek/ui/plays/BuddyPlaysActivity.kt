package com.boardgamegeek.ui.plays

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.buddy.BuddyActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BuddyPlaysActivity : AppCompatActivity() {
    private val viewModel by viewModels<PlaysViewModel>()
    private var buddyName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (savedInstanceState == null) {
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyPlays")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName)
            }
        }

        viewModel.setUsername(buddyName)

        setContent {
            AppTheme {
                val plays by viewModel.plays.collectAsState()
                SimplePlaysScaffold(
                    subtitle = buddyName,
                    playCount = plays.sumOf { it.quantity },
                    onBack = {
                        BuddyActivity.startUp(this, buddyName)
                        finish()
                    },
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
    }

    private fun readIntent() {
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME).orEmpty()
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"

        fun start(context: Context, buddyName: String?) {
            context.startActivity<BuddyPlaysActivity>(
                KEY_BUDDY_NAME to buddyName,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimplePlaysScaffold(
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
