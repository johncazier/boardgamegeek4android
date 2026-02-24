package com.boardgamegeek.ui.plays

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.BuddyActivity
import com.boardgamegeek.ui.SimpleSinglePaneActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BuddyPlaysActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()
    private var buddyName = ""
    private var numberOfPlays = -1

    override val optionsMenuId: Int
        get() = R.menu.text_only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (buddyName.isNotBlank()) {
            supportActionBar?.subtitle = buddyName
        }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyPlays")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName)
            }
        }

        viewModel.setUsername(buddyName)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.plays.collect {
                        numberOfPlays = it.sumOf { play -> play.quantity }
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    override fun readIntent() {
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME).orEmpty()
    }

    override fun createPane() = PlaysFragment.newInstanceForBuddy()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_text, numberOfPlays)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                BuddyActivity.startUp(this, buddyName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"

        fun start(context: Context, buddyName: String?) {
            context.startActivity<BuddyPlaysActivity>(
                KEY_BUDDY_NAME to buddyName,
            )
        }
    }
}
