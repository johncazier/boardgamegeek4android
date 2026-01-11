package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.link
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeekListItemActivity : ComponentActivity() {
    private var geekListId = 0
    private var geekListTitle = ""
    private var order = 0
    private var geekListItem = GeekListItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geekListTitle = intent.getStringExtra(KEY_TITLE).orEmpty()
        geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID)
        order = intent.getIntExtra(KEY_ORDER, 0)
        geekListItem = intent.getParcelableCompat(KEY_ITEM) ?: GeekListItem()

        if (savedInstanceState == null && geekListItem.objectId != BggContract.INVALID_ID) {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekListItem")
                param(FirebaseAnalytics.Param.ITEM_ID, geekListItem.objectId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, geekListItem.objectName)
            }
        }

        setContent {
            AppScreen(
                topBarTitle = geekListItem.objectName,
                currentScreenRouteFromActivity = "",
                onSearchClick = {
                     startActivity(Intent(this, SearchResultsActivity::class.java))
                },
                topBarActions = {
                    IconButton(onClick = {
                        if (geekListItem.isBoardGame) {
                            if (geekListItem.objectId != BggContract.INVALID_ID && geekListItem.objectName.isNotBlank()) {
                                start(this@GeekListItemActivity, geekListItem.objectId, geekListItem.objectName)
                            }
                        } else {
                            if (geekListItem.objectUrl.isNotBlank()) {
                                link(geekListItem.objectUrl)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = stringResource(R.string.menu_view)
                        )
                    }
                }
            ) { paddingValues ->
                GeekListItemScreen(
                    geekListItem = geekListItem,
                    geekListTitle = geekListTitle,
                    order = order,
                    paddingValues = paddingValues
                )
            }
        }
    }

    companion object {
        private const val KEY_ID = "GEEK_LIST_ID"
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "GEEK_LIST_ITEM"

        fun start(context: Context, geekList: GeekList, item: GeekListItem, order: Int) {
            context.startActivity<GeekListItemActivity>(
                KEY_ID to geekList.id,
                KEY_TITLE to geekList.title,
                KEY_ORDER to order,
                KEY_ITEM to item,
            )
        }
    }
}