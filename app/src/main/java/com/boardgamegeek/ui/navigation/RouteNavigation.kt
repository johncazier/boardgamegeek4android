package com.boardgamegeek.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun AppNavigator.popBackStackOrFinish(context: Context) {
    if (canPopBackStack) {
        popBackStack()
    } else {
        context.findActivity()?.finish()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
