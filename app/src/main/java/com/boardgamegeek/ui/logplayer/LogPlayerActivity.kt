package com.boardgamegeek.ui.logplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
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
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.createDiscardDialog
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.showLogPlayerNew
import com.boardgamegeek.extensions.showLogPlayerPosition
import com.boardgamegeek.extensions.showLogPlayerRating
import com.boardgamegeek.extensions.showLogPlayerScore
import com.boardgamegeek.extensions.showLogPlayerTeamColor
import com.boardgamegeek.extensions.showLogPlayerWin
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class LogPlayerActivity : ComponentActivity() {
    private val viewModel by viewModels<LogPlayerViewModel>()

    private var gameName = ""
    private var position = 0
    private var player = PlayPlayer()
    private var originalPlayer: PlayPlayer? = null
    private var isNewPlayer = false
    private var autoPosition = PlayPlayer.SEAT_UNKNOWN
    private var heroImageUrl = ""
    private var usedColors = arrayListOf<String>()

    private var userHasShownTeamColor = false
    private var userHasShownPosition = false
    private var userHasShownScore = false
    private var userHasShownRating = false
    private var userHasShownNew = false
    private var userHasShownWin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            cancel(player)
        }

        viewModel.setGameId(intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID))

        setContent {
            val players by viewModel.players.collectAsStateWithLifecycle()
            val users by viewModel.users.collectAsStateWithLifecycle()
            val colors by viewModel.colors.collectAsStateWithLifecycle()

            AppTheme {
                LogPlayerScreen(
                    gameName = gameName,
                    heroImageUrl = heroImageUrl,
                    player = player,
                    position = position,
                    hasAutoPosition = hasAutoPosition(),
                    autoPosition = autoPosition,
                    isNewPlayer = isNewPlayer,
                    colors = colors,
                    usedColors = usedColors,
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
                    onDone = { save(it) },
                    onCancel = { cancel(it) },
                )
            }
        }
    }

    private fun parseIntent(savedInstanceState: Bundle?) {
        position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        autoPosition = intent.getIntExtra(KEY_AUTO_POSITION, PlayPlayer.SEAT_UNKNOWN)
        isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false)
        val used = intent.getStringArrayExtra(KEY_USED_COLORS)

        if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
            userHasShownScore = true
        }

        if (savedInstanceState == null) {
            player = intent.getParcelableCompat(KEY_PLAYER) ?: PlayPlayer()
            if (hasAutoPosition()) player = player.copy(startingPosition = autoPosition.toString())
            originalPlayer = player.copy()
        } else {
            player = savedInstanceState.getParcelableCompat(KEY_PLAYER) ?: PlayPlayer()
            userHasShownTeamColor = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_TEAM_COLOR)
            userHasShownPosition = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_POSITION)
            userHasShownScore = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_SCORE)
            userHasShownRating = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_RATING)
            userHasShownNew = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_NEW)
            userHasShownWin = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_WIN)
        }

        usedColors = if (used == null) arrayListOf() else ArrayList(listOf(*used))
        usedColors.remove(player.color)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PLAYER, player)
        outState.putBoolean(KEY_USER_HAS_SHOWN_TEAM_COLOR, userHasShownTeamColor)
        outState.putBoolean(KEY_USER_HAS_SHOWN_POSITION, userHasShownPosition)
        outState.putBoolean(KEY_USER_HAS_SHOWN_SCORE, userHasShownScore)
        outState.putBoolean(KEY_USER_HAS_SHOWN_RATING, userHasShownRating)
        outState.putBoolean(KEY_USER_HAS_SHOWN_NEW, userHasShownNew)
        outState.putBoolean(KEY_USER_HAS_SHOWN_WIN, userHasShownWin)
    }

    private fun hasAutoPosition(): Boolean = autoPosition != PlayPlayer.SEAT_UNKNOWN

    private fun save(currentPlayer: PlayPlayer) {
        player = currentPlayer
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(KEY_PLAYER, player)
            putExtra(KEY_POSITION, position)
        })
        finish()
    }

    private fun cancel(currentPlayer: PlayPlayer) {
        player = currentPlayer
        if (player == originalPlayer) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            createDiscardDialog(R.string.player, isNew = isNewPlayer).show()
        }
    }

    data class LaunchInput(
        val gameId: Int,
        val gameName: String,
        val heroImageUrl: String,
        val isRequestingToEndPlay: Boolean,
        val fabColor: Int,
        val usedColors: List<String>,
        val autoPosition: Int,
    )

    class AddPlayerContract : ActivityResultContract<LaunchInput, PlayPlayer?>() {
        override fun createIntent(context: Context, input: LaunchInput): Intent {
            return Intent(context, LogPlayerActivity::class.java).apply {
                putExtra(KEY_GAME_ID, input.gameId)
                putExtra(KEY_GAME_NAME, input.gameName)
                putExtra(KEY_HERO_IMAGE_URL, input.heroImageUrl)
                putExtra(KEY_END_PLAY, input.isRequestingToEndPlay)
                putExtra(KEY_FAB_COLOR, input.fabColor)
                putExtra(KEY_USED_COLORS, input.usedColors.toTypedArray())
                putExtra(KEY_NEW_PLAYER, true)
                putExtra(KEY_AUTO_POSITION, input.autoPosition)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): PlayPlayer? {
            return if (resultCode == RESULT_OK) intent?.getParcelableCompat(KEY_PLAYER) else null
        }
    }

    class EditPlayerContract : ActivityResultContract<Pair<LaunchInput, Pair<Int, PlayPlayer>>, Pair<Int, PlayPlayer?>>() {
        override fun createIntent(context: Context, input: Pair<LaunchInput, Pair<Int, PlayPlayer>>): Intent {
            return Intent(context, LogPlayerActivity::class.java).apply {
                putExtra(KEY_GAME_ID, input.first.gameId)
                putExtra(KEY_GAME_NAME, input.first.gameName)
                putExtra(KEY_HERO_IMAGE_URL, input.first.heroImageUrl)
                putExtra(KEY_END_PLAY, input.first.isRequestingToEndPlay)
                putExtra(KEY_FAB_COLOR, input.first.fabColor)
                putExtra(KEY_USED_COLORS, input.first.usedColors.toTypedArray())
                putExtra(KEY_NEW_PLAYER, false)
                putExtra(KEY_AUTO_POSITION, input.first.autoPosition)
                putExtra(KEY_POSITION, input.second.first)
                putExtra(KEY_PLAYER, input.second.second)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, PlayPlayer?> {
            return if (resultCode == RESULT_OK) {
                val position = intent?.getIntExtra(KEY_POSITION, INVALID_POSITION) ?: INVALID_POSITION
                val player = intent?.getParcelableCompat<PlayPlayer>(KEY_PLAYER)
                position to player
            } else INVALID_POSITION to null
        }
    }

    companion object {
        const val KEY_GAME_ID = "GAME_ID"
        const val KEY_GAME_NAME = "GAME_NAME"
        const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        const val KEY_AUTO_POSITION = "AUTO_POSITION"
        const val KEY_USED_COLORS = "USED_COLORS"
        const val KEY_END_PLAY = "SCORE_SHOWN"
        const val KEY_PLAYER = "PLAYER"
        const val KEY_FAB_COLOR = "FAB_COLOR"
        const val KEY_POSITION = "POSITION"
        const val KEY_NEW_PLAYER = "NEW_PLAYER"
        const val INVALID_POSITION = -1
        const val KEY_USER_HAS_SHOWN_TEAM_COLOR = "USER_HAS_SHOWN_TEAM_COLOR"
        const val KEY_USER_HAS_SHOWN_POSITION = "USER_HAS_SHOWN_POSITION"
        const val KEY_USER_HAS_SHOWN_SCORE = "USER_HAS_SHOWN_SCORE"
        const val KEY_USER_HAS_SHOWN_RATING = "USER_HAS_SHOWN_RATING"
        const val KEY_USER_HAS_SHOWN_NEW = "USER_HAS_SHOWN_NEW"
        const val KEY_USER_HAS_SHOWN_WIN = "USER_HAS_SHOWN_WIN"
    }
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
                            style = MaterialTheme.typography.bodySmall
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
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = heroImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    label = { Text(stringResource(R.string.player_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                DropdownMenu(expanded = showPlayerSuggestions, onDismissRequest = { showPlayerSuggestions = false }) {
                    playerSuggestions.forEach {
                        DropdownMenuItem(
                            text = { Text("${it.name} (${it.username})") },
                            onClick = {
                                form = form.copy(name = it.name, username = it.username)
                                showPlayerSuggestions = false
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_players),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showPlayerSuggestions = true }
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
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_buddies),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showUserSuggestions = true }
                )

                if (teamVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = form.color,
                            onValueChange = { form = form.copy(color = it) },
                            label = { Text(stringResource(R.string.team_color)) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showColorDialog = true }
                                .background(Color(form.color.asColorRgb()), CircleShape)
                        ) {
                        }
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
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddFieldMenu = false }) { Text(stringResource(R.string.cancel)) } }
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(color.asColorRgb()), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(color)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showColorDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
