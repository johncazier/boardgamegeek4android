package com.boardgamegeek.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.model.NewPlayPlayer
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.components.DateField
import com.boardgamegeek.ui.navigation.HandleViewModelActions
import com.boardgamegeek.ui.viewmodel.ComposeLogPlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeLogPlayScreen(viewModel: ComposeLogPlayViewModel) {

    HandleViewModelActions(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_log_play)) },
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
            GameImage(viewModel)

            GameDate(viewModel)

            GameComments(viewModel)

            GamePlayers(viewModel)

            GameExpansions(viewModel)
        }
    }
}

@Composable
private fun GameComments(viewModel: ComposeLogPlayViewModel) {

    val comments = viewModel.commentsFlow.collectAsStateWithLifecycle()

    OutlinedTextField(
        value = comments.value,
        onValueChange = { viewModel.commentsFlow.value = it },
        label = {
            Text(text = stringResource(R.string.comments))
        },
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun GamePlayers(viewModel: ComposeLogPlayViewModel) {

    val players by viewModel.availablePlayersFlow.collectAsStateWithLifecycle()

    val selectedPlayers by viewModel.selectedPlayersFlow.collectAsStateWithLifecycle()

    val playerMap by viewModel.playerMapFlow.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    TextButton(onClick = { showDialog = true }, modifier = Modifier.padding(start = 8.dp)) {
        Text(text = stringResource(R.string.title_add_players))
    }

    LazyColumn {

        itemsIndexed(selectedPlayers) { index, player ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val playerInfo = playerMap[player.id] ?: NewPlayPlayer(player)

                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = player.name, style = MaterialTheme.typography.bodyLarge)

                    if (!player.username.isBlank()) {
                        Text(text = player.username, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = playerInfo.score,
                    onValueChange = { viewModel.updatePlayerScore(player, it) },
                    label = { Text(text = stringResource(R.string.score)) },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                )

                Checkbox(checked = playerInfo.isWin, onCheckedChange = { viewModel.onPlayerWinChanged(player, it) })

                Text(text = stringResource(R.string.win))

                Spacer(modifier = Modifier.width(16.dp))

                //todo delete icon
            }
        }
    }

    if (showDialog) {
        PlayerSelectionDialog(
            players,
            selectedPlayers,
            onDismiss = { showDialog = false },
            onConfirm = { players ->
                viewModel.onPlayersSelected(players.map { it.id })
                showDialog = false
            }
        )
    }
}

@Composable
fun PlayerSelectionDialog(
    players: List<Player>,
    selectedPlayers: List<Player>,
    onDismiss: () -> Unit,
    onConfirm: (selectedPlayers: List<Player>) -> Unit
) {
    val selected = remember { mutableStateMapOf<Player, Boolean>() }

    selectedPlayers.forEach { selected[it] = true }

    players.forEach { selected.putIfAbsent(it, false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Players", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .fillMaxWidth()
                ) {
                    items(players) { player ->
                        Row(
                            modifier = Modifier
                                .clickable { selected[player] = !selected[player]!! }
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected[player] == true,
                                onCheckedChange = { selected[player] = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(player.name)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onConfirm(selected.filterValues { it }.keys.toList())
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun GameDate(viewModel: ComposeLogPlayViewModel) {
    Column {

        val selectedDate by viewModel.selectedDateFlow.collectAsStateWithLifecycle()

        DateField(selectedDate) { viewModel.updateSelectedDate(it) }
    }
}

@Composable
private fun GameImage(viewModel: ComposeLogPlayViewModel) {

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
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun GameExpansions(viewModel: ComposeLogPlayViewModel) {

    Text(text = stringResource(R.string.expansions), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, start = 16.dp))

    val expansions by viewModel.expansionsFlow.collectAsStateWithLifecycle()

    val selectedExpansionIds by viewModel.selectedExpansionIdsFlow.collectAsStateWithLifecycle()

    LazyColumn {

        items(expansions) { expansion ->

            val selected = selectedExpansionIds.contains(expansion.id)

            Row(
                modifier = Modifier
                    .clickable { viewModel.onExpansionChecked(expansion.id, !selected) }
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { viewModel.onExpansionChecked(expansion.id, it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(expansion.name)
            }
        }
    }
}