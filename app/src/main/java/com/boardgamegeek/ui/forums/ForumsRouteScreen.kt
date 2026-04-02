package com.boardgamegeek.ui.forums

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.theme.AppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

@Composable
fun ForumsRouteScreen(
    viewModel: ForumsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forums")
        }
    }
    AppTheme {
        AppScreen(
            topBarTitle = stringResource(R.string.title_forums),
            currentScreenRouteFromActivity = "forums",
        ) { paddingValues ->
            ForumsScreen(
                viewModel = viewModel,
                forumType = Forum.Type.REGION,
                objectId = BggContract.INVALID_ID,
                objectName = "",
                paddingValues = paddingValues,
            )
        }
    }
}
