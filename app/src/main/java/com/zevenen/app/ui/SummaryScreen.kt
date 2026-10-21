package com.zevenen.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zevenen.app.game.GameState
import com.zevenen.app.game.Phase
import kotlinx.coroutines.launch

/**
 * Round summary (and final screen): the scores of the round just played,
 * the full scoreboard, and buttons to continue, share the final scores as an
 * image, or start over.
 */
@Composable
fun SummaryScreen(
    state: GameState,
    onNext: () -> Unit,
    onNewGame: () -> Unit,
    onShare: (image: Bitmap, text: String) -> Unit = { _, _ -> },
) {
    val finished = state.phase == Phase.Finished
    val lastRound = state.completedRounds.last()
    var showAbandonDialog by remember { mutableStateOf(false) }
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The scores area; on the final screen this is what gets shared as an image.
        Box(
            Modifier
                .fillMaxWidth()
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawContent()
                }
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                Modifier.padding(bottom = 12.dp),
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
            }
        }

        if (!finished) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (lastRound == state.rules.totalRounds) "Show final scores" else "Next round")
            }
        }
        OutlinedButton(
            onClick = { showAbandonDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (finished) "New game" else "Abandon game")
        }
        if (finished) {
            Button(
                onClick = {
                    scope.launch {
                        val image = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        onShare(image, shareText(state))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Share scores")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text(if (finished) "Start a new game?" else "Abandon game?") },
            text = {
                Text(
                    if (finished)
                        "You will return to the setup screen and the final scores will be cleared."
                    else
                        "The current game and all its scores will be lost."
                )
            },
            confirmButton = {
                TextButton(onClick = onNewGame) {
                    Text(if (finished) "New game" else "Abandon", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text(if (finished) "Review scores" else "Keep playing")
                }
            },
        )
    }
}

/** Ranked plain-text summary used as fallback for apps that only accept text. */
private fun shareText(state: GameState): String {
    val ranked = state.players.indices.sortedByDescending { state.totalScore(it) }
    val lines = ranked.mapIndexed { i, player ->
        "${i + 1}. ${state.players[player]} — ${state.totalScore(player)}"
    }
    return "Zevenen — final scores after ${state.rules.totalRounds} rounds:\n" +
        lines.joinToString("\n")
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
