package com.boardgamegeek.ui.gamecolors

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R

@Composable
fun AddColorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var colorName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_color)) },
        text = {
            OutlinedTextField(
                value = colorName,
                onValueChange = { colorName = it },
                label = { Text(stringResource(R.string.color_name)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(colorName) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
