package com.boardgamegeek.ui.gamecollectionitem

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asWishListPriority
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.game.GameLauncher
import com.boardgamegeek.ui.image.ImageLauncher
import com.boardgamegeek.ui.navigation.GameCollectionItemRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCollectionItemRouteScreen(
    route: GameCollectionItemRoute,
    viewModel: GameCollectionItemViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }
    val item by viewModel.item.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isEdited by viewModel.isEdited.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var draft by remember(item) { mutableStateOf(item?.toDraft()) }

    LaunchedEffect(route.internalId) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameCollectionItem")
            param(FirebaseAnalytics.Param.ITEM_ID, route.collectionId.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, route.collectionName)
        }
        viewModel.setInternalId(route.internalId)
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            if (message.isNotBlank()) snackbarHostState.showSnackbar(message)
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = titleFromNames(
                                collectionName = item?.collectionName ?: route.collectionName,
                                gameYear = route.gameYearPublished,
                                collectionYear = item?.collectionYearPublished ?: route.collectionYearPublished,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isEditMode && isEdited) showDiscardDialog = true
                                else if (isEditMode) viewModel.disableEditMode()
                                else navigator.popBackStackOrFinish(context)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    },
                    actions = {
                        val imageUrl = item?.let { current ->
                            current.heroImageUrl.ifBlank { current.thumbnailUrl }
                        }.orEmpty()
                        IconButton(
                            enabled = imageUrl.isNotBlank(),
                            onClick = {
                                ImageLauncher.start(
                                    context,
                                    item?.heroImageUrl?.ifBlank { item?.thumbnailUrl } ?: route.heroImageUrl.ifBlank { route.thumbnailUrl },
                                )
                            },
                        ) {
                            Icon(Icons.Default.Image, contentDescription = stringResource(R.string.menu_view_image))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                        if (!isEditMode && (item?.isDirty == true)) {
                            TextButton(onClick = viewModel::reset) {
                                Text(stringResource(R.string.menu_discard_changes))
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (route.collectionId != BggContract.INVALID_ID) {
                    FloatingActionButton(
                        onClick = {
                            if (isEditMode) {
                                val source = item
                                val edited = draft
                                if (source != null && edited != null) {
                                    viewModel.saveChanges(source, edited)
                                }
                            } else {
                                viewModel.enableEditMode()
                                draft = item?.toDraft()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditMode) stringResource(R.string.save) else stringResource(R.string.edit),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            when {
                isRefreshing && item == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                item == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.invalid_collection_status))
                    }
                }
                else -> {
                    GameCollectionItemScreen(
                        item = item!!,
                        draft = draft ?: item!!.toDraft(),
                        isEditMode = isEditMode,
                        onDraftChange = {
                            draft = it
                            viewModel.markEdited()
                        },
                        paddingValues = paddingValues,
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.are_you_sure_delete_collection_item)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete()
                            showDeleteDialog = false
                            navigator.popBackStackOrFinish(context)
                        },
                    ) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.menu_discard_changes)) },
                text = { Text(stringResource(R.string.discard_changes_message, context.getString(R.string.collection_item))) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.reset()
                            viewModel.disableEditMode()
                            showDiscardDialog = false
                        },
                    ) { Text(stringResource(R.string.menu_discard_changes)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun GameCollectionItemScreen(
    item: CollectionItem,
    draft: CollectionItemDraft,
    isEditMode: Boolean,
    onDraftChange: (CollectionItemDraft) -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = item.heroImageUrl.ifBlank { item.thumbnailUrl },
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Section("Status") {
            if (isEditMode) {
                StatusSwitch("Own", draft.own) { onDraftChange(draft.copy(own = it)) }
                StatusSwitch("Preordered", draft.preordered) { onDraftChange(draft.copy(preordered = it)) }
                StatusSwitch("Previously Owned", draft.previouslyOwned) { onDraftChange(draft.copy(previouslyOwned = it)) }
                StatusSwitch("Want to Buy", draft.wantToBuy) { onDraftChange(draft.copy(wantToBuy = it)) }
                StatusSwitch("Want to Play", draft.wantToPlay) { onDraftChange(draft.copy(wantToPlay = it)) }
            } else {
                Text(getStatusDescription(context, item), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Section("Rating & Comment") {
            if (isEditMode) {
                OutlinedTextField(
                    value = if (draft.rating <= 0.0) "" else draft.rating.toString(),
                    onValueChange = { onDraftChange(draft.copy(rating = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text(stringResource(R.string.rating)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.comment,
                    onValueChange = { onDraftChange(draft.copy(comment = it)) },
                    label = { Text(stringResource(R.string.comment)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(stringResource(R.string.rating) + ": ${item.rating}", style = MaterialTheme.typography.bodyMedium)
                Text(item.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Section(stringResource(R.string.wishlist)) {
            if (isEditMode) {
                StatusSwitch(stringResource(R.string.collection_status_wishlist), draft.wishlist) {
                    onDraftChange(draft.copy(wishlist = it))
                }
                if (draft.wishlist) {
                    OutlinedTextField(
                        value = draft.wishlistPriority.toString(),
                        onValueChange = { onDraftChange(draft.copy(wishlistPriority = it.toIntOrNull()?.coerceIn(1, 5) ?: 1)) },
                        label = { Text(stringResource(R.string.collection_sort_wishlist_priority)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = draft.wishlistComment,
                    onValueChange = { onDraftChange(draft.copy(wishlistComment = it)) },
                    label = { Text(stringResource(R.string.wishlist_comment)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (item.wishList) {
                    Text(item.wishListPriority.asWishListPriority(context), style = MaterialTheme.typography.bodyMedium)
                }
                Text(item.wishListComment, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Section(stringResource(R.string.private_info)) {
            if (isEditMode) {
                OutlinedTextField(
                    value = draft.quantity?.toString().orEmpty(),
                    onValueChange = { onDraftChange(draft.copy(quantity = it.toIntOrNull())) },
                    label = { Text(stringResource(R.string.quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.pricePaid?.toString().orEmpty(),
                    onValueChange = { onDraftChange(draft.copy(pricePaid = it.toDoubleOrNull())) },
                    label = { Text(stringResource(R.string.price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.currentValue?.toString().orEmpty(),
                    onValueChange = { onDraftChange(draft.copy(currentValue = it.toDoubleOrNull())) },
                    label = { Text(stringResource(R.string.current_value)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.acquiredFrom.orEmpty(),
                    onValueChange = { onDraftChange(draft.copy(acquiredFrom = it)) },
                    label = { Text(stringResource(R.string.acquired_from)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.inventoryLocation.orEmpty(),
                    onValueChange = { onDraftChange(draft.copy(inventoryLocation = it)) },
                    label = { Text(stringResource(R.string.inventory_location)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.privateComment,
                    onValueChange = { onDraftChange(draft.copy(privateComment = it)) },
                    label = { Text(stringResource(R.string.private_comment)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(item.getPrivateInfo(context).toString(), style = MaterialTheme.typography.bodyMedium)
                Text(item.privateComment, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Section(stringResource(R.string.title_trade)) {
            if (isEditMode) {
                StatusSwitch(stringResource(R.string.collection_status_for_trade), draft.forTrade) {
                    onDraftChange(draft.copy(forTrade = it))
                }
                StatusSwitch(stringResource(R.string.collection_status_want_in_trade), draft.wantInTrade) {
                    onDraftChange(draft.copy(wantInTrade = it))
                }
                OutlinedTextField(
                    value = draft.conditionText,
                    onValueChange = { onDraftChange(draft.copy(conditionText = it)) },
                    label = { Text(stringResource(R.string.trade_condition)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.wantPartsList,
                    onValueChange = { onDraftChange(draft.copy(wantPartsList = it)) },
                    label = { Text(stringResource(R.string.want_parts_list)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.hasPartsList,
                    onValueChange = { onDraftChange(draft.copy(hasPartsList = it)) },
                    label = { Text(stringResource(R.string.has_parts_list)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(if (item.forTrade) stringResource(R.string.collection_status_for_trade) else "", style = MaterialTheme.typography.bodyMedium)
                Text(if (item.wantInTrade) stringResource(R.string.collection_status_want_in_trade) else "", style = MaterialTheme.typography.bodyMedium)
                Text(item.conditionText, style = MaterialTheme.typography.bodyMedium)
                Text(item.wantPartsList, style = MaterialTheme.typography.bodyMedium)
                Text(item.hasPartsList, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            text = stringResource(R.string.last_modified_prefix, item.lastModifiedDate.formatTimestamp(context)),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.synced_prefix, item.syncTimestamp.formatTimestamp(context)),
            style = MaterialTheme.typography.bodySmall
        )
        if (item.collectionId != BggContract.INVALID_ID) {
            Text(item.collectionId.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun StatusSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun titleFromNames(collectionName: String, gameYear: Int, collectionYear: Int): String {
    return if (collectionYear == CollectionItem.YEAR_UNKNOWN || collectionYear == gameYear) {
        collectionName
    } else {
        "$collectionName ($collectionYear)"
    }
}

private fun getStatusDescription(context: Context, item: CollectionItem): String {
    val statuses = mutableListOf<String>()
    if (item.own) statuses += context.getString(R.string.collection_status_own)
    if (item.previouslyOwned) statuses += context.getString(R.string.collection_status_prev_owned)
    if (item.wantToBuy) statuses += context.getString(R.string.collection_status_want_to_buy)
    if (item.wantToPlay) statuses += context.getString(R.string.collection_status_want_to_play)
    if (item.preOrdered) statuses += context.getString(R.string.collection_status_preordered)
    return if (statuses.isEmpty() && item.numberOfPlays > 0) context.getString(R.string.played) else statuses.formatList()
}

private fun CollectionItem.toDraft() = CollectionItemDraft(
    own = own,
    preordered = preOrdered,
    previouslyOwned = previouslyOwned,
    wantToBuy = wantToBuy,
    wantToPlay = wantToPlay,
    forTrade = forTrade,
    wantInTrade = wantInTrade,
    wishlist = wishList,
    wishlistPriority = wishListPriority.coerceIn(1, 5),
    rating = rating,
    comment = comment,
    privateComment = privateComment,
    wishlistComment = wishListComment,
    conditionText = conditionText,
    wantPartsList = wantPartsList,
    hasPartsList = hasPartsList,
    pricePaidCurrency = pricePaidCurrency,
    pricePaid = pricePaid,
    currentValueCurrency = currentValueCurrency,
    currentValue = currentValue,
    quantity = quantity,
    acquisitionDate = acquisitionDate,
    acquiredFrom = acquiredFrom,
    inventoryLocation = inventoryLocation,
)
