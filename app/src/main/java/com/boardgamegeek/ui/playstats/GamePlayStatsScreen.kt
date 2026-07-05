package com.boardgamegeek.ui.playstats

import android.graphics.Color
import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boardgamegeek.R
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.PlayStatPrefs
import com.boardgamegeek.extensions.asPercentage
import com.boardgamegeek.extensions.asTime
import com.boardgamegeek.extensions.cdf
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.ui.widget.ScoreGraphView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.animation.Easing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GamePlayStatsScreen(
    viewModel: GamePlayStatsViewModel,
    gameId: Int,
    @ColorInt headerColor: Int,
    contentPadding: PaddingValues,
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.preferences() }

    val collectionItems by viewModel.collectionItems.collectAsStateWithLifecycle()
    val plays by viewModel.plays.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        viewModel.setGameId(gameId)
    }

    val publishedPlayingTime = collectionItems?.firstOrNull()?.playingTime ?: 0
    val personalRating = collectionItems
        ?.filter { item -> item.rating > 0.0 }
        ?.map { item -> item.rating }
        ?.average() ?: Game.UNRATED
    val isGameOwned = collectionItems?.any { item -> item.own } == true
    val modifiedWhitmoreScore = collectionItems
        ?.filter { item -> item.rating > 0.0 }
        ?.map { item -> item.modifiedWhitmoreScore }
        ?.average() ?: 0.0

    val isDarkTheme = isSystemInDarkTheme()
    val playCountColors = remember(collectionItems, isDarkTheme) {
        val fallback = if (isDarkTheme) {
            intArrayOf(
                ContextCompat.getColor(ctx, R.color.orange),
                ContextCompat.getColor(ctx, R.color.medium_blue),
                ContextCompat.getColor(ctx, R.color.light_blue),
            )
        } else {
            intArrayOf(
                ContextCompat.getColor(ctx, R.color.orange),
                ContextCompat.getColor(ctx, R.color.dark_blue),
                ContextCompat.getColor(ctx, R.color.light_blue),
            )
        }
        collectionItems?.firstOrNull()?.let { item ->
            intArrayOf(
                sanitizeChartColor(item.winsColor, fallback[0], isDarkTheme),
                sanitizeChartColor(item.winnablePlaysColor, fallback[1], isDarkTheme),
                sanitizeChartColor(item.allPlaysColor, fallback[2], isDarkTheme),
            )
        } ?: fallback
    }

    val includeIncomplete = remember {
        prefs[PlayStatPrefs.LOG_PLAY_STATS_INCOMPLETE, false] ?: false
    }

    val hIndex = remember {
        prefs[PlayStatPrefs.KEY_GAME_H_INDEX, 0] ?: 0
    }

    val statsState by produceState<Stats?>(
        initialValue = null,
        plays,
        players,
        publishedPlayingTime,
        personalRating,
        modifiedWhitmoreScore,
        includeIncomplete,
        hIndex
    ) {
        val playsValue = plays
        val playersValue = players
        if (playsValue == null || playersValue == null || playsValue.isEmpty()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            Stats(
                playsValue.filter { play -> includeIncomplete || !play.incomplete },
                playersValue,
                publishedPlayingTime,
                personalRating,
                hIndex,
                modifiedWhitmoreScore,
            ).apply { calculate() }
        }
    }

    val stats = statsState

    val playsValue = plays
    val playersValue = players

    when {
        playsValue == null || playersValue == null -> {
            LoadingContent(contentPadding)
        }
        playsValue.isEmpty() -> {
            EmptyContent(contentPadding)
        }
        stats == null -> {
            LoadingContent(contentPadding)
        }
        else -> {
            StatsContent(
                stats = stats,
                headerColor = headerColor,
                contentPadding = contentPadding,
                playCountColors = playCountColors,
                username = prefs[AccountPreferences.KEY_USERNAME, ""].orEmpty(),
                publishedPlayingTime = publishedPlayingTime,
                personalRating = personalRating,
                isGameOwned = isGameOwned,
            )
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.empty_play_stats))
    }
}

@Composable
private fun StatsContent(
    stats: Stats,
    @ColorInt headerColor: Int,
    contentPadding: PaddingValues,
    playCountColors: IntArray,
    username: String,
    publishedPlayingTime: Int,
    personalRating: Double,
    isGameOwned: Boolean,
) {
    val headerTint = if (headerColor != Color.TRANSPARENT) ComposeColor(headerColor) else ComposeColor.Unspecified

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            vertical = dimensionResource(R.dimen.padding_standard),
            horizontal = dimensionResource(R.dimen.padding_standard)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_standard))
    ) {
        item {
            PlayCountCard(
                stats = stats,
                headerTint = headerTint,
                playCountColors = playCountColors,
                username = username
            )
        }

        if (stats.hasScores()) {
            item {
                ScoresCard(
                    stats = stats,
                    headerTint = headerTint
                )
            }
        }

        if (stats.getPlayerStats().isNotEmpty()) {
            item {
                PlayersCard(
                    stats = stats,
                    headerTint = headerTint
                )
            }
        }

        item {
            DatesCard(stats = stats, headerTint = headerTint)
        }

        item {
            TimeCard(stats = stats, headerTint = headerTint, publishedPlayingTime = publishedPlayingTime)
        }

        if (stats.playsPerLocation.isNotEmpty()) {
            item {
                LocationsCard(stats = stats, headerTint = headerTint)
            }
        }

        item {
            AdvancedCard(
                stats = stats,
                headerTint = headerTint,
                personalRating = personalRating,
                isGameOwned = isGameOwned
            )
        }
    }
}

@Composable
private fun PlayCountCard(
    stats: Stats,
    headerTint: ComposeColor,
    playCountColors: IntArray,
    username: String,
) {
    CardSection(
        titleRes = R.string.title_play_count,
        headerTint = headerTint
    ) {
        val playCount = stats.playCountSince()
        val playCountIncomplete = stats.playCountSince(includeIncomplete = true) - playCount

        val coinLabel = when {
            stats.getDateForPlayNumber(100).isNotEmpty() -> stringResource(R.string.play_stat_dollar)
            stats.getDateForPlayNumber(50).isNotEmpty() -> stringResource(R.string.play_stat_half_dollar)
            stats.getDateForPlayNumber(25).isNotEmpty() -> stringResource(R.string.play_stat_quarter)
            stats.getDateForPlayNumber(10).isNotEmpty() -> stringResource(R.string.play_stat_dime)
            stats.getDateForPlayNumber(5).isNotEmpty() -> stringResource(R.string.play_stat_nickel)
            else -> ""
        }

        if (coinLabel.isNotBlank()) {
            StatRow(label = "", value = coinLabel)
        }

        StatRow(labelRes = R.string.play_stat_play_count, value = playCount.toString())
        if (playCountIncomplete > 0) {
            StatRow(labelRes = R.string.play_stat_play_count_incomplete, value = playCountIncomplete.toString())
        }
        StatRow(labelRes = R.string.play_stat_months_played, value = stats.getMonthsPlayed().toString())
        if (stats.playsPerMonth > 0.0) {
            StatRow(labelRes = R.string.play_stat_play_rate, value = DOUBLE_FORMAT.format(stats.playsPerMonth))
        }

        if (username.isNotBlank()) {
            val playCountValues = buildPlayCountValues(stats, username)
            if (playCountValues.isNotEmpty()) {
                val labelColor = resolveTint(headerTint, MaterialTheme.colorScheme.onSurface).toArgb()
                PlayCountChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = dimensionResource(R.dimen.padding_extra)),
                    playCountValues = playCountValues,
                    playCountColors = playCountColors,
                    labelColor = labelColor,
                )
            }
        }
    }
}

private fun buildPlayCountValues(stats: Stats, username: String): List<BarEntry> {
    if (stats.minPlayerCount <= 0 || stats.maxPlayerCount <= 0) return emptyList()
    val userStats = stats.getPlayerStats(username)
    val values = ArrayList<BarEntry>()
    for (i in stats.minPlayerCount..stats.maxPlayerCount) {
        val winnablePlayCount = userStats?.getWinnablePlayCountByPlayerCount(i) ?: 0
        val wins = userStats?.getWinCountByPlayerCount(i) ?: 0
        val playCountPerPlayer = stats.getPlayCountByPlayerCount(i)
        values.add(
            BarEntry(
                i.toFloat(), floatArrayOf(
                    wins.toFloat(),
                    winnablePlayCount - wins.toFloat(),
                    playCountPerPlayer - winnablePlayCount.toFloat()
                )
            )
        )
    }
    return values
}

@Composable
private fun PlayCountChart(
    modifier: Modifier,
    playCountValues: List<BarEntry>,
    playCountColors: IntArray,
    labelColor: Int,
) {
    val titlePlays = stringResource(R.string.title_plays)
    val titleWins = stringResource(R.string.title_wins)
    val titleWinnable = stringResource(R.string.winnable)
    val titleAll = stringResource(R.string.all)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            HorizontalBarChart(context).apply {
                description = null
                setDrawGridBackground(false)
                axisLeft.isEnabled = false
                axisRight.granularity = 1.0f
                xAxis.granularity = 1.0f
                xAxis.setDrawGridLines(false)
                axisRight.textColor = labelColor
                xAxis.textColor = labelColor
                legend.textColor = labelColor
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { chart ->
            val playCountDataSet = BarDataSet(playCountValues, titlePlays).apply {
                setDrawValues(false)
                isHighlightEnabled = false
                setColors(*playCountColors)
                stackLabels = arrayOf(titleWins, titleWinnable, titleAll)
            }
            val dataSets = mutableListOf<IBarDataSet>()
            dataSets.add(playCountDataSet)
            chart.data = BarData(dataSets)
            chart.animateY(1000, Easing.EaseInOutBack)
            chart.invalidate()
        }
    )
}

@Composable
private fun ScoresCard(
    stats: Stats,
    headerTint: ComposeColor,
) {
    var showLowScorers by remember { mutableStateOf(false) }
    var showHighScorers by remember { mutableStateOf(false) }
    var showScoreHelp by remember { mutableStateOf(false) }

    CardSection(
        titleRes = R.string.title_scores,
        headerTint = headerTint,
        headerTrailing = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_help_outline_18),
                contentDescription = stringResource(R.string.help_title),
                tint = resolveTint(headerTint, MaterialTheme.colorScheme.onSurface)
            )
        },
        onHeaderTrailingClick = { showScoreHelp = true }
    ) {
        ScoreSummaryRow(
            low = SCORE_FORMAT.format(stats.lowScore),
            average = SCORE_FORMAT.format(stats.averageScore),
            averageWin = SCORE_FORMAT.format(stats.averageWinningScore),
            high = SCORE_FORMAT.format(stats.highScore),
            onLowClick = { showLowScorers = true },
            onHighClick = { showHighScorers = true }
        )

        if (stats.highScore != INVALID_SCORE && stats.lowScore != INVALID_SCORE && stats.highScore > stats.lowScore) {
            ScoreGraph(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(top = dimensionResource(R.dimen.padding_half)),
                lowScore = stats.lowScore,
                averageScore = stats.averageScore,
                averageWinScore = stats.averageWinningScore,
                highScore = stats.highScore,
                personalLow = null,
                personalAverage = null,
                personalAverageWin = null,
                personalHigh = null,
            )
        }
    }

    if (showScoreHelp) {
        ScoreHelpDialog(onDismiss = { showScoreHelp = false })
    }

    if (showLowScorers) {
        SimpleMessageDialog(
            titleRes = R.string.title_low_scorers,
            message = stats.lowScorers,
            onDismiss = { showLowScorers = false }
        )
    }

    if (showHighScorers) {
        SimpleMessageDialog(
            titleRes = R.string.title_high_scorers,
            message = stats.highScorers,
            onDismiss = { showHighScorers = false }
        )
    }
}

@Composable
private fun PlayersCard(
    stats: Stats,
    headerTint: ComposeColor,
) {
    var showHelp by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    CardSection(
        titleRes = R.string.title_players,
        headerTint = headerTint,
        headerTrailing = {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_help_outline_18),
                contentDescription = stringResource(R.string.help_title),
                tint = resolveTint(headerTint, MaterialTheme.colorScheme.onSurface)
            )
        },
        onHeaderTrailingClick = { showHelp = true }
    ) {
        PlayerHeaderRow()
        Divider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_half)))

        stats.getPlayerStats().forEach { playerStat ->
            key(playerStat.id) {
                val isExpanded = expanded[playerStat.id] ?: false
                PlayerRow(
                    playerStat = playerStat,
                    overallStats = stats,
                    expanded = isExpanded,
                    showScores = stats.hasScores(),
                    onToggle = {
                        expanded[playerStat.id] = !isExpanded
                    }
                )
                Divider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_half)))
            }
        }
    }

    if (showHelp) {
        SimpleMessageDialog(
            titleRes = R.string.title_players_skill,
            messageRes = R.string.player_skill_info,
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun PlayerHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_half)),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(R.string.title_player),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(3f)
        )
        Text(
            text = stringResource(R.string.title_wins),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.title_win_skill),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerRow(
    playerStat: PlayerStats,
    overallStats: Stats,
    expanded: Boolean,
    showScores: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(enabled = showScores, onClick = onToggle)
            .padding(vertical = dimensionResource(R.dimen.padding_half))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playerStat.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(3f)
            )
            Text(
                text = stringResource(
                    R.string.play_stat_win_percentage,
                    playerStat.numberOfPlaysWon,
                    playerStat.numberOfWinnablePlays,
                    playerStat.winPercentage
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playerStat.winSkill.toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(visible = expanded && showScores) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.padding_standard))
            ) {
                Text(
                    text = stringResource(R.string.title_scores),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_half))
                )
                ScoreSummaryRow(
                    low = playerStat.lowScoreText,
                    average = playerStat.averageScoreText,
                    averageWin = playerStat.averageWinScoreText,
                    high = playerStat.highScoreText,
                    onLowClick = null,
                    onHighClick = null
                )
                ScoreGraph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(top = dimensionResource(R.dimen.padding_half)),
                    lowScore = overallStats.lowScore,
                    averageScore = overallStats.averageScore,
                    averageWinScore = overallStats.averageWinningScore,
                    highScore = overallStats.highScore,
                    personalLow = playerStat.lowScore.takeUnless { it == INVALID_SCORE },
                    personalAverage = playerStat.averageScore.takeUnless { it == INVALID_SCORE },
                    personalAverageWin = playerStat.averageWinScore.takeUnless { it == INVALID_SCORE },
                    personalHigh = playerStat.highScore.takeUnless { it == INVALID_SCORE },
                )
            }
        }
    }
}

@Composable
private fun DatesCard(
    stats: Stats,
    headerTint: ComposeColor,
) {
    val ctx = LocalContext.current
    CardSection(
        titleRes = R.string.title_dates,
        headerTint = headerTint
    ) {
        val rows = listOf(
            R.string.play_stat_first_play to stats.getDateForPlayNumber(1),
            R.string.play_stat_nickel to stats.getDateForPlayNumber(5),
            R.string.play_stat_dime to stats.getDateForPlayNumber(10),
            R.string.play_stat_quarter to stats.getDateForPlayNumber(25),
            R.string.play_stat_half_dollar to stats.getDateForPlayNumber(50),
            R.string.play_stat_dollar to stats.getDateForPlayNumber(100),
            R.string.play_stat_last_play to stats.getDateForPlayNumber(stats.playCountSince())
        )

        rows.filter { it.second.isNotEmpty() }.forEach { (labelRes, date) ->
            val formatted = formatDate(ctx, date)
            StatRow(labelRes = labelRes, value = formatted)
        }
    }
}

@Composable
private fun TimeCard(
    stats: Stats,
    headerTint: ComposeColor,
    publishedPlayingTime: Int,
) {
    CardSection(
        titleRes = R.string.title_play_time,
        headerTint = headerTint
    ) {
        StatRow(labelRes = R.string.play_stat_hours_played, value = stats.hoursPlayedSince().toInt().toString())
        val average = stats.averagePlayTime
        if (average > 0) {
            StatRow(labelRes = R.string.play_stat_average_play_time, value = average.asTime())
            if (publishedPlayingTime > 0) {
                if (average > publishedPlayingTime) {
                    StatRow(
                        labelRes = R.string.play_stat_average_play_time_slower,
                        value = (average - publishedPlayingTime).asTime()
                    )
                } else if (publishedPlayingTime > average) {
                    StatRow(
                        labelRes = R.string.play_stat_average_play_time_faster,
                        value = (publishedPlayingTime - average).asTime()
                    )
                }
            }
        }
        if (stats.averagePlayTimePerPlayer > 0) {
            StatRow(labelRes = R.string.play_stat_average_play_time_per_player, value = stats.averagePlayTimePerPlayer.asTime())
        }
    }
}

@Composable
private fun LocationsCard(
    stats: Stats,
    headerTint: ComposeColor,
) {
    CardSection(
        titleRes = R.string.title_locations,
        headerTint = headerTint
    ) {
        stats.playsPerLocation.forEach { location ->
            StatRow(label = location.key, value = location.value.toString())
        }
    }
}

@Composable
private fun AdvancedCard(
    stats: Stats,
    headerTint: ComposeColor,
    personalRating: Double,
    isGameOwned: Boolean,
) {
    CardSection(
        titleRes = R.string.title_advanced,
        headerTint = headerTint
    ) {
        if (personalRating != Game.UNRATED) {
            StatRow(
                labelRes = R.string.play_stat_fhm,
                value = stats.calculateFriendlessHappinessMetric().toString(),
                infoRes = R.string.play_stat_fhm_info
            )
            StatRow(
                labelRes = R.string.play_stat_hhm,
                value = stats.calculateHuberHappinessMetricSince().toString(),
                infoRes = R.string.play_stat_hhm_info
            )
            StatRow(
                labelRes = R.string.play_stat_huber_heat,
                value = DOUBLE_FORMAT.format(stats.calculateHuberHeat()),
                infoRes = R.string.play_stat_huber_heat_info
            )
            StatRow(
                labelRes = R.string.play_stat_zefquaavius_heat,
                value = DOUBLE_FORMAT.format(stats.calculateZefquaaviusHeat()),
                infoRes = R.string.play_stat_zefquaavius_heat_info
            )
            StatRow(
                labelRes = R.string.play_stat_ruhm,
                value = DOUBLE_FORMAT.format(stats.calculateRandyCoxNotUnhappinessMetric()),
                infoRes = R.string.play_stat_ruhm_info
            )
        }
        if (isGameOwned) {
            StatRow(
                labelRes = R.string.play_stat_utilization,
                value = stats.calculateUtilization().asPercentage(),
                infoRes = R.string.play_stat_utilization_info
            )
        }
        if (stats.playCountShortOfHIndex > 0) {
            StatRow(
                labelRes = R.string.play_stat_game_h_index_offset_out,
                value = stats.playCountShortOfHIndex.toString()
            )
        } else {
            StatRow(
                labelRes = R.string.play_stat_game_h_index_offset_in,
                value = ""
            )
        }
    }
}

@Composable
private fun CardSection(
    @StringRes titleRes: Int,
    headerTint: ComposeColor,
    headerTrailing: (@Composable () -> Unit)? = null,
    onHeaderTrailingClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_standard))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = resolveTint(headerTint, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f)
                )
                if (headerTrailing != null) {
                    if (onHeaderTrailingClick != null) {
                        Box(modifier = Modifier.clickable { onHeaderTrailingClick() }) {
                            headerTrailing()
                        }
                    } else {
                        headerTrailing()
                    }
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_half)))
            content()
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    @StringRes infoRes: Int? = null,
) {
    StatRowInternal(label = label, labelRes = null, value = value, infoRes = infoRes)
}

@Composable
private fun StatRow(
    @StringRes labelRes: Int,
    value: String,
    @StringRes infoRes: Int? = null,
) {
    StatRowInternal(label = null, labelRes = labelRes, value = value, infoRes = infoRes)
}

@Composable
private fun StatRowInternal(
    label: String?,
    @StringRes labelRes: Int?,
    value: String,
    @StringRes infoRes: Int?,
) {
    var showInfo by remember { mutableStateOf(false) }
    val labelText = label ?: if (labelRes != null) stringResource(labelRes) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_half)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_standard)))
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = infoRes != null) { showInfo = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (infoRes != null) {
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_half)))
                Icon(
                    painter = painterResource(R.drawable.ic_outline_info_24),
                    contentDescription = stringResource(R.string.information),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showInfo && infoRes != null) {
        InfoDialog(
            title = labelText,
            messageRes = infoRes,
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun ScoreSummaryRow(
    low: String,
    average: String,
    averageWin: String,
    high: String,
    onLowClick: (() -> Unit)?,
    onHighClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScoreValue(
            iconRes = R.drawable.ic_baseline_thumb_down_18,
            tintRes = R.color.score_low,
            value = low,
            onClick = onLowClick
        )
        Spacer(modifier = Modifier.weight(1f))
        ScoreValue(
            iconRes = R.drawable.ic_baseline_thumbs_up_down_18,
            tintRes = R.color.score_average,
            value = average,
            onClick = null
        )
        Spacer(modifier = Modifier.weight(1f))
        ScoreValue(
            iconRes = R.drawable.ic_baseline_star_18,
            tintRes = R.color.score_average_win,
            value = averageWin,
            onClick = null
        )
        Spacer(modifier = Modifier.weight(1f))
        ScoreValue(
            iconRes = R.drawable.ic_baseline_thumb_up_18,
            tintRes = R.color.score_high,
            value = high,
            onClick = onHighClick
        )
    }
}

@Composable
private fun ScoreValue(
    @DrawableRes iconRes: Int,
    @ColorRes tintRes: Int,
    value: String,
    onClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = dimensionResource(R.dimen.padding_half))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colorResource(tintRes),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_standard)))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScoreGraph(
    modifier: Modifier,
    lowScore: Double,
    averageScore: Double,
    averageWinScore: Double,
    highScore: Double,
    personalLow: Double?,
    personalAverage: Double?,
    personalAverageWin: Double?,
    personalHigh: Double?,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ScoreGraphView(context)
        },
        update = { view ->
            view.lowScore = lowScore
            view.averageScore = averageScore
            view.averageWinScore = averageWinScore
            view.highScore = highScore
            if (personalLow != null) view.personalLowScore = personalLow
            if (personalAverage != null) view.personalAverageScore = personalAverage
            if (personalAverageWin != null) view.personalAverageWinScore = personalAverageWin
            if (personalHigh != null) view.personalHighScore = personalHigh
            view.invalidate()
        }
    )
}

@Composable
private fun SimpleMessageDialog(
    @StringRes titleRes: Int,
    message: String? = null,
    @StringRes messageRes: Int? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.ok)) }
        },
        title = { Text(text = stringResource(titleRes)) },
        text = {
            if (message != null) {
                Text(text = message)
            } else if (messageRes != null) {
                Text(text = stringResource(messageRes))
            }
        }
    )
}

@Composable
private fun InfoDialog(
    title: String,
    @StringRes messageRes: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.ok)) }
        },
        title = { Text(text = title) },
        text = {
            val htmlText = stringResource(messageRes)
            Text(
                text = AnnotatedString.fromHtml(
                    htmlText,
                    linkStyles = TextLinkStyles(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                )
            )
        }
    )
}

@Composable
private fun ScoreHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.ok)) }
        },
        title = { Text(text = stringResource(R.string.title_scores)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_half))) {
                ScoreHelpLegendRow(
                    iconRes = R.drawable.ic_baseline_thumb_down_18,
                    labelRes = R.string.low_score,
                    tint = MaterialTheme.colorScheme.error
                )
                ScoreHelpLegendRow(
                    iconRes = R.drawable.ic_baseline_thumbs_up_down_18,
                    labelRes = R.string.average_score,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                ScoreHelpLegendRow(
                    iconRes = R.drawable.ic_baseline_star_18,
                    labelRes = R.string.average_winning_score,
                    tint = MaterialTheme.colorScheme.primary
                )
                ScoreHelpLegendRow(
                    iconRes = R.drawable.ic_baseline_thumb_up_18,
                    labelRes = R.string.high_score,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}

@Composable
private fun ScoreHelpLegendRow(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    tint: ComposeColor,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_standard)))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun resolveTint(color: ComposeColor, fallback: ComposeColor): ComposeColor {
    return if (color != ComposeColor.Unspecified) color else fallback
}

private fun Int.colorOrElse(context: android.content.Context, @ColorInt colorResId: Int): Int {
    return if (this == Color.TRANSPARENT) ContextCompat.getColor(context, colorResId) else this
}

private fun sanitizeChartColor(@ColorInt color: Int, @ColorInt fallback: Int, isDarkTheme: Boolean): Int {
    // Treat transparent/zero/black as invalid to avoid unreadable bars.
    val rgb = color and 0x00FFFFFF
    if (color == 0 || color == Color.TRANSPARENT || rgb == 0) return fallback
    return if (isDarkTheme && isTooDark(color)) fallback else color
}

private fun isTooDark(@ColorInt color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val luminance = (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
    return luminance < 60
}

private fun formatDate(context: android.content.Context, date: String): String {
    if (date.isEmpty()) return ""
    return try {
        val millis = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
        millis.formatDateTime(
            context = context,
            flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        ).toString()
    } catch (e: ParseException) {
        date
    }
}

private class Stats(
    val plays: List<Play>,
    val players: List<PlayPlayer>,
    val publishedPlayingTime: Int,
    val personalRating: Double,
    val hIndex: Int,
    val modifiedWhitmoreScore: Double,
) {
    private var playCountByPlayerCount = mapOf<Int, Int>()
    private var playCountByLocation = mapOf<String, Int>()
    private val playerStats = mutableMapOf<String, PlayerStats>()
    private val playDates = mutableListOf<String>()
    private val filteredPlayers = mutableListOf<Pair<Play, PlayPlayer>>()
    private val sortedPlays = plays.sortedBy { it.dateInMillis }

    val Play.date: String
        get() = simpleDateFormat.format(this.dateInMillis)

    val Play.yearAndMonth: String
        get() = this.date.substring(0, 7)

    fun calculate() {
        playerStats.clear()
        playDates.clear()
        filteredPlayers.clear()

        playCountByPlayerCount = plays.filter { it.playerCount > 0 }.groupingBy { it.playerCount }.fold(0) { playCount, play ->
            playCount + play.quantity
        }
        playCountByLocation = plays.filter { it.location.isNotBlank() }.groupingBy { it.location }.fold(0) { playCount, play ->
            playCount + play.quantity
        }

        val playersByPlayId = players.groupBy { it.playInternalId }

        for (play in sortedPlays) {
            val playPlayers = playersByPlayId[play.internalId].orEmpty()
            repeat(play.quantity) {
                playDates += play.date
                playPlayers.forEach { player ->
                    filteredPlayers += play to player
                }
            }
        }

        filteredPlayers.groupBy(
            keySelector = { it.second.id },
        ).forEach { (key, value) ->
            playerStats[key] = PlayerStats(value)
        }
    }

    private fun getPlaysSince(dateInMillis: Long?) = dateInMillis?.let { plays.filter { it.dateInMillis > dateInMillis } } ?: plays

    fun getDateForPlayNumber(number: Int) = playDates.getOrNull(number - 1) ?: ""

    fun getMonthsPlayed() = plays.map { it.yearAndMonth }.toSet().size

    private fun calculateFlash(): Long {
        return daysBetweenDates(sortedPlays.first().dateInMillis, sortedPlays.last().dateInMillis)
    }

    private fun calculateLag(): Long {
        return daysBetweenDates(sortedPlays.last().dateInMillis)
    }

    private fun daysBetweenDates(from: Long, to: Long = System.currentTimeMillis()): Long {
        return (to - from).milliseconds.inWholeDays.coerceAtLeast(1)
    }

    fun playCountSince(dateInMillis: Long? = null, includeIncomplete: Boolean = false): Int =
        getPlaysSince(dateInMillis).filter { includeIncomplete || !it.incomplete }.sumOf { it.quantity }

    fun hoursPlayedSince(dateInMillis: Long? = null): Double {
        val (estimatePlays, actualPlays) = getPlaysSince(dateInMillis).partition { it.length == 0 }
        return (estimatePlays.sumOf { publishedPlayingTime * it.quantity } + actualPlays.sumOf { it.length }) / 60.0
    }

    val averagePlayTime: Int
        get() = if (plays.any { it.length > 0 }) {
            val playsWithLength = plays.filter { it.length > 0 }
            playsWithLength.sumOf { it.length } / playsWithLength.sumOf { it.quantity }
        } else 0

    val averagePlayTimePerPlayer: Int
        get() = if (plays.any { it.length > 0 && it.playerCount > 0 }) {
            val filteredPlays = plays.filter { it.length > 0 && it.playerCount > 0 }
            filteredPlays.sumOf { it.length / it.playerCount } / filteredPlays.sumOf { it.quantity }
        } else 0

    val minPlayerCount: Int
        get() = playCountByPlayerCount.keys.minOrNull() ?: 0

    val maxPlayerCount: Int
        get() = playCountByPlayerCount.keys.maxOrNull() ?: 0

    fun getPlayCountByPlayerCount(playerCount: Int) = playCountByPlayerCount[playerCount] ?: 0

    fun getPlayerStats(): List<PlayerStats> {
        return playerStats.values.toList().sortedByDescending { it.numberOfPlays }
    }

    fun getPlayerStats(username: String): PlayerStats? {
        return playerStats.values.find { it.username == username }
    }

    fun hasScores() = filteredPlayers.any { it.second.numericScore != null }

    val lowScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.minOrNull() ?: INVALID_SCORE

    val highScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.maxOrNull() ?: INVALID_SCORE

    val averageScore: Double get() = filteredPlayers.mapNotNull { it.second.numericScore }.average()

    val averageWinningScore: Double get() = filteredPlayers.filter { it.second.isWin }.mapNotNull { it.second.numericScore }.average()

    val lowScorers: String
        get() = if (lowScore == INVALID_SCORE) "" else
            filteredPlayers.filter { it.second.numericScore == lowScore }.map { it.second.description }.formatList()

    val highScorers: String
        get() = if (highScore == INVALID_SCORE) "" else
            filteredPlayers.filter { it.second.numericScore == highScore }.map { it.second.description }.formatList()

    val playsPerLocation: List<Map.Entry<String, Int>>
        get() = playCountByLocation.entries.toList().sortedBy { it.key }.sortedByDescending { it.value }

    val playsPerMonth: Double
        get() = (((playCountSince() * 365.25) / calculateFlash()) / 12).coerceAtMost(playCountSince().toDouble())

    fun calculateUtilization(): Double {
        return playCountSince().toDouble().cdf(lambda)
    }

    fun calculateFriendlessHappinessMetric(): Int {
        if (personalRating == Game.UNRATED) return 0
        return ((personalRating * 5) + playCountSince() + (4 * getMonthsPlayed()) + hoursPlayedSince()).toInt()
    }

    fun calculateHuberHappinessMetricSince(dateInMillis: Long? = null): Int {
        if (personalRating == Game.UNRATED) return 0
        return ((personalRating - 4.5) * hoursPlayedSince(dateInMillis)).toInt()
    }

    fun calculateRandyCoxNotUnhappinessMetric(): Double {
        if (personalRating == Game.UNRATED) return 0.0
        val raw = ((calculateFlash().toDouble()) / calculateLag()) * getMonthsPlayed() * personalRating
        return if (raw < 1.0) 0.0 else ln(raw)
    }

    val playCountShortOfHIndex: Int by lazy {
        (hIndex - playCountSince()).coerceAtLeast(0)
    }

    fun calculateHuberHeat(): Double {
        val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        return calculateGrayHotness(lastYear)
    }

    fun calculateZefquaaviusHeat(): Double {
        val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        return calculateZefquaaviusHotness(lastYear)
    }

    fun calculateZefquaaviusHotness(sinceDateInMillis: Long): Double {
        return calculateGrayHotness(sinceDateInMillis) * modifiedWhitmoreScore
    }

    fun calculateGrayHotness(sinceDateInMillis: Long): Double {
        val intervalPlayCount = playCountSince(sinceDateInMillis)
        val s = 1 + (intervalPlayCount.toDouble() / playCountSince())
        return s * s * sqrt(intervalPlayCount.toDouble()) * calculateHuberHappinessMetricSince(sinceDateInMillis)
    }

    companion object {
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val lambda = ln(0.1) / -10
    }
}

private class PlayerStats(val plays: List<Pair<Play, PlayPlayer>>) {
    val Play.isWinnable: Boolean
        get() {
            return when {
                noWinStats -> false
                playerCount == 0 -> false
                deleteTimestamp > 0L -> false
                updateTimestamp > 0L -> true
                else -> playId > 0
            }
        }

    val id: String
        get() = plays.first().second.id

    val username: String
        get() = plays.first().second.username

    val description: String
        get() = plays.first().second.description

    val numberOfPlays: Int
        get() = plays.sumOf { it.first.quantity }

    val numberOfWinnablePlays: Int
        get() = plays.filter { it.first.isWinnable }.sumOf { it.first.quantity }

    val numberOfPlaysWon: Int
        get() = plays.filter { it.second.isWin }.sumOf { it.first.quantity }

    val lowScore: Double
        get() = plays.mapNotNull { it.second.numericScore }.minOrNull() ?: INVALID_SCORE

    val highScore: Double
        get() = plays.mapNotNull { it.second.numericScore }.maxOrNull() ?: INVALID_SCORE

    val averageScore: Double
        get() = plays.mapNotNull { it.second.numericScore }.let {
            if (it.isEmpty()) INVALID_SCORE else it.average()
        }

    val averageWinScore: Double
        get() = plays.filter { it.second.isWin }.mapNotNull { it.second.numericScore }.let {
            if (it.isEmpty()) INVALID_SCORE else it.average()
        }

    val lowScoreText: String
        get() = if (lowScore == INVALID_SCORE) "-" else DOUBLE_FORMAT.format(lowScore)

    val highScoreText: String
        get() = if (highScore == INVALID_SCORE) "-" else DOUBLE_FORMAT.format(highScore)

    val averageScoreText: String
        get() = if (averageScore == INVALID_SCORE) "-" else DOUBLE_FORMAT.format(averageScore)

    val averageWinScoreText: String
        get() = if (averageWinScore == INVALID_SCORE) "-" else DOUBLE_FORMAT.format(averageWinScore)

    fun getWinCountByPlayerCount(playerCount: Int): Int {
        return plays.filter { it.second.isWin && it.first.playerCount == playerCount }.sumOf { it.first.quantity }
    }

    fun getWinnablePlayCountByPlayerCount(playerCount: Int): Int {
        return plays.filter { it.first.isWinnable && it.first.playerCount == playerCount }.sumOf { it.first.quantity }
    }

    private val rawWinSkill: Double
        get() = when (numberOfWinnablePlays) {
            0 -> 0.0
            else -> plays.filter { it.second.isWin }.sumOf { it.first.quantity * it.first.playerCount }
                .toDouble() / numberOfWinnablePlays * 100
        }

    val winSkill: Int
        get() = when (numberOfWinnablePlays) {
            0 -> 0
            else -> ((rawWinSkill * numberOfWinnablePlays + PRIOR_WIN_SKILL * PRIOR_WIN_SKILL_PLAY_COUNT) /
                (numberOfWinnablePlays + PRIOR_WIN_SKILL_PLAY_COUNT)).roundToInt()
        }

    val winPercentage: Int
        get() = when {
            numberOfWinnablePlays <= 0 -> 0
            numberOfPlaysWon >= numberOfWinnablePlays -> 100
            else -> (numberOfPlaysWon.toDouble() / numberOfWinnablePlays * 100).toInt()
        }
}

private val SCORE_FORMAT = DecimalFormat("0.##")
private val DOUBLE_FORMAT = DecimalFormat("0.00")
private const val INVALID_SCORE = Int.MIN_VALUE.toDouble()
private const val PRIOR_WIN_SKILL = 100.0
private const val PRIOR_WIN_SKILL_PLAY_COUNT = 10
