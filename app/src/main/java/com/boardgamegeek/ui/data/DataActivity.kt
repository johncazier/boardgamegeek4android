package com.boardgamegeek.ui.data

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.search.SearchResultsActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataActivity : ComponentActivity() {
    private val viewModel by viewModels<DataPortViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Data")
            }
        }

        setContent {
            AppTheme {
                AppScreen(
                    topBarTitle = stringResource(R.string.title_backup),
                    currentScreenRouteFromActivity = "data",
                    onSearchClick = { startActivity(Intent(this, SearchResultsActivity::class.java)) }
                ) { paddingValues ->
                    DataScreen(
                        viewModel = viewModel,
                        paddingValues = paddingValues,
                    )
                }
            }
        }
    }
}
