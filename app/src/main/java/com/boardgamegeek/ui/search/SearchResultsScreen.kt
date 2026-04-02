package com.boardgamegeek.ui.search

import android.app.SearchManager
import android.content.Context
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.extensions.shareGame
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.logplay.LogPlayLauncher
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: SearchViewModel,
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onGameOpen: (SearchResult) -> Unit,
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val queryState by viewModel.query.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val firebaseAnalytics = remember { FirebaseAnalytics.getInstance(context) }
    var searchText by remember { mutableStateOf(initialQuery) }
    var selectedIds by remember { mutableStateOf(emptySet<Int>()) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && searchText != initialQuery) {
            searchText = initialQuery
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.errorMessage.collectLatest { message ->
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.loggedPlayResult.collectLatest {
            context.notifyLoggedPlay(it)
        }
    }

    LaunchedEffect(searchResults, queryState) {
        val result = searchResults ?: return@LaunchedEffect
        if (result.status != Status.SUCCESS) return@LaunchedEffect
        val query = queryState ?: return@LaunchedEffect
        if (query.first.isBlank()) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }

        val count = result.data?.size ?: 0
        val messageId = if (query.second) R.plurals.search_results_exact else R.plurals.search_results
        val message = context.resources.getQuantityString(messageId, count, count, query.first)
        val actionLabel = if (query.second) context.getString(R.string.more) else null

        val snackbarResult = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Indefinite
        )
        if (snackbarResult == androidx.compose.material3.SnackbarResult.ActionPerformed && query.second) {
            viewModel.searchInexact(query.first)
        }
    }

    LaunchedEffect(searchResults?.data) {
        val ids = searchResults?.data?.map { it.id }?.toSet().orEmpty()
        if (selectedIds.isNotEmpty()) {
            val updated = selectedIds.filter { ids.contains(it) }.toSet()
            if (updated.size != selectedIds.size) {
                selectedIds = updated
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AndroidSearchView(
                        queryText = searchText,
                        onQueryChange = {
                            searchText = it
                            onQueryChange(it)
                        },
                        onQuerySubmit = {
                            searchText = it
                            onSearchSubmit(it)
                        },
                        onClose = onBack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.menu_back)
                        )
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        Text(
                            text = selectedIds.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box {
                            IconButton(onClick = { showSelectionMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            SelectionMenu(
                                expanded = showSelectionMenu,
                                onDismiss = { showSelectionMenu = false },
                                selectedItems = selectedIds,
                                results = searchResults?.data.orEmpty(),
                                onClearSelection = { selectedIds = emptySet() },
                                onLogPlay = { result ->
                                    LogPlayLauncher.logPlay(context, result.id, result.name)
                                },
                                onQuickLogPlay = { result ->
                                    viewModel.logQuickPlay(result.id, result.name)
                                },
                                onShare = { results ->
                                    if (results.size == 1) {
                                        val item = results.first()
                                        (context as? androidx.activity.ComponentActivity)?.shareGame(item.id, item.name, "Search", firebaseAnalytics)
                                    } else {
                                        val games = results.map { it.id to it.name }
                                        (context as? androidx.activity.ComponentActivity)?.shareGames(games, "Search", firebaseAnalytics)
                                    }
                                },
                                onLink = { result -> context.linkBgg(result.id) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SearchResultsContent(
            paddingValues = paddingValues,
            searchResults = searchResults,
            queryState = queryState,
            selectedIds = selectedIds,
            onSelectionChange = { selectedIds = it },
            onGameOpen = onGameOpen
        )
    }
}

@Composable
private fun SearchResultsContent(
    paddingValues: PaddingValues,
    searchResults: com.boardgamegeek.model.RefreshableResource<List<SearchResult>>?,
    queryState: Pair<String, Boolean>?,
    selectedIds: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit,
    onGameOpen: (SearchResult) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when (val result = searchResults) {
            null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                when (result.status) {
                    Status.REFRESHING -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    Status.ERROR -> {
                        val queryText = queryState?.first.orEmpty()
                        Text(
                            text = if (queryText.isBlank()) {
                                stringResource(R.string.search_error_no_data)
                            } else {
                                stringResource(R.string.search_error, queryText, result.message)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Status.SUCCESS -> {
                        val data = result.data.orEmpty()
                        if (data.isEmpty()) {
                            val queryText = queryState?.first.orEmpty()
                            Text(
                                text = if (queryText.isBlank()) {
                                    stringResource(R.string.search_initial_help)
                                } else {
                                    stringResource(R.string.empty_search)
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(data, key = { it.id }) { item ->
                                    val isSelected = selectedIds.contains(item.id)
                                    SearchResultRow(
                                        result = item,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (selectedIds.isNotEmpty()) {
                                                val updated = selectedIds.toMutableSet()
                                                if (isSelected) updated.remove(item.id) else updated.add(item.id)
                                                onSelectionChange(updated)
                                            } else {
                                                onGameOpen(item)
                                            }
                                        },
                                        onLongClick = {
                                            val updated = selectedIds.toMutableSet()
                                            if (isSelected) updated.remove(item.id) else updated.add(item.id)
                                            onSelectionChange(updated)
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
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val fontStyle = if (result.nameType == SearchResult.NAME_TYPE_ALTERNATE) FontStyle.Italic else FontStyle.Normal
    val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = result.name,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = fontStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = result.yearPublished.asYear(context),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = context.getString(R.string.id_list_text, result.id.toString()),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun AndroidSearchView(
    queryText: String,
    onQueryChange: (String) -> Unit,
    onQuerySubmit: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            SearchView(ctx).apply {
                val searchManager = ctx.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
                setSearchableInfo(searchManager?.getSearchableInfo((ctx as? androidx.activity.ComponentActivity)?.componentName))
                isIconified = false
                setOnCloseListener {
                    onClose()
                    true
                }
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        if (query != null && query.length > 1) {
                            onQuerySubmit(query)
                        }
                        clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        onQueryChange(newText.orEmpty())
                        return true
                    }
                })
            }
        },
        update = { view ->
            if (view.query.toString() != queryText) {
                view.setQuery(queryText, false)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedItems: Set<Int>,
    results: List<SearchResult>,
    onClearSelection: () -> Unit,
    onLogPlay: (SearchResult) -> Unit,
    onQuickLogPlay: (SearchResult) -> Unit,
    onShare: (List<SearchResult>) -> Unit,
    onLink: (SearchResult) -> Unit,
) {
    val context = LocalContext.current
    val selectedResults = results.filter { selectedItems.contains(it.id) }
    val signedIn = Authenticator.isSignedIn(context)

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (signedIn && selectedResults.size == 1) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_log_play_short)) },
                onClick = {
                    onLogPlay(selectedResults.first())
                    onClearSelection()
                    onDismiss()
                }
            )
        }
        if (signedIn) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_log_play_quick_short)) },
                onClick = {
                    selectedResults.forEach { onQuickLogPlay(it) }
                    onClearSelection()
                    onDismiss()
                }
            )
        }
        if (selectedResults.isNotEmpty()) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_share)) },
                onClick = {
                    onShare(selectedResults)
                    onClearSelection()
                    onDismiss()
                }
            )
        }
        if (selectedResults.size == 1) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.link_bgg)) },
                onClick = {
                    onLink(selectedResults.first())
                    onClearSelection()
                    onDismiss()
                }
            )
        }
    }
}
