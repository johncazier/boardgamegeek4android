package com.boardgamegeek.ui.geeklist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.createBggUri
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.share
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.SearchResultsActivity
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeekListActivity : ComponentActivity() {
    private var geekListId = BggContract.INVALID_ID
    private var geekListInitialTitle: String = "" // Use this for initial title if needed
    private val viewModel by viewModels<GeekListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        geekListInitialTitle = intent.getStringExtra(KEY_TITLE).orEmpty()

        if (savedInstanceState == null) {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListInitialTitle)
            }
        }

        viewModel.setId(geekListId)

        setContent {
            val geekListResource by viewModel.geekList.collectAsState(initial = RefreshableResource.refreshing())
            var currentGeekListTitle by remember { mutableStateOf(geekListInitialTitle) }

            // Update title when data is successfully loaded
            if (geekListResource.status == Status.SUCCESS) {
                geekListResource.data?.let {
                    currentGeekListTitle = it.title
                }
            }

            AppScreen(
                topBarTitle = currentGeekListTitle,
                currentScreenRouteFromActivity = "",
                onSearchClick = {
                    startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                topBarActions = {
                    IconButton(onClick = {
                        linkToBgg("geeklist", geekListId)
                    }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = getString(R.string.menu_view)
                        )
                    }
                    IconButton(onClick = {
                        val description = String.format(getString(R.string.share_geeklist_text), currentGeekListTitle)
                        val uri = createBggUri("geeklist", geekListId)
                        share(getString(R.string.share_geeklist_subject), "$description\n\n$uri")

                        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekList")
                            param(FirebaseAnalytics.Param.ITEM_ID, geekListId.toString())
                            param(FirebaseAnalytics.Param.ITEM_NAME, currentGeekListTitle)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.menu_share)
                        )
                    }
                }
            ) { paddingValues ->
                GeekListScreen(
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                )
            }
        }
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"

        fun start(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title))
        }

        fun startUp(context: Context, id: Int, title: String) {
            context.startActivity(createIntent(context, id, title).clearTop())
        }

        private fun createIntent(context: Context, id: Int, title: String): Intent {
            return context.intentFor<GeekListActivity>(
                KEY_ID to id,
                KEY_TITLE to title,
            )
        }
    }
}