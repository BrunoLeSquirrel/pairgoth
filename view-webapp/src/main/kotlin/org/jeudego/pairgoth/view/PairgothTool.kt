package org.jeudego.pairgoth.view

import com.republicate.kson.Json

/**
 * Generic utilities
 */

class PairgothTool {
    fun toMap(array: Json.Array) = array.map { ser -> ser as Json.Object }.associateBy { it.getLong("id")!! }

    fun getCriteria() = mapOf(
        "NONE" to "No tie break", // No ranking / tie-break

        "CATEGORY" to "Category",
        "RANK" to "Rank",
        "RATING" to "Rating",
        "NBW" to "Number of wins", // Number win
        "MMS" to "Mac Mahon score", // Macmahon score
        "STS" to "Strasbourg score", // Strasbourg score
        "CPS" to "Cup score", // Cup score

        "SOSW" to "Sum of opponents wins", // Sum of opponents NBW
        "SOSWM1" to "Sum of opponents wins minus 1", //-1
        "SOSWM2" to "Sum of opponents wins minus 2", //-2
        "SODOSW" to "Sum of defeated opponents wins", // Sum of defeated opponents NBW
        "SOSOSW" to "Sum of opponents SOSW", // Sum of opponent SOS
        "CUSSW" to "Cumulative sum of opponents wins", // Cumulative sum of scores (NBW)

        "SOSM" to "Sum of opponents Mac Mahon score", // Sum of opponents McMahon score
        "SOSMM1" to "Sum of opponents Mac Mahon score minus 1", // Same as previous group but with McMahon score
        "SOSMM2" to "Sum of opponents Mac Mahon score minus 2",
        "SODOSM" to "Sum of defeated opponents Mac Mahon score",
        "SOSOSM" to "Sum of opponents SOSM",
        "CUSSM" to "Cumulative sum of opponents Mac Mahon score",

        "SOSTS" to "Sum of opponents Strasbourg score", // Sum of opponnents Strasbourg score

        "EXT" to "Attempted achievements", // Exploits tentes
        "EXR" to "Succeeded achievements", // Exploits reussis

        // For the two criteria below see the user documentation
        "SDC" to "Simplified direct confrontation", // Simplified direct confrontation
        "DC" to "Direct confrontation", // Direct confrontation
    )
}