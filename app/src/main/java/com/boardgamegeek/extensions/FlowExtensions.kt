package com.boardgamegeek.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Converts a Flow to a StateFlow, sharing the upstream flow while there are active subscribers,
 * keeping the upstream flow active for 5000ms after the last subscriber disappears.
 *
 * @param scope The CoroutineScope in which sharing is started. Typically a viewModelScope.
 * @param initialValue The initial value of the StateFlow.
 */
fun <T> Flow<T>.stateInWhileSubscribed(scope: CoroutineScope, initialValue: T): StateFlow<T> {
    return this.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000L), // Explicitly Long
        initialValue = initialValue
    )
}