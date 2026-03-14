package com.boardgamegeek.ui.playercolors

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.buddy.BuddyActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class PlayerColorsActivity : ComponentActivity() {
    private var buddyName: String? = null
    private var playerName: String? = null

    private val viewModel by viewModels<PlayerColorsViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME)
        playerName = intent.getStringExtra(KEY_PLAYER_NAME)

        if (buddyName.isNullOrBlank() && playerName.isNullOrBlank()) {
            Timber.w("Can't launch - missing both buddy name and username.")
            finish()
            return
        }

        if (buddyName.isNullOrEmpty()) {
            viewModel.setPlayerName(playerName)
        } else {
            viewModel.setUsername(buddyName)
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName.orEmpty())
                param(FirebaseAnalytics.Param.ITEM_NAME, playerName.orEmpty())
            }
        }

        setContent {
            val colors by viewModel.colors.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val subtitle = if (buddyName.isNullOrBlank()) playerName.orEmpty() else buddyName.orEmpty()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            var showClearDialog by remember { mutableStateOf(false) }
            var showAddDialog by remember { mutableStateOf(false) }

            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(text = stringResource(R.string.title_favorite_colors))
                                    Text(text = subtitle)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    BuddyActivity.startUp(this, buddyName, playerName)
                                    finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showClearDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.menu_clear)
                                    )
                                }
                            },
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.title_add_color)
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    PlayerColorsScreen(
                        isLoading = isLoading,
                        colors = colors,
                        paddingValues = paddingValues,
                        onGenerate = viewModel::generate,
                        onMoveUp = viewModel::moveUp,
                        onMoveDown = viewModel::moveDown,
                        onDelete = { color ->
                            val index = viewModel.remove(color)
                            if (index >= 0) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = getString(R.string.removed_suffix, color),
                                        actionLabel = getString(R.string.undo),
                                    )
                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        viewModel.add(color, index)
                                    }
                                }
                            }
                        },
                    )
                }

                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text(stringResource(R.string.menu_clear)) },
                        text = { Text(stringResource(R.string.are_you_sure_clear_colors)) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.clear()
                                showClearDialog = false
                            }) { Text(stringResource(R.string.clear)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }

                if (showAddDialog) {
                    AddPlayerColorDialog(
                        hiddenColors = colors,
                        onDismiss = { showAddDialog = false },
                        onColorSelected = { color ->
                            viewModel.add(color)
                            showAddDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        viewModel.save()
        super.onStop()
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, buddyName: String?, playerName: String?) {
            context.startActivity<PlayerColorsActivity>(
                KEY_BUDDY_NAME to buddyName,
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}

@Composable
private fun PlayerColorsScreen(
    isLoading: Boolean,
    colors: List<String>,
    paddingValues: PaddingValues,
    onGenerate: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDelete: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            colors.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.empty_player_colors),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onGenerate) {
                        Text(stringResource(R.string.empty_player_colors_button))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(colors, key = { _, color -> color }) { index, color ->
                        PlayerColorRow(
                            color = color,
                            onMoveUp = { onMoveUp(index) },
                            onMoveDown = { onMoveDown(index) },
                            onDelete = { onDelete(color) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerColorRow(
    color: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorInt = color.asColorRgb()
    val textColor = Color(colorInt.getTextColor())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(colorInt), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = color,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
        }
        IconButton(onClick = onMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.menu_delete))
        }
    }
}

@Composable
private fun AddPlayerColorDialog(
    hiddenColors: List<String>,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    val availableColors = remember(hiddenColors) {
        BggColors.colorList.filterNot { hiddenColors.contains(it.first) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_color)) },
        text = {
            LazyColumn {
                itemsIndexed(availableColors, key = { _, item -> item.first }) { _, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelected(item.first) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(item.second), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(item.first)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
