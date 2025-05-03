package com.boardgamegeek.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardgamegeek.ui.viewmodel.NewLogPlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLogPlayScreen(viewModel: NewLogPlayViewModel) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Log Play") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancel() }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(imageVector = Icons.Filled.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->

        Column(modifier = Modifier
            .padding(padding)
        ) {
            AsyncImage(
                model = viewModel.gameImageUrl,
                contentDescription = null
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Text("Game image url: ${viewModel.gameImageUrl}")
            }
        }
    }
}