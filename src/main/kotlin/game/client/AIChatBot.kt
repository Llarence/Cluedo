package game.client

import game.shared.People
import game.shared.Rooms
import game.shared.Weapons
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class AIChatBot(private val chattiness: Double, private val toxicity: Double, private val confidence: Double, private val humor: Double) {
    private fun<T> getWeightedRandom(array: Array<Pair<T, Double>>): T {
        val rand = Random.nextDouble(0.0, array.sumOf { it.second })

        var currSum = 0.0
        for (item in array) {
            currSum += item.second

            if (currSum >= rand) {
                return item.first
            }
        }

        return array.last().first
    }

    private fun infoToValue(textChattiness: Double, textToxicity: Double, textConfidence: Double, textHumor: Double, multiplier: Double): Double {
        return sqrt(((textChattiness - chattiness).pow(2) + (textToxicity - toxicity).pow(2) + (textConfidence - confidence).pow(2) + (textHumor - humor).pow(2))) * multiplier
    }

    fun getThinkingRemark(): String? {
        if (chattiness > Random.nextDouble().pow(2)) {
            return null
        }

        return getWeightedRandom(arrayOf(
            "Hmm" to infoToValue(0.5, 0.1, 0.3, 0.3, 3.0),
            "Ruh roh" to infoToValue(0.5, 0.2, 0.1, 0.3, 1.0),
            "This is deep" to infoToValue(0.7, 0.3, 0.5, 0.7, 1.0),
            "What to do" to infoToValue(0.7, 0.1, 0.2, 0.5, 1.0),
            "I got dis" to infoToValue(0.5, 0.7, 0.8, 0.6, 1.0),
            "XD" to infoToValue(0.5, 0.2, 0.4, 0.7, 1.0)))
    }

    fun getOtherChatResponse(person: People, text: String): String? {
        if (chattiness > Random.nextDouble().pow(6)) {
            return null
        }

        return getWeightedRandom(arrayOf(
            "True" to infoToValue(0.3, 0.2, 0.6, 0.3, 1.0),
            "You're wrong" to infoToValue(0.8, 0.7, 0.8, 0.5, 1.0),
            "No" to infoToValue(0.3, 0.5, 0.6, 0.4, 1.0),
            "$person is so wrong" to infoToValue(0.4, 0.8, 0.9, 0.5, 1.0),
            "\"$text\" what is this nonsense" to infoToValue(0.8, 0.8, 0.8, 0.4, 1.0),
            "That's deep" to infoToValue(0.6, 0.3, 0.4, 0.8, 1.0)))
    }

    fun getRumorResponse(person: People, weapon: Weapons, room: Rooms): String? {
        if (chattiness > Random.nextDouble().pow(2)) {
            return null
        }

        return getWeightedRandom(arrayOf(
            "GG" to infoToValue(0.7, 0.5, 0.7, 0.8, 1.0),
            "You're so wrong it is not $person" to infoToValue(0.8, 0.8, 0.7, 0.5, 1.0),
            "Haw Haw" to infoToValue(0.3, 0.5, 0.6, 0.7, 1.0),
            "$weapon" to infoToValue(0.4, 0.4, 0.4, 0.6, 1.0),
            "$room" to infoToValue(0.4, 0.4, 0.4, 0.6, 1.0),
            "Interesting" to infoToValue(0.3, 0.1, 0.7, 0.2, 1.0),
            "I know now" to infoToValue(0.6, 0.7, 0.7, 0.4, 1.0)))
    }

    fun getLoseResponse(loser: People): String? {
        if (chattiness > Random.nextDouble()) {
            return null
        }

        return getWeightedRandom(arrayOf(
            "GG" to infoToValue(0.1, 0.3, 0.3, 0.0, 3.0),
            "GG $loser" to infoToValue(0.5, 0.4, 0.4, 0.0, 3.0),
            "Haw Haw" to infoToValue(0.4, 0.8, 0.6, 0.7, 1.0),
            "Take the L $loser" to infoToValue(0.7, 0.9, 0.7, 0.0, 1.0),
            "That could have been me" to infoToValue(0.8, 0.1, 0.2, 0.0, 1.0),
            "Bruh" to infoToValue(0.2, 0.7, 0.4, 0.7, 1.0),
            "Lol" to infoToValue(0.2, 0.7, 0.6, 0.7, 1.0),
            "Aww man $loser was good" to infoToValue(0.8, 0.1, 0.4, 0.3, 1.0)))
    }
}