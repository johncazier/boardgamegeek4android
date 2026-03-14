package com.boardgamegeek.ui.buddy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R

@Composable
fun EditBuddyNicknameDialog(
    initialNickname: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var nickname by remember(initialNickname) { mutableStateOf(initialNickname.orEmpty()) }
    var updatePlays by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_edit_nickname)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.nickname)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                )
                RowWithCheckbox(
                    checked = updatePlays,
                    text = stringResource(R.string.nickname_update_plays),
                    onCheckedChange = { updatePlays = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nickname.trim(), updatePlays) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun RenamePlayerDialog(
    initialName: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newName by remember(initialName) { mutableStateOf(initialName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_edit_player)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.player_name)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName.trim()) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun AddUsernameDialog(
    isUsernameValid: Boolean?,
    isValidating: Boolean,
    onValidate: (String) -> Unit,
    onUsernameChanged: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_username)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onUsernameChanged()
                    },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                )
                Button(
                    onClick = { onValidate(username.trim()) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    val icon = when (isUsernameValid) {
                        null -> Icons.Default.Sync
                        true -> Icons.Default.CheckCircle
                        false -> Icons.Default.Cancel
                    }
                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (isUsernameValid) {
                            null -> stringResource(R.string.validate)
                            true -> stringResource(R.string.valid)
                            false -> stringResource(R.string.not_found)
                        }
                    )
                }
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = username.trim().isNotBlank(),
                onClick = { onConfirm(username.trim()) },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun RowWithCheckbox(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text)
    }
}
