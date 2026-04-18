package com.boardgamegeek.ui.logplay

import android.graphics.Color
import android.widget.Chronometer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.asBoundedRating
import com.boardgamegeek.extensions.asPersonalRating
import com.boardgamegeek.extensions.asScore
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.startTimerWithSystemTime
import com.boardgamegeek.model.GameExpansion
import com.boardgamegeek.model.PlayPlayer
import java.text.DecimalFormat

enum class AddLogPlayField {
    Location,
    Length,
    Quantity,
    Incomplete,
    NoWinStats,
    Comments,
    Players,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPlayScreen(
    gameName: String,
    heroImageUrl: String,
    dateText: String,
    isLoading: Boolean,
    showLocation: Boolean,
    location: String,
    showLength: Boolean,
    length: Int,
    startTime: Long,
    timerEnabled: Boolean,
    showQuantity: Boolean,
    quantity: Int,
    showIncomplete: Boolean,
    incomplete: Boolean,
    showNoWinStats: Boolean,
    noWinStats: Boolean,
    showComments: Boolean,
    comments: String,
    expansions: List<GameExpansion>,
    selectedExpansionIds: Set<Int>,
    showPlayers: Boolean,
    playersLabel: String,
    canAssignColors: Boolean,
    players: List<PlayPlayer>,
    availableFields: List<AddLogPlayField>,
    fabColor: Int,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onDateClick: () -> Unit,
    onLocationChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onStartTimerClick: () -> Unit,
    onStopTimerClick: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onIncompleteChange: (Boolean) -> Unit,
    onNoWinStatsChange: (Boolean) -> Unit,
    onCommentsChange: (String) -> Unit,
    onExpansionToggle: (Int, Boolean) -> Unit,
    onAssignColorsClick: () -> Unit,
    onSortPlayersClick: () -> Unit,
    onAddField: (AddLogPlayField) -> Unit,
    onEditPlayer: (Int) -> Unit,
    onScorePlayer: (Int) -> Unit,
    onRatingPlayer: (Int) -> Unit,
    onColorPlayer: (Int) -> Unit,
    onRemovePlayer: (PlayPlayer) -> Unit,
    onToggleWin: (Int, Boolean) -> Unit,
    onToggleNew: (Int, Boolean) -> Unit,
) {
    var addMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_log_play), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.done))
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { addMenuExpanded = true },
                    containerColor = if (fabColor != Color.TRANSPARENT) ComposeColor(fabColor) else MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_field))
                }
                DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                    availableFields.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(text = addFieldLabel(field)) },
                            onClick = {
                                addMenuExpanded = false
                                onAddField(field)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.image_header_height))
                ) {
                    AsyncImage(
                        model = heroImageUrl,
                        contentDescription = gameName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ComposeColor.Black.copy(alpha = 0.35f))
                    )
                    Text(
                        text = gameName,
                        color = ComposeColor.White,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                    )
                }

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDateClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )

                if (showLocation) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = onLocationChange,
                        label = { Text(stringResource(R.string.location)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                    )
                }

                if (startTime > 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = stringResource(R.string.timer),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        AndroidView(
                            factory = { Chronometer(it) },
                            update = { it.startTimerWithSystemTime(startTime) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onStopTimerClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_timer_off_24),
                                contentDescription = stringResource(R.string.timer),
                            )
                        }
                    }
                } else if (showLength) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = if (length > 0) length.toString() else "",
                            onValueChange = onLengthChange,
                            label = { Text(stringResource(R.string.length_hint)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        IconButton(onClick = onStartTimerClick, enabled = timerEnabled) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_timer_24),
                                contentDescription = stringResource(R.string.timer),
                            )
                        }
                    }
                }

                if (showQuantity) {
                    OutlinedTextField(
                        value = quantity.toString(),
                        onValueChange = onQuantityChange,
                        label = { Text(stringResource(R.string.quantity)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                    )
                }

                if (showIncomplete) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(R.string.incomplete), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = incomplete, onCheckedChange = onIncompleteChange)
                    }
                }

                if (showNoWinStats) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(R.string.noWinStats), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = noWinStats, onCheckedChange = onNoWinStatsChange)
                    }
                }

                if (showComments) {
                    OutlinedTextField(
                        value = comments,
                        onValueChange = onCommentsChange,
                        label = { Text(stringResource(R.string.comments)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (expansions.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = stringResource(R.string.title_owned_expansions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    )
                    expansions.forEach { expansion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExpansionToggle(expansion.id, !selectedExpansionIds.contains(expansion.id)) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selectedExpansionIds.contains(expansion.id),
                                onCheckedChange = { onExpansionToggle(expansion.id, it) },
                            )
                            Text(
                                text = expansion.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                if (showPlayers) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onAssignColorsClick, enabled = canAssignColors) {
                            Icon(Icons.Filled.ColorLens, contentDescription = stringResource(R.string.colors))
                        }
                        Text(
                            text = playersLabel,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onSortPlayersClick) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((players.size * 80).coerceAtLeast(80).dp),
                    ) {
                        itemsIndexed(players, key = { _, player -> player.uiId }) { index, player ->
                            SwipeToRemovePlayerRow(
                                onRemove = { onRemovePlayer(player) },
                            ) {
                                PlayerRow(
                                    player = player,
                                    onEdit = { onEditPlayer(index) },
                                    onScore = { onScorePlayer(index) },
                                    onRating = { onRatingPlayer(index) },
                                    onColor = { onColorPlayer(index) },
                                    onToggleWin = { onToggleWin(index, it) },
                                    onToggleNew = { onToggleNew(index, it) },
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(88.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemovePlayerRow(
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onRemove()
        }
    }

    val backgroundColor = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
        ComposeColor.Transparent
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    val alignment = when (dismissState.targetValue) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart,
        SwipeToDismissBoxValue.Settled -> Alignment.CenterEnd
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove),
                        tint = contentColor,
                    )
                }
            }
        },
        content = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                content()
            }
        },
    )
}

@Composable
private fun PlayerRow(
    player: PlayPlayer,
    onEdit: () -> Unit,
    onScore: () -> Unit,
    onRating: () -> Unit,
    onColor: () -> Unit,
    onToggleWin: (Boolean) -> Unit,
    onToggleNew: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val name = player.name.trim()
    val username = player.username.trim()
    val title = when {
        name.isNotEmpty() -> name
        username.isNotEmpty() -> username
        player.seat != PlayPlayer.SEAT_UNKNOWN -> "Player ${player.seat}"
        else -> "Player"
    }
    val subtitle = if (name.isNotEmpty() && username.isNotEmpty()) username else ""
    val playerColor = player.color.asColorRgb()
    val circleColor = if (playerColor != Color.TRANSPARENT) ComposeColor(playerColor) else ComposeColor.Transparent
    val circleTextColor = if (playerColor != Color.TRANSPARENT) ComposeColor(playerColor.getTextColor()) else MaterialTheme.colorScheme.onSurface
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val ratingText = if (player.rating == 0.0) "" else player.rating.asBoundedRating(context, format = DecimalFormat("0.0######"))
    val scoreText = player.numericScore?.asScore(context) ?: player.score

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .background(circleColor, CircleShape)
                .clickable(onClick = onColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = player.startingPosition,
                style = MaterialTheme.typography.bodyMedium,
                color = circleTextColor,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .clickable(onClick = onEdit),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (player.isWin) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (player.isNew) FontStyle.Italic else FontStyle.Normal,
                ),
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (player.color.isNotBlank() && playerColor == Color.TRANSPARENT) {
                Text(text = player.color, style = MaterialTheme.typography.bodySmall)
            }
        }

        IconValueButton(
            onClick = onRating,
            contentDescription = stringResource(R.string.rating),
            icon = { hasValue ->
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = if (hasValue) 0.28f else 1f),
                )
            },
            value = ratingText,
        )
        IconValueButton(
            onClick = onScore,
            contentDescription = stringResource(R.string.score),
            icon = { hasValue ->
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = if (hasValue) 0.28f else 1f),
                )
            },
            value = scoreText,
        )
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.win)) },
                    trailingIcon = {
                        if (player.isWin) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onToggleWin(!player.isWin)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_label)) },
                    trailingIcon = {
                        if (player.isNew) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onToggleNew(!player.isNew)
                    },
                )
            }
        }
    }
}

@Composable
private fun IconValueButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable (Boolean) -> Unit,
    value: String,
) {
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            icon(value.isNotBlank())
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), CircleShape)
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun addFieldLabel(field: AddLogPlayField): String {
    return when (field) {
        AddLogPlayField.Location -> stringResource(R.string.location)
        AddLogPlayField.Length -> stringResource(R.string.length)
        AddLogPlayField.Quantity -> stringResource(R.string.quantity)
        AddLogPlayField.Incomplete -> stringResource(R.string.incomplete)
        AddLogPlayField.NoWinStats -> stringResource(R.string.noWinStats)
        AddLogPlayField.Comments -> stringResource(R.string.comments)
        AddLogPlayField.Players -> stringResource(R.string.title_players)
    }
}
