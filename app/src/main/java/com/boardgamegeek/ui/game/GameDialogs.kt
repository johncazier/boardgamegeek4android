package com.boardgamegeek.ui.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogGameRanksBinding
import com.boardgamegeek.databinding.DialogGameUsersBinding
import com.boardgamegeek.databinding.FragmentPollBinding
import com.boardgamegeek.databinding.FragmentPollSuggestedPlayerCountBinding
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.extensions.getSpannedText
import com.boardgamegeek.extensions.setViewBackground
import com.boardgamegeek.model.GameFamily
import com.boardgamegeek.model.GameLanguagePoll
import com.boardgamegeek.ui.widget.GameFamilyRow
import com.boardgamegeek.ui.widget.GameSubtypeRow
import com.boardgamegeek.ui.widget.IntegerValueFormatter
import com.boardgamegeek.ui.widget.PlayerNumberRow
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.DecimalFormat

@Composable
private fun AppDialog(
    @StringRes titleResId: Int,
    onDismiss: () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(titleResId),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    confirmButton?.invoke()
                }
            }
        }
    }
}

@Composable
private fun <T : ViewBinding> BoundAndroidView(
    modifier: Modifier = Modifier,
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> T,
    bind: (View) -> T,
    update: T.() -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            inflate(LayoutInflater.from(context), null, false).root
        },
        update = { view ->
            bind(view).update()
        },
    )
}

@Composable
fun CollectionStatusDialog(
    onDismiss: () -> Unit,
    onConfirm: (selectedStatuses: List<String>, wishlistPriority: Int) -> Unit,
) {
    val context = LocalContext.current
    val priorities = remember(context) { context.resources.getStringArray(R.array.wishlist_priority_finite) }
    val statuses = remember {
        listOf(
            "own" to R.string.collection_status_own,
            "previously_owned" to R.string.collection_status_prev_owned,
            "for_trade" to R.string.collection_status_for_trade,
            "want_to_play" to R.string.collection_status_want_to_play,
            "want" to R.string.collection_status_want_in_trade,
            "want_to_buy" to R.string.collection_status_want_to_buy,
            "preordered" to R.string.collection_status_preordered,
            "wishlist" to R.string.collection_status_wishlist,
        )
    }
    val selectedStatuses = remember { mutableStateMapOf<String, Boolean>() }
    var wishlistExpanded by remember { mutableStateOf(false) }
    var wishlistPriority by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_a_copy)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                statuses.forEach { (key, labelResId) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedStatuses[key] == true,
                            onCheckedChange = { checked -> selectedStatuses[key] = checked },
                        )
                        Text(
                            text = stringResource(labelResId),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (selectedStatuses["wishlist"] == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.collection_sort_wishlist_priority),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { wishlistExpanded = true }) {
                            Text(priorities[wishlistPriority - 1])
                        }
                        DropdownMenu(
                            expanded = wishlistExpanded,
                            onDismissRequest = { wishlistExpanded = false },
                        ) {
                            priorities.forEachIndexed { index, label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        wishlistPriority = index + 1
                                        wishlistExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    selectedStatuses.filterValues { it }.keys.toList(),
                    if (selectedStatuses["wishlist"] == true) wishlistPriority else 0,
                )
                onDismiss()
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun GameUsersDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
) {
    val game by viewModel.game.collectAsStateWithLifecycle()

    AppDialog(
        titleResId = R.string.title_users,
        onDismiss = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        BoundAndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            inflate = DialogGameUsersBinding::inflate,
            bind = DialogGameUsersBinding::bind,
        ) {
            game?.let { currentGame ->
                listOf(numberOwningBar, numberTradingBar, numberWantingBar, numberWishingBar)
                    .forEach { bar -> bar.colorize(currentGame.darkColor) }
                val maxUsers = currentGame.maxUsers.toDouble()
                numberOwningBar.setBar(R.string.owning_meter_text, currentGame.numberOfUsersOwned.toDouble(), maxUsers)
                numberTradingBar.setBar(R.string.trading_meter_text, currentGame.numberOfUsersTrading.toDouble(), maxUsers)
                numberWantingBar.setBar(R.string.wanting_meter_text, currentGame.numberOfUsersWanting.toDouble(), maxUsers)
                numberWishingBar.setBar(R.string.wishing_meter_text, currentGame.numberOfUsersWishListing.toDouble(), maxUsers)
            }
        }
    }
}

@Composable
fun GameRanksDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val game by viewModel.game.collectAsStateWithLifecycle()
    val subtypes by viewModel.subtypes.collectAsStateWithLifecycle()
    val families by viewModel.families.collectAsStateWithLifecycle()

    AppDialog(
        titleResId = R.string.title_ranks_ratings,
        onDismiss = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        BoundAndroidView(
            modifier = Modifier.fillMaxWidth(),
            inflate = DialogGameRanksBinding::inflate,
            bind = DialogGameRanksBinding::bind,
        ) {
            val voteCount = game?.numberOfRatings ?: 0
            val standardDeviation = game?.standardDeviation ?: 0.0
            votesView.text = context.getQuantityText(R.plurals.ratings_suffix, voteCount, voteCount)
            standardDeviationView.text = context.getSpannedText(R.string.standard_deviation_prefix, standardDeviation)
            standardDeviationView.isVisible = voteCount > 0

            subtypesView.removeAllViews()
            subtypesView.isVisible = false
            unRankedView.isVisible = false
            var hasRankedSubtype = false
            var unRankedSubtype: CharSequence = context.getText(R.string.game)
            subtypes.forEach { subtype ->
                if (subtype.isRankValid()) {
                    subtypesView.addView(GameSubtypeRow(context, subtype))
                    subtypesView.isVisible = true
                    hasRankedSubtype = true
                } else {
                    unRankedSubtype = subtype.describeType(context)
                }
            }
            if (!hasRankedSubtype && unRankedSubtype.isNotEmpty()) {
                unRankedView.text = context.getSpannedText(R.string.unranked_prefix, unRankedSubtype)
                unRankedView.isVisible = true
            }

            familiesView.removeAllViews()
            familiesView.isVisible = false
            families.filter(GameFamily::isRankValid).forEach { family ->
                familiesView.addView(GameFamilyRow(context, family))
                familiesView.isVisible = true
            }
        }
    }
}

@Composable
fun GameSuggestedPlayerCountDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val game by viewModel.game.collectAsStateWithLifecycle()
    val playerPoll by viewModel.playerPoll.collectAsStateWithLifecycle()
    val totalVoteCount = game?.suggestedPlayerCountPollVoteTotal ?: 0

    AppDialog(
        titleResId = R.string.suggested_numplayers,
        onDismiss = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        BoundAndroidView(
            modifier = Modifier.fillMaxWidth(),
            inflate = FragmentPollSuggestedPlayerCountBinding::inflate,
            bind = FragmentPollSuggestedPlayerCountBinding::bind,
        ) {
            progressView.hide()
            scrollView.isVisible = true
            totalVoteView.text = context.resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)
            pollList.isVisible = totalVoteCount > 0
            keyContainer.isVisible = totalVoteCount > 0
            noVotesSwitch.isVisible = totalVoteCount > 0

            if (keyContainer.childCount == 0) {
                addPollKeyRow(context, keyContainer, R.color.best, R.string.best)
                addPollKeyRow(context, keyContainer, R.color.recommended, R.string.recommended)
                addPollKeyRow(context, keyContainer, R.color.not_recommended, R.string.not_recommended)
            }

            pollList.removeAllViews()
            playerPoll.forEach { (_, playerCount, bestVoteCount, recommendedVoteCount, notRecommendedVoteCount) ->
                val row = PlayerNumberRow(context).apply {
                    setText(playerCount)
                    setVotes(bestVoteCount, recommendedVoteCount, notRecommendedVoteCount, totalVoteCount)
                    setOnClickListener { clickedView ->
                        pollList.children.forEach { existing ->
                            (existing as? PlayerNumberRow)?.clearHighlight()
                        }
                        (clickedView as? PlayerNumberRow)?.let { selectedRow ->
                            selectedRow.setHighlight()
                            keyContainer.children.forEachIndexed { index, keyRow ->
                                keyRow.findViewById<TextView>(R.id.infoView).text = selectedRow.votes[index].toString()
                            }
                        }
                    }
                }
                pollList.addView(row)
            }

            noVotesSwitch.setOnClickListener {
                pollList.children.forEach { row ->
                    (row as? PlayerNumberRow)?.showNoVotes(noVotesSwitch.isChecked)
                }
            }
        }
    }
}

@Composable
fun GameAgePollDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
) {
    val poll by viewModel.agePoll.collectAsStateWithLifecycle()
    PollChartDialog(
        titleResId = R.string.suggested_playerage,
        onDismiss = onDismiss,
    ) { binding ->
        poll?.let { agePoll ->
            val totalVoteCount = agePoll.totalVotes
            if (totalVoteCount > 0) {
                val entries = agePoll.results.map { PieEntry(it.numberOfVotes.toFloat(), it.value) }
                val dataSet = PieDataSet(entries, "").apply {
                    valueFormatter = IntegerValueFormatter(true)
                    setColors(*BggColors.twelveStageColors.toIntArray())
                }
                binding.chartView.data = PieData(dataSet)
                binding.chartView.centerText = binding.root.resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)
                binding.chartView.animateY(1000, Easing.EaseOutCubic)
            }
            binding.progressView.hide()
            binding.scrollView.isVisible = true
        }
    }
}

@Composable
fun GameLanguagePollDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val poll by viewModel.languagePoll.collectAsStateWithLifecycle()
    PollChartDialog(
        titleResId = R.string.language_dependence,
        onDismiss = onDismiss,
    ) { binding ->
        poll?.let { languagePoll ->
            val totalVoteCount = languagePoll.totalVotes
            if (totalVoteCount > 0) {
                val entries = languagePoll.results.sortedBy { it.level }.mapNotNull {
                    val labelResId = when (it.level) {
                        GameLanguagePoll.Level.NONE -> R.string.language_dependence_level_1
                        GameLanguagePoll.Level.SOME -> R.string.language_dependence_level_2
                        GameLanguagePoll.Level.MODERATE -> R.string.language_dependence_level_3
                        GameLanguagePoll.Level.EXTENSIVE -> R.string.language_dependence_level_4
                        GameLanguagePoll.Level.UNPLAYABLE -> R.string.language_dependence_level_5
                        null -> null
                    }
                    labelResId?.let { resId -> PieEntry(it.numberOfVotes.toFloat(), context.getString(resId)) }
                }
                val dataSet = PieDataSet(entries, "").apply {
                    valueFormatter = IntegerValueFormatter(true)
                    setColors(*BggColors.fiveStageColors.toIntArray())
                }
                binding.chartView.data = PieData(dataSet)
                binding.chartView.centerText = binding.root.resources.getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount)
                binding.chartView.animateY(1000, Easing.EaseOutCubic)
            }
            binding.progressView.hide()
            binding.scrollView.isVisible = true
        }
    }
}

@Composable
private fun PollChartDialog(
    @StringRes titleResId: Int,
    onDismiss: () -> Unit,
    updateChart: (FragmentPollBinding) -> Unit,
) {
    val context = LocalContext.current
    AppDialog(
        titleResId = titleResId,
        onDismiss = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        BoundAndroidView(
            modifier = Modifier.fillMaxWidth(),
            inflate = FragmentPollBinding::inflate,
            bind = FragmentPollBinding::bind,
        ) {
            chartView.apply {
                setDrawEntryLabels(false)
                isRotationEnabled = false
                legend.horizontalAlignment = LegendHorizontalAlignment.LEFT
                legend.verticalAlignment = LegendVerticalAlignment.BOTTOM
                legend.isWordWrapEnabled = true
                description = null
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, h: Highlight) {
                        val entry = e as? PieEntry ?: return
                        val message = resources.getQuantityString(
                            R.plurals.pie_chart_click_description,
                            entry.y.toInt(),
                            DecimalFormat("#0").format(entry.y),
                            entry.label,
                        )
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onNothingSelected() = Unit
                })
                updateLayoutParams {
                    width = (resources.displayMetrics.widthPixels * .8).toInt()
                }
            }
            updateChart(this)
        }
    }
}

private fun addPollKeyRow(
    context: android.content.Context,
    container: ViewGroup,
    @ColorRes colorResId: Int,
    @StringRes textResId: Int,
) {
    val row = LayoutInflater.from(context).inflate(R.layout.row_poll_key, container, false) as ViewGroup
    row.findViewById<TextView>(R.id.textView).setText(textResId)
    row.findViewById<View>(R.id.colorView).setViewBackground(ContextCompat.getColor(context, colorResId))
    container.addView(row)
}
