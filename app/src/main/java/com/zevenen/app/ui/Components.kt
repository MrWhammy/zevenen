package com.zevenen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Row of choice chips for values 0..[max], wrapping onto multiple lines.
 * [disabled] values cannot be selected (e.g. the dealer's forbidden prediction,
 * or trick values that would exceed the number of cards dealt).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    max: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Set<Int> = emptySet(),
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (value in 0..max) {
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                enabled = value !in disabled,
                label = { Text(value.toString()) },
            )
        }
    }
}

@Composable
fun ScoreLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
