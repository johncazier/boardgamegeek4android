package com.boardgamegeek.ui.hotness

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.HotGame
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotnessScreen(
    viewModel: HotnessViewModel,
    paddingValues: PaddingValues,
    onGameClick: (gameId: Int, gameName: String, thumbnailUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val hotGames by viewModel.hotGamesFlow.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessageFlow.collectAsStateWithLifecycle()
    val loggedPlayResult by viewModel.loggedPlayResultFlow.collectAsStateWithLifecycle()

    var selectedGames by remember { mutableStateOf<Set<HotGame>>(emptySet()) }
    val isActionMode by remember { derivedStateOf { selectedGames.isNotEmpty() } }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        val message = errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.errorMessageFlow.value = null
        }
    }

    LaunchedEffect(loggedPlayResult) {
        val result = loggedPlayResult
        if (result != null) {
            context.notifyLoggedPlay(result)
            viewModel.loggedPlayResultFlow.value = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            hotGames == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val games = hotGames!!
                if (games.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_hotness),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(games, key = { it.id }) { game ->
                            HotGameItem(
                                game = game,
                                isSelected = selectedGames.contains(game),
                                onToggleSelection = {
                                    selectedGames = if (selectedGames.contains(game)) {
                                        selectedGames - game
                                    } else {
                                        selectedGames + game
                                    }
                                },
                                onItemClick = {
                                    if (isActionMode) {
                                        selectedGames = if (selectedGames.contains(game)) {
                                            selectedGames - game
                                        } else {
                                            selectedGames + game
                                        }
                                    } else {
                                        onGameClick(game.id, game.name, game.thumbnailUrl)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HotGameItem(
    game: HotGame,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onToggleSelection
            )
            .height(IntrinsicSize.Min), // Important for consistent row height with rank
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxHeight(), // Ensure Row fills Card height
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = game.rank.toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .width(40.dp)
                    .padding(end = 8.dp),
                textAlign = TextAlign.Center
            )

            AsyncImage(
                model = game.thumbnailUrl,
                contentDescription = game.name,
                modifier = Modifier
                    .size(60.dp)
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with your placeholder
                error = painterResource(id = R.drawable.ic_launcher_foreground) // Replace with your error drawable
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = game.yearPublished.asYear(context), // Assuming asYear is a simple Int -> String function
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val rating = game.rating

            if (rating != null && rating > 0f) {
                val ratingColorInt = rating.toColor(BggColors.ratingColors)
                Box(modifier = Modifier
                    .padding(start = 8.dp)
                    .background(color = Color(ratingColorInt))
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", rating),
                        color = Color(ratingColorInt.getTextColor()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}