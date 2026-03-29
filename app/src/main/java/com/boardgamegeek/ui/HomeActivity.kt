package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.navigation.BuddiesRoute
import com.boardgamegeek.ui.navigation.CollectionDetailsRoute
import com.boardgamegeek.ui.navigation.HotnessRoute
import com.boardgamegeek.ui.navigation.PlaysSummaryRoute

class HomeActivity : TopLevelActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = preferences()
        val intent = if (Authenticator.isSignedIn(this)) {
            when {
                Authenticator.isOldAuth(this) -> {
                    Authenticator.signOut(this)
                    MainActivity.createIntent(this, HotnessRoute)
                }
                prefs.isCollectionSetToSync() -> MainActivity.createIntent(this, CollectionDetailsRoute)
                prefs[PREFERENCES_KEY_SYNC_PLAYS, false] == true -> MainActivity.createIntent(this, PlaysSummaryRoute)
                prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true -> MainActivity.createIntent(this, BuddiesRoute)
                else -> MainActivity.createIntent(this, HotnessRoute)
            }
        } else {
            MainActivity.createIntent(this, HotnessRoute)
        }

        startActivity(intent)
        finish()
    }
}
