package com.zevenen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zevenen.app.game.Phase
import com.zevenen.app.ui.PredictScreen
import com.zevenen.app.ui.ScoreboardTable
import com.zevenen.app.ui.SetupScreen
import com.zevenen.app.ui.SummaryScreen
import com.zevenen.app.ui.TricksScreen
import com.zevenen.app.ui.ZevenenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZevenenTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val vm: GameViewModel = viewModel()
    val state = vm.state
    var showScoreboard by remember { mutableStateOf(false) }

    if (state == null) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SetupScreen(onStart = vm::startGame)
        }
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (state.phase) {
                Phase.Prediction -> PredictScreen(
                    state = state,
                    onPredict = vm::predict,
                    onShowScoreboard = { showScoreboard = true },
                )
                Phase.Tricks -> TricksScreen(
                    state = state,
                    onTricks = vm::setTricks,
                    onConfirm = vm::confirmRound,
                    onShowScoreboard = { showScoreboard = true },
                )
                Phase.Summary -> SummaryScreen(state = state, onNext = vm::nextRound, onNewGame = vm::newGame)
                Phase.Finished -> SummaryScreen(state = state, onNext = {}, onNewGame = vm::newGame)
            }
        }
    }

    if (showScoreboard) {
        AlertDialog(
            onDismissRequest = { showScoreboard = false },
            confirmButton = {
                TextButton(onClick = { showScoreboard = false }) { Text("Close") }
            },
            title = { Text("Scoreboard") },
            text = {
                Column {
                    if (state.completedRounds.isEmpty()) {
                        Text("No rounds scored yet.")
                    } else {
                        ScoreboardTable(state)
                    }
                }
            },
        )
    }
}
