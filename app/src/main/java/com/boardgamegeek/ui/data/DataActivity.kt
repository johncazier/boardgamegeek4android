package com.boardgamegeek.ui.data

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DataActivity : ComponentActivity() {
    private val viewModel by viewModels<DataPortViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Data")
            }
        }

        setContent {
            DataRouteScreen()
        }
    }
}

@Composable
fun DataRouteScreen(
    viewModel: DataPortViewModel = hiltViewModel(),
) {
    AppTheme {
        AppScreen(
            topBarTitle = stringResource(R.string.title_backup),
            currentScreenRouteFromActivity = "data",
        ) { paddingValues ->
            DataScreen(
                viewModel = viewModel,
                paddingValues = paddingValues,
            )
        }
    }
}
