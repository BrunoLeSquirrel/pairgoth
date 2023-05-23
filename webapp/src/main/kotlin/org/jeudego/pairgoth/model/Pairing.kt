package org.jeudego.pairgoth.model

import com.republicate.kson.Json
import org.jeudego.pairgoth.api.ApiHandler.Companion.badRequest
import org.jeudego.pairgoth.model.Pairing.PairingType.*
import org.jeudego.pairgoth.model.MacMahon
import org.jeudego.pairgoth.model.RoundRobin
import org.jeudego.pairgoth.model.Swiss
import org.jeudego.pairgoth.pairing.MacMahonSolver
import org.jeudego.pairgoth.pairing.SwissSolver
import java.util.Random

sealed class Pairing(val type: PairingType, val weights: Weights = Weights()) {
    companion object {}
    enum class PairingType { SWISS, MACMAHON, ROUNDROBIN }
    data class Weights(
        val played: Double =    1_000_000.0, // players already met
        val group: Double =       100_000.0, // different group
        val handicap: Double =     50_000.0, // for each handicap stone
        val score: Double =        10_000.0, // per difference of score or MMS
        val place: Double =         1_000.0, // per difference of expected position for Swiss
        val color: Double =           500.0, // per color unbalancing
        val club: Double =            100.0, // same club weight
        val country: Double =          50.0  // same country
    )

    abstract fun pair(tournament: Tournament<*>, round: Int, pairables: List<Pairable>): List<Game>
}

fun Tournament<*>.historyBefore(round: Int) =
    if (lastRound() == 0) emptyList()
    else (0 until round).flatMap { games(round).values }

class Swiss(
    var method: Method,
    var firstRoundMethod: Method = method,
): Pairing(SWISS, Weights(
    handicap = 0.0, // no handicap games anyway
    club = 0.0,
    country = 0.0
)) {
    enum class Method { SPLIT_AND_FOLD, SPLIT_AND_RANDOM, SPLIT_AND_SLIP }
    override fun pair(tournament: Tournament<*>, round: Int, pairables: List<Pairable>): List<Game> {
        val actualMethod = if (round == 1) firstRoundMethod else method
        return SwissSolver(tournament.historyBefore(round), pairables, weights, actualMethod).pair()
    }
}

class MacMahon(
    var bar: Int = 0,
    var minLevel: Int = -30,
    var reducer: Int = 1
): Pairing(MACMAHON) {
    val groups = mutableListOf<Int>()

    override fun pair(tournament: Tournament<*>, round: Int, pairables: List<Pairable>): List<Game> {
        return MacMahonSolver(tournament.historyBefore(round), pairables, weights, mmBase = minLevel, mmBar = bar, reducer = reducer).pair()
    }
}

class RoundRobin: Pairing(ROUNDROBIN) {
    override fun pair(tournament: Tournament<*>, round: Int, pairables: List<Pairable>): List<Game> {
        TODO()
    }
}

// Serialization

fun Pairing.Companion.fromJson(json: Json.Object) = when (json.getString("type")?.let { Pairing.PairingType.valueOf(it) } ?: badRequest("missing pairing type")) {
    SWISS -> Swiss(
        method = json.getString("method")?.let { Swiss.Method.valueOf(it) } ?: badRequest("missing pairing method"),
        firstRoundMethod = json.getString("firstRoundMethod")?.let { Swiss.Method.valueOf(it) } ?: json.getString("method")!!.let { Swiss.Method.valueOf(it) }
    )
    MACMAHON -> MacMahon(
        bar = json.getInt("bar") ?: 0,
        minLevel = json.getInt("minLevel") ?: -30,
        reducer = json.getInt("reducer") ?: 1
    )
    ROUNDROBIN -> RoundRobin()
}

fun Pairing.toJson() = when (this) {
    is Swiss ->
        if (method == firstRoundMethod) Json.Object("type" to type.name, "method" to method.name)
        else Json.Object("type" to type.name, "method" to method.name, "firstRoundMethod" to firstRoundMethod.name)
    is MacMahon -> Json.Object("type" to type.name, "bar" to bar, "minLevel" to minLevel, "reducer" to reducer)
    is RoundRobin -> Json.Object("type" to type.name)
}

