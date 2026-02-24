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
class PlayerPlaysActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var name = ""
    private var playCount = -1

    override val optionsMenuId: Int
        get() = R.menu.text_only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (name.isNotBlank()) {
            supportActionBar?.subtitle = name
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerPlays")
                param(FirebaseAnalytics.Param.ITEM_NAME, name)
            }
        }

        viewModel.setPlayerName(name)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.plays.collect {
                        playCount = it.sumOf { play -> play.quantity }
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    override fun readIntent() {
        name = intent.getStringExtra(KEY_PLAYER_NAME).orEmpty()
    }

    override fun createPane() = PlaysFragment.newInstanceForPlayer()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_text, playCount)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                BuddyActivity.startUp(this, "", name)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, playerName: String?) {
            context.startActivity<PlayerPlaysActivity>(
                KEY_PLAYER_NAME to playerName
            )
        }
    }
}
