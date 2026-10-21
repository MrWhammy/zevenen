package com.zevenen.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zevenen.app.game.GameState

/** Header shown on all in-game screens: round number, cards dealt and dealer. */
@Composable
fun RoundHeader(state: GameState, onStandings: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Round ${state.currentRound} of ${state.rules.totalRounds}",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "${state.currentRecord.cards} cards  •  Dealer: ${state.players[state.dealer]}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Standings:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val totals = state.totalScores
            val summary = state.players.indices
                .sortedByDescending { totals[it] }
                .joinToString("   ") { "${state.players[it]} ${totals[it]}" }
            Text(summary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            TextButton(onClick = onStandings) { Text("Scoreboard") }
        }
    }
}

/** Prediction phase: one player at a time predicts their number of tricks. */
@Composable
fun PredictScreen(state: GameState, onPredict: (Int) -> Unit, onShowScoreboard: () -> Unit) {
    val record = state.currentRecord
    val player = state.predictingPlayer ?: return
    val forbidden = if (player == state.dealer) state.forbiddenPrediction else null
    val others = record.predictions.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundHeader(state, onShowScoreboard)

        if (record.predictions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                record.predictions.forEach { (index, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.players[index])
                        Text("$value")
                    }
                }
            }
        }

        AnimatedContent(targetState = player, transitionSpec = { fadeIn() togetherWith fadeOut() }) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${state.players[p]}, predict your tricks:",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (p == state.dealer) {
                        Text(
                            if (forbidden != null) {
                                "As dealer you may not predict $forbidden: the total must not equal ${record.cards} tricks."
                            } else {
                                "As dealer you may predict anything: the others already predicted more than ${record.cards} tricks together."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChoiceChips(
                        max = record.cards,
                        selected = null,
                        onSelect = onPredict,
                        disabled = if (forbidden != null) setOf(forbidden) else emptySet(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Others predicted $others of ${record.cards} tricks so far",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Tricks phase: enter how many tricks each player actually won. */
@Composable
fun TricksScreen(
    state: GameState,
    onTricks: (playerIndex: Int, value: Int) -> Unit,
    onConfirm: () -> Unit,
    onShowScoreboard: () -> Unit,
) {
    val record = state.currentRecord
    val entered = record.tricksSum()
    val complete = state.tricksComplete

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoundHeader(state, onShowScoreboard)

        state.players.forEachIndexed { index, name ->
            val prediction = record.predictions[index]
            val maxAllowed = record.cards - record.tricks.filterKeys { it != index }.values.sum()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "predicted $prediction",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChoiceChips(
                        max = record.cards.coerceAtMost(maxAllowed),
                        selected = record.tricks[index],
                        onSelect = { onTricks(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Text(
            "Tricks entered: $entered of ${record.cards}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (entered == record.cards) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = onConfirm,
            enabled = complete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Score the round")
        }
        Spacer(Modifier.height(24.dp))
    }
}
