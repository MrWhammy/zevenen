package com.zevenen.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.zevenen.app.game.GameRules
import com.zevenen.app.game.GameState
import com.zevenen.app.game.Phase
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Holds the running game. The state is kept in a Compose-observable property and
 * persisted to SharedPreferences after every change, so the game survives the app
 * being killed (e.g. when switching apps mid-game).
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    var state: GameState? by mutableStateOf(load())
        private set

    fun startGame(players: List<String>, rules: GameRules) {
        state = GameState(players, rules, firstDealer = Random.nextInt(players.size))
        persist()
    }

    fun predict(value: Int) {
        val current = state ?: return
        val player = current.predictingPlayer ?: return
        state = current.withPrediction(player, value)
        persist()
    }

    fun setTricks(playerIndex: Int, value: Int) {
        val current = state ?: return
        if (current.phase != Phase.Tricks) return
        state = current.withTricks(playerIndex, value)
        persist()
    }

    fun confirmRound() {
        val current = state ?: return
        state = current.finishRound()
        persist()
    }

    fun nextRound() {
        val current = state ?: return
        state = current.nextRound()
        persist()
    }

    fun newGame() {
        state = null
        prefs().edit().remove(KEY_STATE).apply()
    }

    private fun prefs() = getApplication<Application>().getSharedPreferences(PREFS, Application.MODE_PRIVATE)

    private fun persist() {
        val s = state
        if (s == null) {
            prefs().edit().remove(KEY_STATE).apply()
            return
        }
        val json = JSONObject().apply {
            put("players", JSONArray(s.players))
            put("rules", JSONObject().apply {
                put("maxCards", s.rules.maxCards)
                put("upFirst", s.rules.upFirst)
                put("repeatMiddle", s.rules.repeatMiddle)
                put("finalSingleCard", s.rules.finalSingleCard)
                put("correctBonus", s.rules.correctBonus)
                put("perTrickPoints", s.rules.perTrickPoints)
                put("penaltyPerDiff", s.rules.penaltyPerDiff)
            })
            put("firstDealer", s.firstDealer)
            put("currentRound", s.currentRound)
            put("phase", s.phase.name)
            put("records", JSONArray().apply {
                s.records.forEach { r ->
                    put(JSONObject().apply {
                        put("cards", r.cards)
                        put("predictions", mapToJson(r.predictions))
                        put("tricks", mapToJson(r.tricks))
                    })
                }
            })
        }
        prefs().edit().putString(KEY_STATE, json.toString()).apply()
    }

    private fun mapToJson(map: Map<Int, Int>): JSONObject {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), v) }
        return o
    }

    private fun jsonToMap(o: JSONObject): Map<Int, Int> {
        val m = mutableMapOf<Int, Int>()
        for (key in o.keys()) m[key.toInt()] = o.getInt(key)
        return m
    }

    private fun load(): GameState? = try {
        val raw = prefs().getString(KEY_STATE, null) ?: return null
        val json = JSONObject(raw)
        val rulesJson = json.getJSONObject("rules")
        val rules = GameRules(
            maxCards = rulesJson.getInt("maxCards"),
            upFirst = rulesJson.getBoolean("upFirst"),
            repeatMiddle = rulesJson.getBoolean("repeatMiddle"),
            finalSingleCard = rulesJson.getBoolean("finalSingleCard"),
            correctBonus = rulesJson.getInt("correctBonus"),
            perTrickPoints = rulesJson.getInt("perTrickPoints"),
            penaltyPerDiff = rulesJson.getInt("penaltyPerDiff"),
        )
        val records = mutableListOf<com.zevenen.app.game.RoundRecord>()
        for (i in 0 until json.getJSONArray("records").length()) {
            val r = json.getJSONArray("records").getJSONObject(i)
            records.add(
                com.zevenen.app.game.RoundRecord(
                    cards = r.getInt("cards"),
                    predictions = jsonToMap(r.getJSONObject("predictions")),
                    tricks = jsonToMap(r.getJSONObject("tricks")),
                )
            )
        }
        GameState(
            players = (0 until json.getJSONArray("players").length()).map { json.getJSONArray("players").getString(it) },
            rules = rules,
            firstDealer = json.getInt("firstDealer"),
            currentRound = json.getInt("currentRound"),
            phase = Phase.valueOf(json.getString("phase")),
            records = records,
        )
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PREFS = "zevenen"
        private const val KEY_STATE = "game_state"
    }
}
