package com.zevenen.app.game

/** Predictions and tricks for a single round, keyed by player index. */
data class RoundRecord(
    val cards: Int,
    val predictions: Map<Int, Int> = emptyMap(),
    val tricks: Map<Int, Int> = emptyMap(),
) {
    fun predictionSum(): Int = predictions.values.sum()
    fun tricksSum(): Int = tricks.values.sum()
}

enum class Phase { Prediction, Tricks, Summary, Finished }

/**
 * Immutable state of a running game. All mutating functions validate their input and
 * return a new state; the original is left untouched.
 *
 * The dealer is [firstDealer] in round 1 and rotates to the next player every round.
 * Predictions are entered starting left of the dealer; the dealer predicts last and may
 * not predict the value that would make the total equal the number of cards dealt.
 */
data class GameState(
    val players: List<String>,
    val rules: GameRules,
    val firstDealer: Int,
    val currentRound: Int = 1,
    val phase: Phase = Phase.Prediction,
    val records: List<RoundRecord> = List(rules.totalRounds) { RoundRecord(rules.cardsForRound(it + 1)) },
) {
    init {
        require(players.size in MIN_PLAYERS..MAX_PLAYERS) { "need $MIN_PLAYERS..$MAX_PLAYERS players" }
        require(players.all { it.isNotBlank() }) { "player names must not be blank" }
        require(firstDealer in players.indices)
    }

    val dealer: Int get() = (firstDealer + currentRound - 1) % players.size

    /** Player indices in prediction order: left of the dealer first, dealer last. */
    val predictionOrder: List<Int>
        get() = List(players.size) { (dealer + 1 + it) % players.size }

    /** The player currently expected to predict, or null once all predictions are in. */
    val predictingPlayer: Int?
        get() = if (phase != Phase.Prediction) null
        else predictionOrder.firstOrNull { it !in records[currentRound - 1].predictions }

    /**
     * The prediction the dealer is not allowed to make: the value that would make the
     * sum of all predictions exactly equal the number of cards dealt. Null if no value
     * is forbidden (the others' predictions already exceed the number of tricks).
     */
    val forbiddenPrediction: Int?
        get() {
            if (phase != Phase.Prediction) return null
            val record = records[currentRound - 1]
            val others = record.predictions.filterKeys { it != dealer }.values.sum()
            val forbidden = record.cards - others
            return if (forbidden in 0..record.cards) forbidden else null
        }

    val currentRecord: RoundRecord get() = records[currentRound - 1]

    /** Records a prediction for [playerIndex]. Only valid in the prediction phase. */
    fun withPrediction(playerIndex: Int, prediction: Int): GameState {
        require(phase == Phase.Prediction) { "not in prediction phase" }
        require(playerIndex == predictingPlayer) { "not this player's turn to predict" }
        val record = currentRecord
        require(prediction in 0..record.cards) { "prediction out of range" }
        if (playerIndex == dealer) {
            require(prediction != forbiddenPrediction) { "total of predictions may not equal ${record.cards}" }
        }
        val newPredictions = record.predictions + (playerIndex to prediction)
        val allIn = newPredictions.size == players.size
        val newRecords = records.toMutableList().also {
            it[currentRound - 1] = record.copy(predictions = newPredictions)
        }
        return copy(records = newRecords, phase = if (allIn) Phase.Tricks else Phase.Prediction)
    }

    /** Records the number of tricks won by [playerIndex]. Only valid in the tricks phase. */
    fun withTricks(playerIndex: Int, tricks: Int): GameState {
        require(phase == Phase.Tricks) { "not in tricks phase" }
        require(playerIndex in players.indices)
        val record = currentRecord
        require(tricks in 0..record.cards) { "tricks out of range" }
        val others = record.tricks.filterKeys { it != playerIndex }.values.sum()
        require(others + tricks <= record.cards) { "more tricks than cards dealt" }
        val newRecords = records.toMutableList().also {
            it[currentRound - 1] = record.copy(tricks = record.tricks + (playerIndex to tricks))
        }
        return copy(records = newRecords)
    }

    /** True once the entered tricks sum exactly to the number of cards dealt. */
    val tricksComplete: Boolean
        get() = phase == Phase.Tricks && currentRecord.tricks.size == players.size &&
            currentRecord.tricksSum() == currentRecord.cards

    /** Moves from the tricks phase to the round summary. */
    fun finishRound(): GameState {
        require(tricksComplete) { "tricks do not sum to ${currentRecord.cards}" }
        return copy(phase = Phase.Summary)
    }

    /** Moves from the summary to the next round, or to the finished phase after the last round. */
    fun nextRound(): GameState {
        require(phase == Phase.Summary) { "not in summary phase" }
        return if (currentRound == rules.totalRounds) {
            copy(phase = Phase.Finished)
        } else {
            copy(currentRound = currentRound + 1, phase = Phase.Prediction)
        }
    }

    /** Indices of all rounds whose tricks are fully entered (used for scoring). */
    val completedRounds: List<Int>
        get() = (1..rules.totalRounds).filter { r ->
            val rec = records[r - 1]
            rec.predictions.size == players.size && rec.tricks.size == players.size && rec.tricksSum() == rec.cards
        }

    /** Points [playerIndex] scored in [round] (only meaningful for completed rounds). */
    fun roundPoints(round: Int, playerIndex: Int): Int {
        val record = records[round - 1]
        return rules.roundScore(record.predictions.getValue(playerIndex), record.tricks.getValue(playerIndex))
    }

    /** Cumulative score of [playerIndex] over all completed rounds. */
    fun totalScore(playerIndex: Int): Int =
        completedRounds.sumOf { roundPoints(it, playerIndex) }

    /** Total scores, indexed by player index. */
    val totalScores: List<Int> get() = players.indices.map { totalScore(it) }

    /** Player indices with the highest total score (may be more than one on a tie). */
    val winners: List<Int>
        get() {
            val scores = totalScores
            val best = scores.max()
            return scores.indices.filter { scores[it] == best }
        }
}

const val MIN_PLAYERS = 2
const val MAX_PLAYERS = 8
