package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.ui.buddy.BuddyLauncher

class BuddyPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = BuddyLauncher.createIntent(context, summary.toString(), null)
    }
}
