package com.boardgamegeek.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.boardgamegeek.ui.viewmodel.NewLogPlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLogPlayScreen(viewModel: NewLogPlayViewModel) {

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Log Play") })
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {
            Text("Game ID: ${viewModel.gameId}, name: ${viewModel.gameName}")
        }
    }
}