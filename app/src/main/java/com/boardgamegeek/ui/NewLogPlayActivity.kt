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
        fun start(context: Context, gameId: Int, gameName: String, imageUrl: String) {
            context.startActivity<NewLogPlayActivity>(
                "route" to NewLogPlayRoute(gameId, gameName, imageUrl)
            )
        }
    }
}