package com.zevenen.app.game

import kotlin.math.abs

/**
 * Configurable rules for a game of Zevenen.
 *
 * The round structure is a "mountain": deals count up from 1 card to [maxCards]
 * and back down (or down first and back up), optionally repeating the turning
 * round, optionally followed by a single extra round with 1 card.
 *
 * @param maxCards highest number of cards dealt in a round.
 * @param upFirst true: start with 1 card, count up to [maxCards], then back down.
 *   false: start with [maxCards], count down to 1, then back up.
 * @param repeatMiddle true: the turning round is dealt twice in a row
 *   (up first: "12, 13, 13, 12"; down first: "2, 1, 1, 2").
 * @param finalSingleCard true: add one extra round with a single card at the end.
 * @param correctBonus points for predicting the number of tricks exactly right.
 * @param perTrickPoints extra points per trick won when the prediction was exact.
 * @param penaltyPerDiff negative points per difference between prediction and tricks won.
 */
data class GameRules(
    val maxCards: Int,
    val upFirst: Boolean,
    val repeatMiddle: Boolean,
    val finalSingleCard: Boolean,
    val correctBonus: Int = 10,
    val perTrickPoints: Int = 2,
    val penaltyPerDiff: Int = 2,
) {
    init {
        require(maxCards in 1..MAX_CARDS) { "invalid maximum card count" }
        require(correctBonus in 0..1000) { "invalid bonus" }
        require(perTrickPoints in 0..100) { "invalid trick points" }
        require(penaltyPerDiff in 0..100) { "invalid penalty" }
    }

    /** Number of cards dealt in every round of the game. */
    val pattern: List<Int> = buildList {
        if (upFirst) {
            addAll(1..maxCards)
            if (repeatMiddle) add(maxCards)
            addAll((maxCards - 1) downTo 1)
        } else {
            addAll(maxCards downTo 1)
            if (repeatMiddle) add(1)
            addAll(2..maxCards)
        }
        if (finalSingleCard) add(1)
    }

    val totalRounds: Int get() = pattern.size

    fun cardsForRound(round: Int): Int {
        require(round in 1..totalRounds) { "round $round out of range" }
        return pattern[round - 1]
    }

    /** Score for a single player in a single round, given prediction and tricks won. */
    fun roundScore(prediction: Int, tricks: Int): Int =
        if (prediction == tricks) correctBonus + perTrickPoints * tricks
        else -penaltyPerDiff * abs(prediction - tricks)

    companion object {
        const val MAX_CARDS = 52

        /** Default: 7 down to 1, 1 repeated, back up to 7, plus a final single-card round. */
        val DEFAULT: GameRules = GameRules(
            maxCards = 7,
            upFirst = false,
            repeatMiddle = true,
            finalSingleCard = true,
        )
    }
}
