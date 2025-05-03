package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.boardgamegeek.extensions.startActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewLogPlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Test - gameId: ${intent.getIntExtra(KEY_GAME_ID, 0)}, name: ${intent.getStringExtra(KEY_GAME_NAME)}")
        }
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"

        fun start(context: Context, gameId: Int, gameName: String) {
            context.startActivity<NewLogPlayActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
            )
        }
    }
}