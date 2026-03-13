package com.boardgamegeek.ui.gamecolors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.darkenColor

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun GameColorsScreen(
    colors: List<String>?,
    selectedColors: SnapshotStateList<String>,
    selectionMode: Boolean,
    onColorDelete: (String) -> Unit,
    paddingValues: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when (val items = colors) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_colors),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(items, key = { it }) { colorName ->
                            val isSelected = selectedColors.contains(colorName)
                            if (selectionMode) {
                                ColorRow(
                                    colorName = colorName,
                                    isSelected = isSelected,
                                    onClick = { toggleSelection(selectedColors, colorName) },
                                    onLongClick = { toggleSelection(selectedColors, colorName) },
                                )
                            } else {
                                val dismissState = rememberDismissState(
                                    confirmStateChange = { value ->
                                        if (value != DismissValue.Default) {
                                            onColorDelete(colorName)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )
                                SwipeToDismiss(
                                    state = dismissState,
                                    directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                                    background = { DismissBackground() },
                                    dismissContent = {
                                        ColorRow(
                                            colorName = colorName,
                                            isSelected = isSelected,
                                            onClick = {},
                                            onLongClick = { toggleSelection(selectedColors, colorName) },
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorRow(
    colorName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colorValue = remember(colorName) { colorName.asColorRgb() }
    val circleSize = dimensionResource(R.dimen.color_circle_diameter)
    val borderColor = Color(colorValue.darkenColor())
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(Color(colorValue), shape = CircleShape)
                .border(1.dp, borderColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = colorName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DismissBackground() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_delete_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_delete_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private fun toggleSelection(selectedColors: SnapshotStateList<String>, colorName: String) {
    if (selectedColors.contains(colorName)) {
        selectedColors.remove(colorName)
    } else {
        selectedColors.add(colorName)
    }
}

suspend fun showUndoSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String,
    onUndo: () -> Unit,
) {
    val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = actionLabel
    )
    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
        onUndo()
    }
}
