package com.boardgamegeek.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.model.PlayPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

sealed interface AppRouteResult

data class LogPlayerRouteResult(
    val position: Int,
    val player: PlayPlayer?,
) : AppRouteResult

interface RouteResultCoordinator {
    fun deliver(requestId: String, result: AppRouteResult)

    fun observe(requestId: String): Flow<AppRouteResult?>

    fun clear(requestId: String)
}

val LocalRouteResultCoordinator = compositionLocalOf<RouteResultCoordinator> {
    error("No RouteResultCoordinator provided")
}

@Composable
fun rememberRouteResultCoordinator(
    coordinatorViewModel: RouteResultCoordinatorViewModel = viewModel(),
): RouteResultCoordinator {
    return remember(coordinatorViewModel) {
        ViewModelRouteResultCoordinator(coordinatorViewModel)
    }
}

class RouteResultCoordinatorViewModel : ViewModel() {
    private val results = MutableStateFlow<Map<String, AppRouteResult>>(emptyMap())

    fun deliver(requestId: String, result: AppRouteResult) {
        results.update { it + (requestId to result) }
    }

    fun observe(requestId: String): Flow<AppRouteResult?> {
        return results.map { it[requestId] }.distinctUntilChanged()
    }

    fun clear(requestId: String) {
        results.update { it - requestId }
    }
}

private class ViewModelRouteResultCoordinator(
    private val viewModel: RouteResultCoordinatorViewModel,
) : RouteResultCoordinator {
    override fun deliver(requestId: String, result: AppRouteResult) {
        viewModel.deliver(requestId, result)
    }

    override fun observe(requestId: String): Flow<AppRouteResult?> {
        return viewModel.observe(requestId)
    }

    override fun clear(requestId: String) {
        viewModel.clear(requestId)
    }
}
