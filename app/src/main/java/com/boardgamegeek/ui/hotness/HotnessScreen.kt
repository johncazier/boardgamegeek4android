package com.boardgamegeek.ui.hotness

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.HotGame
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotnessScreen(
    viewModel: HotnessViewModel,
    paddingValues: PaddingValues,
    onGameClick: (gameId: Int, gameName: String, thumbnailUrl: String?) -> Unit,
    onLogPlayForm: (gameId: Int, gameName: String, thumbnailUrl: String?) -> Unit,
    onLogPlayWizard: (gameId: Int, gameName: String) -> Unit,
    onComposeLogPlay: (gameId: Int, gameName: String, thumbnailUrl: String?) -> Unit,
    onShareGame: (gameId: Int, gameName: String, shareMethod: String) -> Unit,
    onShareGames: (gamesToShare: List<Pair<Int, String>>, shareMethod: String) -> Unit,
    onLinkBgg: (gameId: Int) -> Unit
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

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isActionMode) {
                HotnessActionModeBar(
                    selectedCount = selectedGames.size,
                    onCloseActionMode = { selectedGames = emptySet() },
                    onActionItemClicked = { itemId ->
                        val gamesList = selectedGames.toList()
                        if (gamesList.isEmpty()) return@HotnessActionModeBar

                        when (itemId) {
                            R.id.menu_log_play_form -> gamesList.firstOrNull()?.let {
                                onLogPlayForm(it.id, it.name, it.thumbnailUrl)
                            }
                            R.id.menu_log_play_quick -> {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                gamesList.forEach { viewModel.logQuickPlay(it.id, it.name) }
                            }
                            R.id.menu_log_play_wizard -> gamesList.firstOrNull()?.let {
                                onLogPlayWizard(it.id, it.name)
                            }
                            R.id.menu_compose_log_play -> gamesList.firstOrNull()?.let {
                                onComposeLogPlay(it.id, it.name, it.thumbnailUrl)
                            }
                            R.id.menu_share -> {
                                val shareMethod = "Hotness"
                                if (gamesList.size == 1) {
                                    gamesList.firstOrNull()?.let { onShareGame(it.id, it.name, shareMethod) }
                                } else {
                                    onShareGames(gamesList.map { it.id to it.name }, shareMethod)
                                }
                            }
                            R.id.menu_link -> gamesList.firstOrNull()?.let { onLinkBgg(it.id) }
                        }
                        selectedGames = emptySet()
                    }
                )
            }
        }
    ) { innerPadding ->
        when {
            hotGames == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val games = hotGames!!
                if (games.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isActionMode) innerPadding else PaddingValues(0.dp)),
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotnessActionModeBar(
    selectedCount: Int,
    onCloseActionMode: () -> Unit,
    onActionItemClicked: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSignedIn = remember { Authenticator.isSignedIn(context) } // Check auth status

    TopAppBar(
        modifier = modifier,
        title = { Text(pluralStringResource(R.plurals.msg_games_selected, selectedCount, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onCloseActionMode) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.abc_action_mode_done))
            }
        },
        actions = {
            // Log Play (Form) - Visible if count == 1 and signed in
            if (selectedCount == 1 && isSignedIn) {
                IconButton(onClick = { onActionItemClicked(R.id.menu_log_play_form) }) {
                    Icon(imageVector = Icons.Default.AddChart, contentDescription = stringResource(R.string.menu_log_play_short))
                }
            }
            // Share - Always visible
            IconButton(onClick = { onActionItemClicked(R.id.menu_share) }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.menu_share))
            }
            // Link - Visible if count == 1
            if (selectedCount == 1) {
                IconButton(onClick = { onActionItemClicked(R.id.menu_link) }) {
                    Icon(Icons.Filled.Link, contentDescription = stringResource(R.string.link_bgg))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            titleContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionIconContentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    )
}