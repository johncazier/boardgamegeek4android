package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.content.Intent

inline fun <reified T : Activity> Context.startActivity(noinline init: (Intent.() -> Unit)? = null) {
    val intent = Intent(this, T::class.java)
    if (init != null) {
        intent.init()
    }
    startActivity(intent)
}
