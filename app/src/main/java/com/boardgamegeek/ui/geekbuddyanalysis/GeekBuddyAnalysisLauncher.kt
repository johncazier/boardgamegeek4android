package com.boardgamegeek.ui.geekbuddyanalysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.getTextColor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.toColor
import com.boardgamegeek.model.GeekBuddyAnalysis
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.navigation.BuddyRoute
import com.boardgamegeek.ui.navigation.GeekBuddyAnalysisRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.popBackStackOrFinish

@OptIn(ExperimentalMaterialApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GeekBuddyAnalysisRouteScreen(
    route: GeekBuddyAnalysisRoute,
    viewModel: GeekBuddyAnalysisViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val resource by viewModel.analysis.collectAsStateWithLifecycle()
    val refreshing = resource?.status == Status.REFRESHING
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = { viewModel.load(route.gameId) })

    LaunchedEffect(route.gameId) { viewModel.load(route.gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.link_geekbuddy_analysis))
                        if (route.gameName.isNotBlank()) {
                            Text(
                                text = route.gameName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStackOrFinish(context) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                },
                actions = {
                    IconButton(onClick = { context.linkToBgg("geekbuddy/analyze/thing", route.gameId) }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = stringResource(R.string.menu_view_in_browser))
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState),
        ) {
            when {
                resource == null || refreshing && resource?.data == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                resource?.status == Status.ERROR && resource?.data == null -> {
                    AnalysisMessage(resource?.message.orEmpty(), Modifier.align(Alignment.Center))
                }
                resource?.data?.sections.isNullOrEmpty() -> {
                    AnalysisMessage(
                        resource?.data?.message.orEmpty().ifBlank { stringResource(R.string.empty_geekbuddy_analysis) },
                        Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    AnalysisList(
                        analysis = resource?.data!!,
                        onBuddyClick = { navigator.navigate(BuddyRoute(username = it)) },
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun AnalysisList(
    analysis: GeekBuddyAnalysis,
    onBuddyClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        analysis.sections.forEachIndexed { sectionIndex, section ->
            item(key = "header-$sectionIndex-${section.title}") {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            itemsIndexed(
                items = section.buddies,
                key = { buddyIndex, buddy -> "$sectionIndex-$buddyIndex-${section.title}-${buddy.username}" },
            ) { _, buddy ->
                val rating = buddy.details
                    .firstOrNull { it.label.equals("Rating", ignoreCase = true) }
                    ?.value
                    ?.toDoubleOrNull()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBuddyClick(buddy.username) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = buddy.username,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        rating?.let {
                            val ratingColorInt = it.toColor(BggColors.ratingColors)
                            val contentColor = if (ratingColorInt == android.graphics.Color.TRANSPARENT) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color(ratingColorInt.getTextColor())
                            }
                            Text(
                                text = buddy.details.first { detail -> detail.label.equals("Rating", ignoreCase = true) }.value,
                                color = contentColor,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .background(Color(ratingColorInt), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    buddy.details
                        .filterNot { it.label.equals("Rating", ignoreCase = true) }
                        .forEach { detail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (detail.label.isNotBlank()) {
                                    Text(
                                        text = detail.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.35f),
                                    )
                                }
                                Text(
                                    text = detail.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.65f),
                                )
                            }
                        }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AnalysisMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.padding(24.dp),
    )
}
