package com.boardgamegeek.ui.collectiondetails

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.ui.AppScreen
import com.boardgamegeek.ui.SearchResultsActivity
import com.boardgamegeek.ui.navigation.BottomNavItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailsActivity : AppCompatActivity() {
    private val viewModel by viewModels<CollectionDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loggedPlayResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                notifyLoggedPlay(it)
            }
        }

        viewModel.refresh()

        setContent {
            AppScreen(
                topBarTitle = stringResource(R.string.title_collection_details),
                currentScreenRouteFromActivity = BottomNavItem.Collection.route,
                onSearchClick = { startActivity(intentFor<SearchResultsActivity>()) },
                drawerGesturesEnabled = false
            ) { paddingValues ->
                CollectionDetailsScreen(viewModel = viewModel, paddingValues = paddingValues)
            }
        }
    }
}
