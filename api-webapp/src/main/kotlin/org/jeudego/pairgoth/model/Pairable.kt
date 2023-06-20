package org.jeudego.pairgoth.model

import com.republicate.kson.Json
import org.jeudego.pairgoth.api.ApiHandler.Companion.badRequest
import org.jeudego.pairgoth.store.Store

// Pairable

sealed class Pairable(val id: ID, val name: String, open val rating: Int, open val rank: Int) {
    companion object {
        val MIN_RANK: Int = -30 // 30k
    }
    abstract fun toJson(): Json.Object
    abstract val club: String?
    abstract val country: String?
    open fun nameSeed(): String {
        return name
    }
    val skip = mutableSetOf<Int>() // skipped rounds
}

object ByePlayer: Pairable(0, "bye", 0, Int.MIN_VALUE) {
    override fun toJson(): Json.Object {
        throw Error("bye player should never be serialized")
    }

    override val club = "none"
    override val country = "none"
}

fun Pairable.displayRank() = if (rank < 0) "${-rank}k" else "${rank + 1}d"

private val rankRegex = Regex("(\\d+)([kd])", RegexOption.IGNORE_CASE)

fun Pairable.Companion.parseRank(rankStr: String): Int {
    val (level, letter) = rankRegex.matchEntire(rankStr)?.destructured ?: throw Error("invalid rank: $rankStr")
    val num = level.toInt()
    if (num < 0 || letter != "k" && letter != "K" && num > 9) throw Error("invalid rank: $rankStr")
    return when (letter.lowercase()) {
        "k" -> -num
        "d" -> num - 1
        else -> throw Error("impossible")
    }
}

// Player

class Player(
    id: ID,
    name: String,
    var firstname: String,
    rating: Int,
    rank: Int,
    override var country: String,
    override var club: String
): Pairable(id, name, rating, rank) {
    companion object
    // used to store external IDs ("FFG" => FFG ID, "EGF" => EGF PIN, "AGA" => AGA ID ...)
    val externalIds = mutableMapOf<String, String>()
    override fun toJson(): Json.Object = Json.MutableObject(
        "id" to id,
        "name" to name,
        "firstname" to firstname,
        "rating" to rating,
        "rank" to rank,
        "country" to country,
        "club" to club
    ).also {
        if (skip.isNotEmpty()) it["skip"] = Json.Array(skip)
    }
    override fun nameSeed(): String {
        return name + firstname
    }
}

fun Player.Companion.fromJson(json: Json.Object, default: Player? = null) = Player(
    id = json.getInt("id") ?: default?.id ?: Store.nextPlayerId,
    name = json.getString("name") ?: default?.name ?: badRequest("missing name"),
    firstname = json.getString("firstname") ?: default?.firstname ?: badRequest("missing firstname"),
    rating = json.getInt("rating") ?: default?.rating ?: badRequest("missing rating"),
    rank = json.getInt("rank") ?: default?.rank ?: badRequest("missing rank"),
    country = json.getString("country") ?: default?.country ?: badRequest("missing country"),
    club = json.getString("club") ?: default?.club ?: badRequest("missing club")
).also { player ->
    player.skip.clear()
    json.getArray("skip")?.let {
        if (it.isNotEmpty()) player.skip.addAll(it.map { id -> (id as Number).toInt() })
    }
}
