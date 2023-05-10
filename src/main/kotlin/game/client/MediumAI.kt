package game.client

import game.shared.*
import java.net.InetAddress
import kotlin.random.Random

// Make a little harder
class MediumAI(address: InetAddress) : AI(address) {
    private val chatBot = AIChatBot(Random.nextDouble(), Random.nextDouble(), Random.nextDouble(), Random.nextDouble())

    private val revealed = mutableMapOf<People, MutableSet<Card>>()

    private var currWinGuess: Triple<People, Weapons, Rooms>? = null

    private val tricklessness = Random.nextInt(2,6)

    init {
        for (player in players) {
            revealed[player.person] = mutableSetOf()
        }
    }

    private fun getKnownCards(useMyCards: Boolean): Array<Card> {
        val knownCards = mutableSetOf<Card>()

        if (useMyCards) {
            knownCards.addAll(myCards)
        }

        knownCards.addAll(publicCards)
        for (event in history) {
            if (event is RumorEvent) {
                if (event.response?.second != null) {
                    knownCards.add(event.response.second!!)
                }
            }
        }

        return knownCards.toTypedArray()
    }

    private fun getCurrGuess(): Triple<People, Weapons, Rooms>? {
        val possiblePeople = People.values().toMutableList()
        val possibleWeapons = Weapons.values().toMutableList()
        val possibleRooms = Rooms.values().toMutableList()

        for (card in getKnownCards(true)) {
            if (card is PersonCard) {
                possiblePeople.remove(card.person)
            }

            if (card is WeaponCard) {
                possibleWeapons.remove(card.weapon)
            }

            if (card is RoomCard) {
                possibleRooms.remove(card.room)
            }
        }

        return if (possiblePeople.size == 1 && possibleWeapons.size == 1 && possibleRooms.size == 1) {
            Triple(possiblePeople[0], possibleWeapons[0], possibleRooms[0])
        } else {
            null
        }
    }

    override fun onChat(chatMessage: ChatMessage) {
        if (chatMessage.person != self) {
            val response = chatBot.getOtherChatResponse(chatMessage.person, chatMessage.text)
            if (response != null) {
                sendChatMessage(response)
            }
        }
    }

    override fun pick(): People {
        return peopleLeft.random()
    }

    override fun getMove(moves: Int): Location {
        val text = chatBot.getThinkingRemark()
        if (text != null) {
            sendChatMessage(text)
        }

        val possibleMoves = Board.getPossibleMoves(players.find { it.person == self }!!.location, moves, players).toMutableList()

        val possibleRooms = mutableListOf<RoomLocation>()
        for (move in possibleMoves) {
            if (move is RoomLocation) {
                possibleRooms.add(move)
            }
        }

        val cluedoRoom = possibleRooms.find { it.room == null }
        if (cluedoRoom != null) {
            currWinGuess = getCurrGuess()
            if (currWinGuess != null) {
                return cluedoRoom
            } else {
                possibleRooms.remove(cluedoRoom)
                possibleMoves.remove(cluedoRoom)
            }
        }

        return if (possibleRooms.size > 0) {
            possibleRooms.random()
        } else {
            possibleMoves.random()
        }
    }

    override fun startRumor(): Pair<People, Weapons> {
        val possiblePeople = People.values().toMutableList()
        val possibleWeapons = Weapons.values().toMutableList()

        val knownCards = getKnownCards(Random.nextInt(tricklessness) == 0)
        knownCards.shuffle()

        for (card in knownCards) {
            if (card is PersonCard) {
                possiblePeople.remove(card.person)
            }

            if (card is WeaponCard) {
                possibleWeapons.remove(card.weapon)
            }
        }

        val person = if (possiblePeople.size > 0) {
            possiblePeople.random()
        } else {
            People.values().random()
        }

        val weapon = if (possiblePeople.size > 0) {
            possibleWeapons.random()
        } else {
            Weapons.values().random()
        }

        return Pair(person, weapon)
    }

    override fun getCounterExample(): Card {
        val text = chatBot.getRumorResponse(currRumor!!.person, currRumor!!.weapon, currRumor!!.room)
        if (text != null) {
            sendChatMessage(text)
        }

        val counterExamples = mutableListOf<Card>()

        for (card in myCards) {
            if (card is PersonCard) {
                if (card.person == currRumor!!.person) {
                    counterExamples.add(card)
                }
            }

            if (card is WeaponCard) {
                if (card.weapon == currRumor!!.weapon) {
                    counterExamples.add(card)
                }
            }

            if (card is RoomCard) {
                if (card.room == currRumor!!.room) {
                    counterExamples.add(card)
                }
            }
        }

        val currRevealed = revealed[currRumor!!.rumorStarter]!!
        val unrevealedCounterExamples = mutableListOf<Card>()
        unrevealedCounterExamples.addAll(counterExamples)
        for (counterExample in counterExamples) {
            if (counterExample in currRevealed) {
                unrevealedCounterExamples.remove(counterExample)
            }
        }

        val card: Card
        if (unrevealedCounterExamples.size != 0) {
            card = unrevealedCounterExamples.random()
            currRevealed.add(card)
        } else {
            card = counterExamples.random()
        }

        return card
    }

    override fun getGuess(): Triple<People, Weapons, Rooms> {
        return currWinGuess!!
    }

    override fun onElimination(person: People) {
        if (person != self) {
            val text = chatBot.getLoseResponse(person)
            if (text != null) {
                sendChatMessage(text)
            }
        }
    }
}