package com.zevenen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zevenen.app.game.GameRules
import com.zevenen.app.game.MAX_PLAYERS
import com.zevenen.app.game.MIN_PLAYERS

/** Setup screen: player names and configurable rules. */
@Composable
fun SetupScreen(onStart: (players: List<String>, rules: GameRules) -> Unit) {
    var playerNames by remember { mutableStateOf(List(3) { "" }) }
    var maxCardsText by remember { mutableStateOf(GameRules.DEFAULT.maxCards.toString()) }
    var upFirst by remember { mutableStateOf(GameRules.DEFAULT.upFirst) }
    var repeatMiddle by remember { mutableStateOf(GameRules.DEFAULT.repeatMiddle) }
    var finalSingleCard by remember { mutableStateOf(GameRules.DEFAULT.finalSingleCard) }
    var bonusText by remember { mutableStateOf(GameRules.DEFAULT.correctBonus.toString()) }
    var trickText by remember { mutableStateOf(GameRules.DEFAULT.perTrickPoints.toString()) }
    var penaltyText by remember { mutableStateOf(GameRules.DEFAULT.penaltyPerDiff.toString()) }

    val maxCards = maxCardsText.toIntOrNull()
    val bonus = bonusText.toIntOrNull()
    val trick = trickText.toIntOrNull()
    val penalty = penaltyText.toIntOrNull()

    val names = playerNames.map { it.trim() }
    val named = names.filter { it.isNotEmpty() }
    val playersValid = named.size >= MIN_PLAYERS && named.distinct().size == named.size

    val roundsValid = maxCards != null && maxCards in 1..GameRules.MAX_CARDS
    val scoringValid = bonus != null && trick != null && penalty != null &&
        bonus in 0..1000 && trick in 0..100 && penalty in 0..100

    val previewRules = if (roundsValid) {
        GameRules(maxCards!!, upFirst, repeatMiddle, finalSingleCard)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Zevenen", style = MaterialTheme.typography.headlineLarge)
        Text("Trick prediction score keeper", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Players", style = MaterialTheme.typography.titleMedium)
        playerNames.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        playerNames = playerNames.toMutableList().also { it[index] = newValue }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Player ${index + 1}") },
                    singleLine = true,
                )
                if (playerNames.size > MIN_PLAYERS) {
                    OutlinedButton(
                        onClick = { playerNames = playerNames.filterIndexed { i, _ -> i != index } },
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
        if (playerNames.size < MAX_PLAYERS) {
            TextButton(
                onClick = { playerNames = playerNames + "" },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text("Add player")
            }
        }

        Text("Rounds", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = maxCardsText,
            onValueChange = { maxCardsText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Maximum cards in a round") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = !roundsValid,
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !upFirst,
                onClick = { upFirst = false },
                label = { Text("$maxCards → 1 → $maxCards") },
            )
            FilterChip(
                selected = upFirst,
                onClick = { upFirst = true },
                label = { Text("1 → $maxCards → 1") },
            )
        }
        SwitchRow(
            text = "Repeat the turning round",
            checked = repeatMiddle,
            onCheckedChange = { repeatMiddle = it },
            supporting = if (upFirst) "… ${maxCardsNr(maxCards, -1)}, $maxCards, $maxCards, ${maxCardsNr(maxCards, -1)} …"
            else "… 2, 1, 1, 2 …",
        )
        SwitchRow(
            text = "Final round with 1 card",
            checked = finalSingleCard,
            onCheckedChange = { finalSingleCard = it },
        )
        if (previewRules != null) {
            Text(
                "Rounds: ${previewRules.pattern.joinToString("-")}" +
                    " (${previewRules.totalRounds} rounds)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Scoring", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bonusText,
                onValueChange = { bonusText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Correct bonus") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = bonus == null,
                singleLine = true,
            )
            OutlinedTextField(
                value = trickText,
                onValueChange = { trickText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Points per trick") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = trick == null,
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = penaltyText,
            onValueChange = { penaltyText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Penalty per missed trick") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = penalty == null,
            singleLine = true,
        )
        Text(
            "Correct prediction: bonus + points per trick won. Wrong prediction: penalty x difference.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                if (playersValid && roundsValid && scoringValid) {
                    onStart(
                        named,
                        GameRules(
                            maxCards!!,
                            upFirst,
                            repeatMiddle,
                            finalSingleCard,
                            bonus!!,
                            trick!!,
                            penalty!!,
                        ),
                    )
                }
            },
            enabled = playersValid && roundsValid && scoringValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start game")
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun maxCardsNr(maxCards: Int?, offset: Int): String =
    ((maxCards ?: 7) + offset).coerceAtLeast(1).toString()

@Composable
private fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
