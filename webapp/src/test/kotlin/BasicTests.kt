package org.jeudego.pairgoth.test

import com.republicate.kson.Json
import org.junit.jupiter.api.MethodOrderer.MethodName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@TestMethodOrder(MethodName::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BasicTests: TestBase() {

    val aTournament = Json.Object(
        "type" to "INDIVIDUAL",
        "name" to "Mon Tournoi",
        "shortName" to "mon-tournoi",
        "startDate" to "2023-05-10",
        "endDate" to "2023-05-12",
        "country" to "FR",
        "location" to "Marseille",
        "online" to false,
        "timeSystem" to Json.Object(
            "type" to "FISCHER",
            "mainTime" to 1200,
            "increment" to 10
        ),
        "rounds" to 2,
        "pairing" to Json.Object(
            "type" to "SWISS",
            "method" to "SPLIT_AND_SLIP"
        )
    )

    val aTeamTournament = Json.Object(
        "type" to "TEAM2",
        "name" to "Mon Tournoi par équipes",
        "shortName" to "mon-tournoi-par-equipes",
        "startDate" to "2023-05-20",
        "endDate" to "2023-05-23",
        "country" to "FR",
        "location" to "Marseille",
        "online" to true,
        "timeSystem" to Json.Object(
            "type" to "FISCHER",
            "mainTime" to 1200,
            "increment" to 10
        ),
        "rounds" to 2,
        "pairing" to Json.Object(
            "type" to "MACMAHON"
        )
    )

    val aPlayer = Json.Object(
        "name" to "Burma",
        "firstname" to "Nestor",
        "rating" to 1600,
        "rank" to -5,
        "country" to "FR",
        "club" to "13Ma"
    )

    val anotherPlayer = Json.Object(
        "name" to "Poirot",
        "firstname" to "Hercule",
        "rating" to 1700,
        "rank" to -1,
        "country" to "FR",
        "club" to "75Op"
    )

    @Test
    fun `001 create tournament`() {
        val resp = TestAPI.post("/api/tour", aTournament) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
    }

    @Test
    fun `002 get tournament`() {
        val resp = TestAPI.get("/api/tour/1") as Json.Object
        assertEquals(1, resp.getInt("id"), "First tournament should have id #1")
        // filter out "id", and also "komi", "rules" and "gobanSize" which were provided by default
        val cmp = Json.Object(*resp.entries.filter { it.key !in listOf("id", "komi", "rules", "gobanSize") }.map { Pair(it.key, it.value) }.toTypedArray())
        assertEquals(aTournament.toString(), cmp.toString(), "tournament differs")
    }

    @Test
    fun `003 register user`() {
        val resp = TestAPI.post("/api/tour/1/part", aPlayer) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        val players = TestAPI.get("/api/tour/1/part") as Json.Array
        val player = players[0] as Json.Object
        assertEquals(1, player.getInt("id"), "First player should have id #1")
        // filter out "id"
        val cmp = Json.Object(*player.entries.filter { it.key != "id" }.map { Pair(it.key, it.value) }.toTypedArray())
        assertEquals(aPlayer.toString(), cmp.toString(), "player differs")
    }

    @Test
    fun `004 modify user`() {
        // remove player #1 from round #2
        val resp = TestAPI.put("/api/tour/1/part/1", Json.Object("skip" to Json.Array(2))) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        val player = TestAPI.get("/api/tour/1/part/1") as Json.Object
        assertEquals("[2]", player.getArray("skip").toString(), "First player should have id #1")
    }

    @Test
    fun `005 pair`() {
        val resp = TestAPI.post("/api/tour/1/part", anotherPlayer) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        var games = TestAPI.post("/api/tour/1/pair/1", Json.Array("all"))
        val possibleResults = setOf(
            """[{"id":1,"w":1,"b":2,"h":0,"r":"?"}]""",
            """[{"id":1,"w":2,"b":1,"h":0,"r":"?"}]"""
        )
        assertTrue(possibleResults.contains(games.toString()), "pairing differs")
        games = TestAPI.get("/api/tour/1/res/1") as Json.Array
        assertTrue(possibleResults.contains(games.toString()), "results differs")
        val empty = TestAPI.get("/api/tour/1/pair/1") as Json.Array
        assertEquals("[]", empty.toString(), "no more pairables for round 1")
    }

    @Test
    fun `006 result`() {
        val resp = TestAPI.put("/api/tour/1/res/1", Json.parse("""{"id":1,"result":"b"}""")) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        val games = TestAPI.get("/api/tour/1/res/1")
        val possibleResults = setOf(
            """[{"id":1,"w":1,"b":2,"h":0,"r":"b"}]""",
            """[{"id":1,"w":2,"b":1,"h":0,"r":"b"}]"""
        )
        assertTrue(possibleResults.contains(games.toString()), "results differ")
    }

    @Test
    fun `007 team tournament, MacMahon`() {
        var resp = TestAPI.post("/api/tour", aTeamTournament) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        assertEquals(2, resp.getInt("id"), "expecting id #2 for new tournament")
        resp = TestAPI.post("/api/tour/2/part", aPlayer) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        assertEquals(3, resp.getInt("id"), "expecting id #3 for new player")
        resp = TestAPI.post("/api/tour/2/part", anotherPlayer) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        assertEquals(4, resp.getInt("id"), "expecting id #{ for new player")
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        var arr = TestAPI.get("/api/tour/2/pair/1") as Json.Array
        assertEquals("[]", arr.toString(), "expecting an empty array")
        resp = TestAPI.post("/api/tour/2/team", Json.parse("""{ "name":"The Buffallos", "players":[3, 4] }""") as Json.Object) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        assertEquals(5, resp.getInt("id"), "expecting team id #5")
        resp = TestAPI.get("/api/tour/2/team/5") as Json.Object
        assertEquals("""{"id":5,"name":"The Buffallos","players":[3,4]}""", resp.toString(), "expecting team description")
        arr = TestAPI.get("/api/tour/2/pair/1") as Json.Array
        assertEquals("[5]", arr.toString(), "expecting a singleton array")
        // nothing stops us in reusing players in different teams, at least for now...
        resp = TestAPI.post("/api/tour/2/team", Json.parse("""{ "name":"The Billies", "players":[3, 4] }""") as Json.Object) as Json.Object
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        assertEquals(6, resp.getInt("id"), "expecting team id #6")
        arr = TestAPI.get("/api/tour/2/pair/1") as Json.Array
        assertEquals("[5,6]", arr.toString(), "expecting two pairables")
        arr = TestAPI.post("/api/tour/2/pair/1", Json.parse("""["all"]""")) as Json.Array
        assertTrue(resp.getBoolean("success") == true, "expecting success")
        val expected = """"["id":1,"w":5,"b":6,"h":3,"r":"?"]"""
    }
}
