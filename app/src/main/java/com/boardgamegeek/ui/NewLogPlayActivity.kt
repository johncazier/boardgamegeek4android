package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.viewmodel.NewLogPlayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewLogPlayActivity : ComponentActivity() {

    private val viewModel: NewLogPlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewLogPlayScreen(viewModel)
        }
    }

    companion object {
        const val KEY_GAME_ID = "GAME_ID"
        const val KEY_GAME_NAME = "GAME_NAME"

        fun start(context: Context, gameId: Int, gameName: String) {
            context.startActivity<NewLogPlayActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
            )
        }
    }
}