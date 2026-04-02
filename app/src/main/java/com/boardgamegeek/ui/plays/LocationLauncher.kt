package com.boardgamegeek.ui.plays

import android.view.LayoutInflater
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.LocationRoute
import com.boardgamegeek.ui.navigation.popBackStackOrFinish
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPlaysScaffold(
    subtitle: String,
    playCount: Int,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.title_plays) + " - $subtitle")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                actions = {
                    Text(text = playCount.toString())
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.menu_edit))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}

@Composable
fun LocationRouteScreen(
    route: LocationRoute,
    viewModel: PlaysViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val snackbarHostState = remember { SnackbarHostState() }
    val location by viewModel.location.collectAsState()
    val plays by viewModel.plays.collectAsState()

    LaunchedEffect(route.locationName) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
            param(FirebaseAnalytics.Param.ITEM_NAME, route.locationName)
        }
        viewModel.setLocation(route.locationName)
    }

    LaunchedEffect(viewModel) {
        viewModel.updateMessageFlow.collect { content ->
            if (!content.isNullOrBlank()) {
                snackbarHostState.showSnackbar(content)
                viewModel.clearUpdateMessage()
            }
        }
    }

    AppTheme {
        LocationPlaysScaffold(
            subtitle = location.ifBlank { context.getString(R.string.no_location) },
            playCount = plays.sumOf { it.quantity },
            snackbarHostState = snackbarHostState,
            onBack = { navigator.popBackStackOrFinish(context) },
            onEdit = {
                val binding = DialogEditTextBinding.inflate(LayoutInflater.from(context))
                binding.editTextContainer.hint = context.getString(R.string.location_hint)
                binding.editText.setAndSelectExistingText(location)

                val dialog = context.createThemedBuilder()
                    .setTitle(R.string.title_edit_location)
                    .setView(binding.root)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val text = binding.editText.text?.toString().orEmpty().trim()
                        if (text.isNotBlank()) {
                            FirebaseAnalytics.getInstance(context).logEvent("DataManipulation") {
                                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                                param("Action", "Edit")
                            }
                            viewModel.renameLocation(location, text)
                        }
                    }
                    .create()

                dialog.requestFocus(binding.editText)
                dialog.show()
            },
        ) { paddingValues ->
            PlaysScreen(
                viewModel = viewModel,
                emptyStringResId = R.string.empty_plays_location,
                showGameName = true,
                gameId = com.boardgamegeek.provider.BggContract.INVALID_ID,
                gameName = "",
                heroImageUrl = "",
                arePlayersCustomSorted = false,
                iconColor = android.graphics.Color.TRANSPARENT,
                contentPadding = paddingValues,
            )
        }
    }
}
