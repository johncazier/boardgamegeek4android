package com.boardgamegeek.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.ui.components.DateField
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

        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            Box {
                AsyncImage(
                    model = viewModel.gameImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Black),
                    contentScale = ContentScale.FillWidth,
                    alpha = .5f
                )

                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = viewModel.gameName,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            Column {

                val selectedDate by viewModel.selectedDateFlow.collectAsStateWithLifecycle()

                DateField(selectedDate) { viewModel.updateSelectedDate(it) }
            }
        }
    }
}