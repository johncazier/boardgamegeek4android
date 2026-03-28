package com.boardgamegeek.ui.collection

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFactory

private data class SheetSection(
    @StringRes val titleResId: Int,
    val options: List<SheetOption>,
)

private data class SheetOption(
    val type: Int,
    @StringRes val labelResId: Int,
)

private fun typeFromResource(context: android.content.Context, @StringRes resId: Int): Int {
    return context.getString(resId).toInt()
}

@Composable
private fun rememberCollectionSortSections(): List<SheetSection> {
    val context = LocalContext.current
    val includePlayDate = context.preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] == true
    return remember(context, includePlayDate) {
        listOf(
            SheetSection(
                titleResId = R.string.collection_sort_personal,
                options = buildList {
                    add(SheetOption(typeFromResource(context, R.string.collection_sort_type_my_rating), R.string.collection_sort_my_rating))
                    add(SheetOption(typeFromResource(context, R.string.collection_sort_type_wishlist_priority), R.string.collection_sort_wishlist_priority))
                    add(SheetOption(typeFromResource(context, R.string.collection_sort_type_play_count_desc), R.string.collection_sort_play_count))
                    if (includePlayDate) {
                        add(SheetOption(typeFromResource(context, R.string.collection_sort_type_play_date_max), R.string.collection_sort_play_date))
                    }
                    add(SheetOption(typeFromResource(context, R.string.collection_sort_type_last_modified), R.string.collection_sort_last_modified))
                },
            ),
            SheetSection(
                titleResId = R.string.collection_sort_private_info,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_acquisition_date), R.string.acquisition_date),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_acquired_from), R.string.acquired_from),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_inventory_location), R.string.collection_sort_inventory_location),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_price_paid), R.string.collection_sort_price_paid),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_current_value), R.string.collection_sort_current_value),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_sort_stats,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_collection_name), R.string.collection_sort_collection_name),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_year_published_asc), R.string.collection_sort_year_published),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_play_time_asc), R.string.collection_sort_play_time),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_suggested_age_asc), R.string.collection_sort_suggested_age),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_sort_community,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_rank), R.string.collection_sort_rank),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_geek_rating), R.string.collection_sort_geek_rating),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_average_rating), R.string.collection_sort_average_rating),
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_average_weight_asc), R.string.collection_sort_average_weight),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_sort_local,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_sort_type_last_viewed), R.string.collection_sort_last_viewed),
                ),
            ),
        )
    }
}

@Composable
private fun rememberCollectionFilterSections(): List<SheetSection> {
    val context = LocalContext.current
    return remember(context) {
        listOf(
            SheetSection(
                titleResId = R.string.collection_filter_personal,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_collection_status), R.string.menu_collection_status),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_my_rating), R.string.menu_my_rating),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_comment), R.string.menu_comment),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_play_count), R.string.menu_play_count),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_filter_private_info,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_acquired_from), R.string.menu_acquired_from),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_inventory_location), R.string.menu_inventory_location),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_private_comment), R.string.menu_private_comment),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_filter_stats,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_collection_name), R.string.menu_collection_name),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_subtype), R.string.menu_expansion_status),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_number_of_players), R.string.menu_number_of_players),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_year_published), R.string.menu_year_published),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_play_time), R.string.menu_play_time),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_suggested_age), R.string.menu_suggested_age),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_filter_community,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_geek_ranking), R.string.menu_geek_ranking),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_geek_rating), R.string.menu_geek_rating),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_average_rating), R.string.menu_average_rating),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_recommended_player_count), R.string.menu_recommended_player_count),
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_average_weight), R.string.menu_average_weight),
                ),
            ),
            SheetSection(
                titleResId = R.string.collection_sort_local,
                options = listOf(
                    SheetOption(typeFromResource(context, R.string.collection_filter_type_favorite), R.string.menu_favorite),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSortSheet(
    currentSortType: Int,
    onDismiss: () -> Unit,
    onSortSelected: (Int) -> Unit,
) {
    val sections = rememberCollectionSortSections()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            sections.forEach { section ->
                item(key = "sort_header_${section.titleResId}") {
                    Text(
                        text = stringResource(section.titleResId),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                items(section.options, key = { option -> "sort_${option.type}" }) { option ->
                    ListItem(
                        headlineContent = { Text(stringResource(option.labelResId)) },
                        trailingContent = {
                            if (option.type == currentSortType) {
                                Text(text = stringResource(R.string.yes), style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            onSortSelected(option.type)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(text = stringResource(option.labelResId))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionFilterSheet(
    filters: List<CollectionFilterer>,
    onDismiss: () -> Unit,
    onFilterRemoved: (Int) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity
    val sections = rememberCollectionFilterSections()
    val factory = remember(context) { CollectionFiltererFactory(context) }
    val dialogFactory = remember { CollectionFilterDialogFactory() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            sections.forEach { section ->
                item(key = "filter_header_${section.titleResId}") {
                    Text(
                        text = stringResource(section.titleResId),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                items(section.options, key = { option -> "filter_${option.type}" }) { option ->
                    val appliedFilter = filters.find { it.type == option.type }
                    ListItem(
                        headlineContent = { Text(stringResource(option.labelResId)) },
                        supportingContent = {
                            appliedFilter?.description()?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        },
                        trailingContent = {
                            if (appliedFilter != null) {
                                TextButton(onClick = {
                                    onFilterRemoved(option.type)
                                    onDismiss()
                                }) {
                                    Text(stringResource(R.string.clear))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            if (factory.create(option.type) != null) {
                                activity?.let { host ->
                                    dialogFactory.create(context, option.type)?.createDialog(host, appliedFilter)
                                }
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(text = stringResource(option.labelResId))
                    }
                }
            }
        }
    }
}

@Composable
fun SaveCollectionViewDialog(
    initialName: String,
    description: String,
    selectedViewId: Int,
    viewModel: CollectionViewModel,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var isDefault by remember(initialName, viewModel.defaultViewIdFlow.value) {
        mutableStateOf(
            viewModel.defaultViewIdFlow.value != CollectionViewPrefs.DEFAULT_DEFAULT_ID &&
                viewModel.findViewId(initialName) == viewModel.defaultViewIdFlow.value,
        )
    }
    var showConflictDialog by remember { mutableStateOf(false) }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text(stringResource(R.string.title_collection_view_name_in_use)) },
            text = { Text(stringResource(R.string.msg_collection_view_name_in_use)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.update(name.trim(), isDefault)
                    showConflictDialog = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.update))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.insert(name.trim(), isDefault)
                    showConflictDialog = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.create))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_save_view)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text(stringResource(R.string.set_as_default))
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) return@TextButton
                    val existingId = viewModel.findViewId(trimmedName)
                    when {
                        existingId > 0 && existingId != selectedViewId -> showConflictDialog = true
                        selectedViewId > 0 -> {
                            viewModel.update(trimmedName, isDefault)
                            onDismiss()
                        }
                        else -> {
                            viewModel.insert(trimmedName, isDefault)
                            onDismiss()
                        }
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun DeleteCollectionViewDialog(
    views: List<CollectionView>,
    onDeleteView: (CollectionView) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<CollectionView?>(null) }

    pendingDelete?.let { view ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.are_you_sure_title)) },
            text = { Text(stringResource(R.string.are_you_sure_delete_collection_view, view.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteView(view)
                    pendingDelete = null
                    onDismiss()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_delete_view)) },
        text = {
            if (views.isEmpty()) {
                Text("No saved collection views.")
            } else {
                Column {
                    views.forEachIndexed { index, view ->
                        TextButton(
                            onClick = { pendingDelete = view },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(view.name, modifier = Modifier.fillMaxWidth())
                        }
                        if (index < views.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
