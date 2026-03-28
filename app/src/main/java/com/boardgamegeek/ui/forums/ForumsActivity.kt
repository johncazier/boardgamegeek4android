package com.boardgamegeek.ui.forums

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.ui.navigation.ForumsRoute
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumsActivity : ComponentActivity() {
    private val viewModel by viewModels<ForumsViewModel>()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forums")
            }
        }

        setContent {
            ForumsRouteScreen()
        }
    }

    companion object {
        fun startUp(context: Context) =
            context.startActivity(MainActivity.createIntent(context, ForumsRoute).clearTop())
    }
}

@Composable
fun ForumsRouteScreen(
    viewModel: ForumsViewModel = hiltViewModel(),
) {
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
