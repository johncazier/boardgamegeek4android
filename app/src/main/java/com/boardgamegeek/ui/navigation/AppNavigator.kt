package com.boardgamegeek.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack

interface AppNavigator {
    val currentRoute: AppRoute?

    fun navigate(route: AppRoute)

    fun navigateTopLevel(route: AppRoute)

    fun popBackStack()
}

val LocalAppNavigator = compositionLocalOf<AppNavigator> {
    error("No AppNavigator provided")
}

class BackStackAppNavigator(
    private val backStack: NavBackStack<NavKey>,
) : AppNavigator {
    override val currentRoute: AppRoute?
        get() = backStack.lastOrNull() as? AppRoute

    override fun navigate(route: AppRoute) {
        backStack.add(route)
    }

    override fun navigateTopLevel(route: AppRoute) {
        val current = backStack.lastOrNull()
        if (current == route && backStack.size == 1) {
            return
        }
        backStack.clear()
        backStack.add(route)
    }

    override fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}
