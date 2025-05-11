package com.boardgamegeek.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boardgamegeek.extensions.formatMedium
import com.boardgamegeek.extensions.toUtcMillis
import com.boardgamegeek.extensions.utcMillisToLocalDate
import java.time.LocalDate
import com.boardgamegeek.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.toUtcMillis())

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(utcMillisToLocalDate(datePickerState.selectedDateMillis))
                    showDialog = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .clickable(onClick = { showDialog = true })
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.Event, contentDescription = null)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = selectedDate.formatMedium(),
            fontSize = 18.sp
        )
    }
}