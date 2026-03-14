package com.boardgamegeek.ui.buddycollection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createStatusMap
import com.boardgamegeek.mappers.mapFromResourceToEnum
import com.boardgamegeek.mappers.mapToResource
import com.boardgamegeek.ui.buddy.BuddyActivity
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class BuddyCollectionActivity : ComponentActivity() {
    private var buddyName = ""
    private val viewModel by viewModels<BuddyCollectionViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME).orEmpty()
        if (buddyName.isBlank()) {
            Timber.w("Missing buddy name.")
            finish()
            return
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName)
            }
        }

        viewModel.setUsername(buddyName)

        setContent {
            val status by viewModel.status.collectAsStateWithLifecycle()
            val collectionResource by viewModel.collection.collectAsStateWithLifecycle()
            val statuses = remember { createStatusMap() }
            var showFilterMenu by remember { mutableStateOf(false) }

            val statusLabel = statuses[status.mapToResource()].orEmpty()
            val collection = collectionResource?.data.orEmpty()

            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (statusLabel.isBlank()) buddyName else "$buddyName - $statusLabel"
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    BuddyActivity.startUp(this, buddyName)
                                    finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = stringResource(R.string.menu_collection_status_)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    statuses.forEach { (key, value) ->
                                        DropdownMenuItem(
                                            text = { Text(value) },
                                            onClick = {
                                                viewModel.setStatus(key.mapFromResourceToEnum())
                                                showFilterMenu = false
                                            }
                                        )
                                    }
                                }
                                if (collection.isNotEmpty()) {
                                    IconButton(onClick = {
                                        collection.randomOrNull()?.let { randomItem ->
                                            GameActivity.start(this@BuddyCollectionActivity, randomItem.gameId, randomItem.gameName, randomItem.thumbnailUrl)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = stringResource(R.string.menu_collection_random_game)
                                        )
                                    }
                                }
                            }
                        )
                    },
                ) { paddingValues ->
                    BuddyCollectionScreen(
                        resource = collectionResource,
                        paddingValues = paddingValues,
                        onGameClick = { item ->
                            GameActivity.start(this, item.gameId, item.gameName, item.thumbnailUrl)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"

        fun start(context: Context, buddyName: String?) {
            context.startActivity(Intent(context, BuddyCollectionActivity::class.java).apply {
                putExtra(KEY_BUDDY_NAME, buddyName)
            })
        }
    }
}
