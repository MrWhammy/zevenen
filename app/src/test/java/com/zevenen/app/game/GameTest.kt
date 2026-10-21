package com.zevenen.app.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    @Test
    fun `down first with repeated middle and final round`() {
        // 7 down to 1, 1 repeated, back up to 7, plus a final single-card round.
        val rules = GameRules(maxCards = 7, upFirst = false, repeatMiddle = true, finalSingleCard = true)
        assertEquals(
            listOf(7, 6, 5, 4, 3, 2, 1, 1, 2, 3, 4, 5, 6, 7, 1),
            rules.pattern,
        )
        assertEquals(15, rules.totalRounds)
    }

    @Test
    fun `down first without repeat or final round`() {
        val rules = GameRules(maxCards = 7, upFirst = false, repeatMiddle = false, finalSingleCard = false)
        assertEquals(listOf(7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7), rules.pattern)
        assertEquals(13, rules.totalRounds)
    }

    @Test
    fun `up first with repeated middle`() {
        val rules = GameRules(maxCards = 13, upFirst = true, repeatMiddle = true, finalSingleCard = false)
        val expected = (1..12).toList() + listOf(13, 13) + (12 downTo 1).toList()
        assertEquals(expected, rules.pattern)
        assertEquals(26, rules.totalRounds)
    }

    @Test
    fun `up first without repeat, with final round`() {
        val rules = GameRules(maxCards = 2, upFirst = true, repeatMiddle = false, finalSingleCard = true)
        assertEquals(listOf(1, 2, 1, 1), rules.pattern)
    }

    @Test
    fun `cards per round follows the pattern`() {
        val rules = GameRules(maxCards = 2, upFirst = false, repeatMiddle = false, finalSingleCard = false)
        assertEquals(2, rules.cardsForRound(1))
        assertEquals(1, rules.cardsForRound(2))
        assertEquals(2, rules.cardsForRound(3))
    }

    @Test
    fun `rejects invalid maximum card count`() {
        try {
            GameRules(maxCards = 0, upFirst = true, repeatMiddle = false, finalSingleCard = false)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
        try {
            GameRules(maxCards = 53, upFirst = true, repeatMiddle = false, finalSingleCard = false)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun `scores correct prediction as bonus plus tricks`() {
        val rules = GameRules(maxCards = 7, upFirst = false, repeatMiddle = false, finalSingleCard = false)
        assertEquals(10, rules.roundScore(prediction = 0, tricks = 0))
        assertEquals(10 + 2 * 3, rules.roundScore(prediction = 3, tricks = 3))
        assertEquals(10 + 2 * 7, rules.roundScore(prediction = 7, tricks = 7))
    }

    @Test
    fun `scores wrong prediction as minus twice the difference`() {
        val rules = GameRules(maxCards = 7, upFirst = false, repeatMiddle = false, finalSingleCard = false)
        assertEquals(-2, rules.roundScore(prediction = 3, tricks = 2))
        assertEquals(-4, rules.roundScore(prediction = 1, tricks = 3))
        assertEquals(-8, rules.roundScore(prediction = 5, tricks = 1))
    }

    @Test
    fun `supports custom scoring`() {
        val rules = GameRules(
            maxCards = 1, upFirst = true, repeatMiddle = false, finalSingleCard = false,
            correctBonus = 20, perTrickPoints = 3, penaltyPerDiff = 1,
        )
        assertEquals(23, rules.roundScore(1, 1))
        assertEquals(-3, rules.roundScore(1, 4))
    }
}

class GameStateTest {

    // Pattern: 2, 1, 2 (three rounds). Anna deals round 1.
    private val rules = GameRules(maxCards = 2, upFirst = false, repeatMiddle = false, finalSingleCard = false)
    private val players = listOf("Anna", "Bob", "Cees")

    private fun gameAtPredictionPhase(): GameState {
        var state = GameState(players, rules, firstDealer = 0)
        // Prediction order: Bob, Cees, Anna (dealer last)
        state = state.withPrediction(1, 1)
        state = state.withPrediction(2, 0)
        return state
    }

    @Test
    fun `dealer rotates every round and dealer predicts last`() {
        val state = GameState(players, rules, firstDealer = 0)
        assertEquals(0, state.dealer)
        assertEquals(listOf(1, 2, 0), state.predictionOrder)
        val round2 = state.copy(currentRound = 2)
        assertEquals(1, round2.dealer)
        assertEquals(listOf(2, 0, 1), round2.predictionOrder)
    }

    @Test
    fun `forbidden prediction makes total differ from cards`() {
        val state = gameAtPredictionPhase()
        // Others predicted 1 + 0 = 1 of 2 cards, so Anna (dealer) cannot predict 1.
        assertEquals(1, state.forbiddenPrediction)
        assertEquals(0, state.predictingPlayer)
    }

    @Test
    fun `dealer cannot predict the forbidden value`() {
        val state = gameAtPredictionPhase()
        try {
            state.withPrediction(0, 1)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("2"))
        }
        // Any other value is fine.
        assertEquals(Phase.Tricks, state.withPrediction(0, 0).phase)
        assertEquals(Phase.Tricks, state.withPrediction(0, 2).phase)
    }

    @Test
    fun `no forbidden prediction when others already exceeded the cards`() {
        var state = GameState(players, rules, firstDealer = 0)
        state = state.withPrediction(1, 2)
        state = state.withPrediction(2, 2)
        assertNull(state.forbiddenPrediction)
    }

    @Test
    fun `players predict in order`() {
        val state = GameState(players, rules, firstDealer = 0)
        assertEquals(1, state.predictingPlayer)
        try {
            state.withPrediction(0, 1) // Anna is the dealer and predicts last
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun `tricks must sum to the number of cards`() {
        var state = gameAtPredictionPhase().withPrediction(0, 0)
        assertEquals(Phase.Tricks, state.phase)
        state = state.withTricks(0, 2)
        state = state.withTricks(1, 0)
        state = state.withTricks(2, 0)
        assertTrue(state.tricksComplete)
        try {
            state.withTricks(2, 2)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
        state = state.finishRound()
        assertEquals(Phase.Summary, state.phase)
    }

    @Test
    fun `full round scoring and totals`() {
        var state = gameAtPredictionPhase().withPrediction(0, 0)
        state = state.withTricks(0, 1).withTricks(1, 1).withTricks(2, 0).finishRound()
        // Anna: predicted 0, won 1 -> -2. Bob: predicted 1, won 1 -> 12. Cees: predicted 0, won 0 -> 10.
        assertEquals(-2, state.roundPoints(1, 0))
        assertEquals(12, state.roundPoints(1, 1))
        assertEquals(10, state.roundPoints(1, 2))
        assertEquals(listOf(-2, 12, 10), state.totalScores)
    }

    @Test
    fun `advances through rounds and finishes`() {
        // Round 1: 2 cards, dealer Anna.
        var state = gameAtPredictionPhase().withPrediction(0, 2)
        state = state.withTricks(0, 1).withTricks(1, 0).withTricks(2, 1).finishRound()
        assertEquals(1, state.completedRounds.last())
        state = state.nextRound()
        assertEquals(2, state.currentRound)
        assertEquals(Phase.Prediction, state.phase)
        assertEquals(1, state.dealer)
        assertEquals(1, state.currentRecord.cards)
        // Round 2: 1 card, dealer Bob. Order: Cees, Anna, Bob.
        state = state.withPrediction(2, 1).withPrediction(0, 0).withPrediction(1, 1)
        state = state.withTricks(0, 0).withTricks(1, 1).withTricks(2, 0).finishRound()
        state = state.nextRound()
        // Round 3: 2 cards, dealer Cees. Order: Anna, Bob, Cees.
        state = state.withPrediction(0, 2).withPrediction(1, 0).withPrediction(2, 2)
        state = state.withTricks(0, 1).withTricks(1, 1).withTricks(2, 0).finishRound()
        state = state.nextRound()
        assertEquals(Phase.Finished, state.phase)
    }

    @Test
    fun `winners are the players with the highest total, ties included`() {
        var state = gameAtPredictionPhase().withPrediction(0, 0)
        state = state.withTricks(0, 0).withTricks(1, 2).withTricks(2, 0).finishRound()
        // Anna: correct -> 10. Bob: predicted 1, won 2 -> -2. Cees: correct -> 10.
        assertEquals(listOf(0, 2), state.winners)
    }

    @Test
    fun `rejects invalid player counts and blank names`() {
        try {
            GameState(listOf("A"), rules, 0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
        try {
            GameState(listOf("A", " "), rules, 0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) { }
    }
}
