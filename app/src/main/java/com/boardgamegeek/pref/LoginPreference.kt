package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.LoginRoute
import com.boardgamegeek.ui.login.LoginLauncher

class LoginPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var username = context.preferences()[AccountPreferences.KEY_USERNAME, ""]

    init {
        intent = MainActivity.createIntent(getContext(), LoginRoute())
    }

    override fun isEnabled(): Boolean {
        return !Authenticator.isSignedIn(context)
    }

    override fun getSummary(): CharSequence? {
        return username
    }

    fun update(username: String) {
        this.username = username
        notifyChanged()
    }
}
