package com.boardgamegeek.ui.mechanic

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.game.GameActivity
import com.boardgamegeek.ui.linkedcollection.LinkedCollectionScreen
import com.boardgamegeek.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MechanicActivity : ComponentActivity() {
    private var id = BggContract.INVALID_ID
    private var name = ""

    private val viewModel by viewModels<MechanicViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent()
        viewModel.setId(id)

        setContent {
            val sort by viewModel.sort.collectAsStateWithLifecycle()
            val collection by viewModel.collection.collectAsStateWithLifecycle()
            val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
            var showMenu by remember { mutableStateOf(false) }

            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                androidx.compose.foundation.layout.Column {
                                    Text(text = name)
                                    Text(
                                        text = stringResource(R.string.title_mechanic),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.menu_back)
                                    )
                                }
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_sort_name)) },
                                            onClick = {
                                                viewModel.setSort(CollectionItem.SortType.NAME)
                                                showMenu = false
                                            },
                                            trailingIcon = {
                                                if (sort == CollectionItem.SortType.NAME) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_sort_rating)) },
                                            onClick = {
                                                viewModel.setSort(CollectionItem.SortType.RATING)
                                                showMenu = false
                                            },
                                            trailingIcon = {
                                                if (sort == CollectionItem.SortType.RATING) {
                                                    Text("\u2713")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_refresh)) },
                                            onClick = {
                                                viewModel.reload()
                                                showMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_view)) },
                                            onClick = {
                                                linkToBgg("boardgamemechanic", id)
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    LinkedCollectionScreen(
                        collection = collection,
                        emptyMessage = stringResource(
                            R.string.empty_linked_collection,
                            stringResource(R.string.title_mechanic).lowercase(Locale.getDefault())
                        ),
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::reload,
                        onItemClick = { item ->
                            GameActivity.start(
                                this,
                                item.gameId,
                                item.gameName,
                                item.gameThumbnailUrl,
                                item.gameHeroImageUrl
                            )
                        },
                        paddingValues = paddingValues,
                    )
                }
            }
        }
    }

    private fun readIntent() {
        id = intent.getIntExtra(KEY_MECHANIC_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_MECHANIC_NAME).orEmpty()
    }

    companion object {
        private const val KEY_MECHANIC_ID = "MECHANIC_ID"
        private const val KEY_MECHANIC_NAME = "MECHANIC_NAME"

        fun start(context: Context, mechanicId: Int, mechanicName: String) {
            context.startActivity<MechanicActivity>(
                KEY_MECHANIC_ID to mechanicId,
                KEY_MECHANIC_NAME to mechanicName,
            )
        }
    }
}
