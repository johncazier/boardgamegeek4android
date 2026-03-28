package com.boardgamegeek.ui.plays

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogEditTextBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.requestFocus
import com.boardgamegeek.extensions.setAndSelectExistingText
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationActivity : AppCompatActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var locationName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()

        if (savedInstanceState == null) {
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param(FirebaseAnalytics.Param.ITEM_NAME, locationName)
            }
        }

        viewModel.setLocation(locationName)

        setContent {
            AppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val location by viewModel.location.collectAsState()
                val plays by viewModel.plays.collectAsState()

                LaunchedEffect(viewModel) {
                    viewModel.updateMessageFlow.collect { content ->
                        if (!content.isNullOrBlank()) {
                            snackbarHostState.showSnackbar(content)
                            viewModel.clearUpdateMessage()
                        }
                    }
                }

                LocationPlaysScaffold(
                    subtitle = location.ifBlank { getString(R.string.no_location) },
                    playCount = plays.sumOf { it.quantity },
                    snackbarHostState = snackbarHostState,
                    onBack = { finish() },
                    onEdit = { showEditLocationDialog(location) },
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
    }

    private fun readIntent() {
        locationName = intent.getStringExtra(KEY_LOCATION_NAME).orEmpty()
    }

    private fun showEditLocationDialog(currentLocation: String) {
        val binding = DialogEditTextBinding.inflate(layoutInflater)
        binding.editTextContainer.hint = getString(R.string.location_hint)
        binding.editText.setAndSelectExistingText(currentLocation)

        val dialog = createThemedBuilder()
            .setTitle(R.string.title_edit_location)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val text = binding.editText.text?.toString().orEmpty().trim()
                if (text.isNotBlank()) {
                    FirebaseAnalytics.getInstance(this).logEvent("DataManipulation") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                        param("Action", "Edit")
                    }
                    viewModel.renameLocation(currentLocation, text)
                }
            }
            .create()

        dialog.requestFocus(binding.editText)
        dialog.show()
    }

    companion object {
        private const val KEY_LOCATION_NAME = "LOCATION_NAME"

        fun start(context: Context, locationName: String) {
            context.startActivity<LocationActivity>(KEY_LOCATION_NAME to locationName)
        }
    }
}

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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}
