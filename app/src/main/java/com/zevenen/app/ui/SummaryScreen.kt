package com.zevenen.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zevenen.app.game.GameState
import com.zevenen.app.game.Phase

/**
 * Round summary (and final screen): the scores of the round just played,
 * the full scoreboard, and a button to continue or start over.
 */
@Composable
fun SummaryScreen(
    state: GameState,
    onNext: () -> Unit,
    onNewGame: () -> Unit,
) {
    val finished = state.phase == Phase.Finished
    val lastRound = state.completedRounds.last()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (finished) {
            val winners = state.winners
            Text(
                if (winners.size == 1)
                    "${state.players[winners[0]]} wins!"
                else
                    "Tie between ${winners.joinToString(" and ") { state.players[it] }}!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text("Round $lastRound scored", style = MaterialTheme.typography.headlineSmall)
        }

        // Detail of the round that was just played.
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Player", Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium)
                    Text("Pred", Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text("Won", Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text("Points", Modifier.weight(0.7f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                    Text("Total", Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                }
                state.players.forEachIndexed { index, name ->
                    val record = state.records[lastRound - 1]
                    val points = state.roundPoints(lastRound, index)
                    Row(Modifier.fillMaxWidth()) {
                        Text(name, Modifier.weight(1.2f))
                        Text("${record.predictions[index]}", Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text("${record.tricks[index]}", Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text(
                            (if (points >= 0) "+" else "") + points,
                            Modifier.weight(0.7f),
                            textAlign = TextAlign.End,
                            color = if (points >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "${state.totalScore(index)}",
                            Modifier.weight(0.8f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Full scoreboard over all completed rounds.
        ScoreboardTable(state)

        Button(
            onClick = onNext,
            enabled = !finished,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next round")
        }
        OutlinedButton(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (finished) "New game" else "Abandon game")
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Table of all completed rounds: one row per round, one column per player, totals at the end. */
@Composable
fun ScoreboardTable(state: GameState) {
    val rounds = state.completedRounds
    if (rounds.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Column(Modifier.width(52.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("", Modifier.height(20.dp))
                rounds.forEach { round ->
                    val record = state.records[round - 1]
                    Text(
                        "$round (${record.cards})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("Total", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            state.players.forEachIndexed { index, name ->
                Column(
                    Modifier
                        .width(44.dp)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(name.take(5), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    rounds.forEach { round ->
                        Text(
                            state.roundPoints(round, index).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        state.totalScore(index).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
