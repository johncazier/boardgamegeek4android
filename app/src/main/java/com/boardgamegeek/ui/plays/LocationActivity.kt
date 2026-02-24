package com.boardgamegeek.ui.plays

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.longSnackbar
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.dialog.EditLocationNameDialogFragment
import com.boardgamegeek.ui.SimpleSinglePaneActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var locationName = ""
    private var playCount = -1
    private var snackbar: Snackbar? = null

    override val optionsMenuId: Int
        get() = R.menu.location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSubtitle()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param(FirebaseAnalytics.Param.ITEM_NAME, locationName)
            }
        }

        viewModel.setLocation(locationName)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.location.collect {
                        locationName = it
                        intent.putExtra(KEY_LOCATION_NAME, locationName)
                        setSubtitle()
                    }
                }
                launch {
                    viewModel.plays.collect {
                        playCount = it.sumOf { play -> play.quantity }
                        invalidateOptionsMenu()
                    }
                }
                launch {
                    viewModel.updateMessageFlow.collect { content ->
                        if (content.isNullOrBlank()) {
                            snackbar?.dismiss()
                        } else {
                            snackbar = rootContainer?.longSnackbar(content)
                            viewModel.clearUpdateMessage()
                        }
                    }
                }
            }
        }
    }

    override fun readIntent() {
        locationName = intent.getStringExtra(KEY_LOCATION_NAME).orEmpty()
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = locationName.ifBlank { getString(R.string.no_location) }
    }

    override fun createPane() = PlaysFragment.newInstanceForLocation()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_list_count, playCount)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_edit) {
            showAndSurvive(EditLocationNameDialogFragment.newInstance(locationName))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_LOCATION_NAME = "LOCATION_NAME"

        fun start(context: Context, locationName: String) {
            context.startActivity<LocationActivity>(KEY_LOCATION_NAME to locationName)
        }
    }
}
