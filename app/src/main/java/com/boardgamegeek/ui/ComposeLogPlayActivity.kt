package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.ui.viewmodel.ComposeLogPlayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeLogPlayActivity : ComponentActivity() {

    private val viewModel: ComposeLogPlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ComposeLogPlayScreen(viewModel)
            }
        }
    }

    companion object {
        fun start(context: Context, gameId: Int, gameName: String, imageUrl: String) {
            context.startActivity<ComposeLogPlayActivity>(
                "route" to ComposeLogPlayRoute(gameId, gameName, imageUrl)
            )
        }
    }
}