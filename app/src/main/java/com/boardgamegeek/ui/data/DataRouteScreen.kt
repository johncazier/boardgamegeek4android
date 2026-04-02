package com.boardgamegeek.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@Composable
fun DataRouteScreen(
    viewModel: DataPortViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Data")
        }
    }
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
