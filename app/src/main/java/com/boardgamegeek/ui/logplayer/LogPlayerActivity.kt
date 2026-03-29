package com.boardgamegeek.ui.logplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.showLogPlayerNew
import com.boardgamegeek.extensions.showLogPlayerPosition
import com.boardgamegeek.extensions.showLogPlayerRating
import com.boardgamegeek.extensions.showLogPlayerScore
import com.boardgamegeek.extensions.showLogPlayerTeamColor
import com.boardgamegeek.extensions.showLogPlayerWin
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LocalRouteResultCoordinator
import com.boardgamegeek.ui.navigation.LogPlayerPayload
import com.boardgamegeek.ui.navigation.LogPlayerRoute
import com.boardgamegeek.ui.navigation.LogPlayerRouteResult
import com.boardgamegeek.ui.navigation.findActivity
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme

object LogPlayerActivity {
    const val INVALID_POSITION = -1

    data class LaunchInput(
        val gameId: Int,
        val gameName: String,
        val heroImageUrl: String,
        val isRequestingToEndPlay: Boolean,
        val usedColors: List<String>,
        val autoPosition: Int,
    )

    fun addPlayerRoute(
        requestId: String,
        input: LaunchInput,
    ): LogPlayerRoute {
        return LogPlayerRoute(
            requestId = requestId,
            gameId = input.gameId,
            gameName = input.gameName,
            heroImageUrl = input.heroImageUrl,
            isRequestingToEndPlay = input.isRequestingToEndPlay,
            usedColors = input.usedColors,
            autoPosition = input.autoPosition,
            isNewPlayer = true,
        )
    }

    fun editPlayerRoute(
        requestId: String,
        input: LaunchInput,
        position: Int,
        player: PlayPlayer,
    ): LogPlayerRoute {
        return LogPlayerRoute(
            requestId = requestId,
            gameId = input.gameId,
            gameName = input.gameName,
            heroImageUrl = input.heroImageUrl,
            isRequestingToEndPlay = input.isRequestingToEndPlay,
            usedColors = input.usedColors,
            autoPosition = input.autoPosition,
            playerPosition = position,
            isNewPlayer = false,
            player = player.toPayload(),
        )
    }
}

@Composable
fun LogPlayerRouteScreen(
    route: LogPlayerRoute,
    viewModel: LogPlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val navigator = LocalAppNavigator.current
    val routeResults = LocalRouteResultCoordinator.current
    val players by viewModel.players.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val initialPlayer = remember(route) {
        route.player.toPlayPlayer().let { initial ->
            if (route.autoPosition != LogPlayerActivity.INVALID_POSITION && initial.startingPosition.isBlank()) {
                initial.copy(startingPosition = route.autoPosition.toString())
            } else {
                initial
            }
        }
    }
    val originalPlayer = remember(route.requestId) { initialPlayer.copy() }

    var player by rememberSaveable(route.requestId) { mutableStateOf(initialPlayer) }
    var userHasShownTeamColor by rememberSaveable(route.requestId) { mutableStateOf(false) }
    var userHasShownPosition by rememberSaveable(route.requestId) { mutableStateOf(route.isRequestingToEndPlay) }
    var userHasShownScore by rememberSaveable(route.requestId) { mutableStateOf(route.isRequestingToEndPlay) }
    var userHasShownRating by rememberSaveable(route.requestId) { mutableStateOf(false) }
    var userHasShownNew by rememberSaveable(route.requestId) { mutableStateOf(false) }
    var userHasShownWin by rememberSaveable(route.requestId) { mutableStateOf(false) }

    LaunchedEffect(route.gameId) {
        viewModel.setGameId(route.gameId)
    }

    fun closeWithResult(playerResult: PlayPlayer?) {
        routeResults.deliver(
            route.requestId,
            LogPlayerRouteResult(
                position = route.playerPosition,
                player = playerResult,
            ),
        )
        navigator.popBackStackOrFinish(context)
    }

    fun cancel(currentPlayer: PlayPlayer) {
        player = currentPlayer
        if (currentPlayer == originalPlayer) {
            closeWithResult(null)
        } else {
            val currentActivity = activity
            if (currentActivity == null) {
                closeWithResult(null)
            } else {
                currentActivity.createDiscardDialog(
                    R.string.player,
                    isNew = route.isNewPlayer,
                    finishActivity = false,
                ) {
                    closeWithResult(null)
                }.show()
            }
        }
    }

    BackHandler {
        cancel(player)
    }

    AppTheme {
        LogPlayerScreen(
            gameName = route.gameName,
            heroImageUrl = route.heroImageUrl,
            player = player,
            position = route.playerPosition,
            hasAutoPosition = route.autoPosition != LogPlayerActivity.INVALID_POSITION,
            autoPosition = route.autoPosition,
            isNewPlayer = route.isNewPlayer,
            colors = colors,
            usedColors = route.usedColors,
            players = players,
            users = users,
            userHasShownTeamColor = userHasShownTeamColor,
            userHasShownPosition = userHasShownPosition,
            userHasShownScore = userHasShownScore,
            userHasShownRating = userHasShownRating,
            userHasShownNew = userHasShownNew,
            userHasShownWin = userHasShownWin,
            onPlayerChange = { player = it },
            onShownFlagsChange = { team, pos, score, rating, newFlag, win ->
                userHasShownTeamColor = team
                userHasShownPosition = pos
                userHasShownScore = score
                userHasShownRating = rating
                userHasShownNew = newFlag
                userHasShownWin = win
            },
            onDone = {
                player = it
                closeWithResult(it)
            },
            onCancel = { cancel(it) },
        )
    }
}

private fun PlayPlayer.toPayload(): LogPlayerPayload {
    return LogPlayerPayload(
        name = name,
        username = username,
        startingPosition = startingPosition,
        color = color,
        score = score,
        rating = rating,
        userId = userId,
        isNew = isNew,
        isWin = isWin,
        playInternalId = playInternalId,
        uiId = uiId,
        internalId = internalId,
    )
}

private fun LogPlayerPayload.toPlayPlayer(): PlayPlayer {
    return PlayPlayer(
        name = name,
        username = username,
        startingPosition = startingPosition,
        color = color,
        score = score,
        rating = rating,
        userId = userId,
        isNew = isNew,
        isWin = isWin,
        playInternalId = playInternalId,
        uiId = uiId,
        internalId = internalId,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogPlayerScreen(
    gameName: String,
    heroImageUrl: String,
    player: PlayPlayer,
    position: Int,
    hasAutoPosition: Boolean,
    autoPosition: Int,
    isNewPlayer: Boolean,
    colors: List<String>,
    usedColors: List<String>,
    players: List<com.boardgamegeek.model.Player>,
    users: List<com.boardgamegeek.model.User>,
    userHasShownTeamColor: Boolean,
    userHasShownPosition: Boolean,
    userHasShownScore: Boolean,
    userHasShownRating: Boolean,
    userHasShownNew: Boolean,
    userHasShownWin: Boolean,
    onPlayerChange: (PlayPlayer) -> Unit,
    onShownFlagsChange: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDone: (PlayPlayer) -> Unit,
    onCancel: (PlayPlayer) -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.preferences()
    var teamVisible by rememberSaveable { mutableStateOf(prefs.showLogPlayerTeamColor() || userHasShownTeamColor || player.color.isNotBlank()) }
    var positionVisible by rememberSaveable { mutableStateOf(!hasAutoPosition && (prefs.showLogPlayerPosition() || userHasShownPosition || player.startingPosition.isNotBlank())) }
    var scoreVisible by rememberSaveable { mutableStateOf(prefs.showLogPlayerScore() || userHasShownScore || player.score.isNotBlank()) }
    var ratingVisible by rememberSaveable { mutableStateOf(prefs.showLogPlayerRating() || userHasShownRating || player.rating > 0) }
    var newVisible by rememberSaveable { mutableStateOf(prefs.showLogPlayerNew() || userHasShownNew || player.isNew) }
    var winVisible by rememberSaveable { mutableStateOf(prefs.showLogPlayerWin() || userHasShownWin || player.isWin) }
    var showAddFieldMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showPlayerSuggestions by remember { mutableStateOf(false) }
    var showUserSuggestions by remember { mutableStateOf(false) }

    var form by rememberSaveable { mutableStateOf(player) }
    val availableColors = remember(colors, usedColors) { colors.filterNot { usedColors.contains(it) } }
    val playerSuggestions = remember(players, form.name) {
        players.filter { it.name.contains(form.name, ignoreCase = true) }.take(8)
    }
    val userSuggestions = remember(users, form.username) {
        users.filter { it.username.contains(form.username, ignoreCase = true) }.take(8)
    }

    onShownFlagsChange(teamVisible, positionVisible, scoreVisible, ratingVisible, newVisible, winVisible)
    onPlayerChange(form)

    val addableFields = buildList {
        if (!teamVisible) add(R.string.team_color)
        if (!hasAutoPosition && !positionVisible) add(R.string.starting_position)
        if (!scoreVisible) add(R.string.score)
        if (!ratingVisible) add(R.string.rating)
        if (!newVisible) add(R.string.new_label)
        if (!winVisible) add(R.string.win)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = gameName)
                        Text(
                            text = if (hasAutoPosition) stringResource(R.string.generic_player, autoPosition) else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onCancel(form) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { onDone(form) }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                },
            )
        },
        floatingActionButton = {
            if (addableFields.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddFieldMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_field))
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            AsyncImage(
                model = heroImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    label = { Text(stringResource(R.string.player_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                DropdownMenu(expanded = showPlayerSuggestions, onDismissRequest = { showPlayerSuggestions = false }) {
                    playerSuggestions.forEach {
                        DropdownMenuItem(
                            text = { Text("${it.name} (${it.username})") },
                            onClick = {
                                form = form.copy(name = it.name, username = it.username)
                                showPlayerSuggestions = false
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_players),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showPlayerSuggestions = true },
                )

                OutlinedTextField(
                    value = form.username,
                    onValueChange = { form = form.copy(username = it) },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = showUserSuggestions, onDismissRequest = { showUserSuggestions = false }) {
                    userSuggestions.forEach {
                        DropdownMenuItem(
                            text = { Text(it.username) },
                            onClick = {
                                form = form.copy(username = it.username, name = it.playNickname.ifBlank { it.fullName })
                                showUserSuggestions = false
                            },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_buddies),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showUserSuggestions = true },
                )

                if (teamVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = form.color,
                            onValueChange = { form = form.copy(color = it) },
                            label = { Text(stringResource(R.string.team_color)) },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showColorDialog = true }
                                .background(Color(form.color.asColorRgb()), CircleShape),
                        )
                    }
                }

                if (positionVisible) {
                    OutlinedTextField(
                        value = form.startingPosition,
                        onValueChange = { form = form.copy(startingPosition = it) },
                        label = { Text(stringResource(R.string.starting_position)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (scoreVisible) {
                    OutlinedTextField(
                        value = form.score,
                        onValueChange = { form = form.copy(score = it) },
                        label = { Text(stringResource(R.string.score)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (ratingVisible) {
                    OutlinedTextField(
                        value = if (form.rating == PlayPlayer.DEFAULT_RATING) "" else form.rating.toString(),
                        onValueChange = { form = form.copy(rating = it.toDoubleOrNull() ?: 0.0) },
                        label = { Text(stringResource(R.string.rating)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (newVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.new_label), modifier = Modifier.weight(1f))
                        Switch(checked = form.isNew, onCheckedChange = { form = form.copy(isNew = it) })
                    }
                }
                if (winVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.win), modifier = Modifier.weight(1f))
                        Switch(checked = form.isWin, onCheckedChange = { form = form.copy(isWin = it) })
                    }
                }
                if (position != LogPlayerActivity.INVALID_POSITION && !isNewPlayer) {
                    Text(
                        text = stringResource(R.string.generic_player, position + 1),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showAddFieldMenu) {
        AlertDialog(
            onDismissRequest = { showAddFieldMenu = false },
            title = { Text(stringResource(R.string.add_field)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    addableFields.forEach { resId ->
                        Text(
                            text = stringResource(resId),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (resId) {
                                        R.string.team_color -> teamVisible = true
                                        R.string.starting_position -> positionVisible = true
                                        R.string.score -> scoreVisible = true
                                        R.string.rating -> ratingVisible = true
                                        R.string.new_label -> {
                                            newVisible = true
                                            form = form.copy(isNew = true)
                                        }

                                        R.string.win -> {
                                            winVisible = true
                                            form = form.copy(isWin = true)
                                        }
                                    }
                                    showAddFieldMenu = false
                                }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddFieldMenu = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text(stringResource(R.string.team_color)) },
            text = {
                Column {
                    availableColors.forEach { color ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    form = form.copy(color = color)
                                    showColorDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(color.asColorRgb()), CircleShape),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(color)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
