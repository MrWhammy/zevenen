package com.zevenen.app

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.zevenen.app.game.GameRules
import com.zevenen.app.game.GameState
import com.zevenen.app.ui.PredictScreen
import com.zevenen.app.ui.SetupScreen
import com.zevenen.app.ui.SummaryScreen
import com.zevenen.app.ui.TricksScreen
import com.zevenen.app.ui.ZevenenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every screen of the app on the JVM with Robolectric and saves a PNG
 * for each, so the result can be seen without running on a device.
 *
 * Output: /home/brecht/Code/zevenen-screenshots/
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outputDir = File("/home/brecht/Code/zevenen-screenshots")

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        outputDir.mkdirs()
        File(outputDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun setState(content: @Composable () -> Unit) {
        compose.setContent { ZevenenTheme { content() } }
        compose.waitForIdle()
    }

    private val players = listOf("Anna", "Bob", "Cees", "Dorien")

    /** 15 rounds: 7-6-5-4-3-2-1-1-2-3-4-5-6-7-1. Round 1 has 7 cards. */
    private val rules = GameRules.DEFAULT

    @Test
    fun screenshotSetup() {
        setState { SetupScreen(onStart = { _, _ -> }) }
        capture("01_setup")
    }

    @Test
    fun screenshotPredictionMidRound() {
        // Anna deals; Bob and Cees predicted already, Dorien is next.
        val state = GameState(players, rules, firstDealer = 0)
            .withPrediction(1, 3)
            .withPrediction(2, 0)
        setState { PredictScreen(state, onPredict = {}, onShowScoreboard = {}) }
        capture("02_prediction")
    }

    @Test
    fun screenshotDealerForbiddenPrediction() {
        // Only the dealer (Anna) is left; 4 of 7 tricks are taken, so 3 is forbidden.
        val state = GameState(players, rules, firstDealer = 0)
            .withPrediction(1, 3)
            .withPrediction(2, 0)
            .withPrediction(3, 1)
        setState { PredictScreen(state, onPredict = {}, onShowScoreboard = {}) }
        capture("03_dealer_forbidden")
    }

    @Test
    fun screenshotTricksEntry() {
        val state = GameState(players, rules, firstDealer = 0)
            .withPrediction(1, 3)
            .withPrediction(2, 0)
            .withPrediction(3, 1)
            .withPrediction(0, 2)
            .withTricks(0, 1)
        setState { TricksScreen(state, onTricks = { _, _ -> }, onConfirm = {}, onShowScoreboard = {}) }
        capture("04_tricks")
    }

    @Test
    fun screenshotTricksComplete() {
        val state = GameState(players, rules, firstDealer = 0)
            .withPrediction(1, 3)
            .withPrediction(2, 0)
            .withPrediction(3, 1)
            .withPrediction(0, 2)
            .withTricks(0, 3)
            .withTricks(1, 2)
            .withTricks(2, 0)
            .withTricks(3, 2)
        // still in Tricks phase; the tricks sum to 7 so the confirm button is enabled
        setState { TricksScreen(state, onTricks = { _, _ -> }, onConfirm = {}, onShowScoreboard = {}) }
        capture("07_tricks_complete")
    }

    @Test
    fun screenshotRoundSummary() {
        val state = GameState(players, rules, firstDealer = 0)
            .withPrediction(1, 3)
            .withPrediction(2, 0)
            .withPrediction(3, 1)
            .withPrediction(0, 2)
            .withTricks(0, 3)
            .withTricks(1, 2)
            .withTricks(2, 0)
            .withTricks(3, 2)
            .finishRound()
        setState { SummaryScreen(state, onNext = {}, onNewGame = {}) }
        capture("05_summary")
    }

    @Test
    fun screenshotFinished() {
        // Short game: max 2 cards, down first, no repeat, no final round -> 2-1-2.
        val shortRules = GameRules(maxCards = 2, upFirst = false, repeatMiddle = false, finalSingleCard = false)
        var state = GameState(players, shortRules, firstDealer = 0)

        // Round 1: 2 cards, dealer Anna. Order: Bob, Cees, Dorien, Anna.
        state = state
            .withPrediction(1, 1).withPrediction(2, 0).withPrediction(3, 1)
            .withPrediction(0, 1) // others predicted 2 of 2 cards, so 0 is forbidden for the dealer
            .withTricks(0, 1).withTricks(1, 1).withTricks(2, 0).withTricks(3, 0)
            .finishRound().nextRound()

        // Round 2: 1 card, dealer Bob. Order: Cees, Dorien, Anna, Bob.
        state = state
            .withPrediction(2, 1).withPrediction(3, 0).withPrediction(0, 0)
            .withPrediction(1, 1) // others predicted 1 of 1 card, so 0 is forbidden for the dealer
            .withTricks(0, 0).withTricks(1, 1).withTricks(2, 0).withTricks(3, 0)
            .finishRound().nextRound()

        // Round 3: 2 cards, dealer Cees. Order: Dorien, Anna, Bob, Cees.
        state = state
            .withPrediction(3, 2).withPrediction(0, 0).withPrediction(1, 2)
            .withPrediction(2, 0) // others already took 4 of 2 cards: no forbidden value
            .withTricks(0, 1).withTricks(1, 1).withTricks(2, 0).withTricks(3, 0)
            .finishRound().nextRound()

        setState { SummaryScreen(state, onNext = {}, onNewGame = {}) }
        capture("06_finished")
    }
}
