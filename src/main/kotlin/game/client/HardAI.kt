package game.client

import game.shared.*
import java.net.InetAddress
import kotlin.math.pow
import kotlin.random.Random

class PathNode(val location: Location, val prev: PathNode?)

class HardAI(address: InetAddress) : AI(address) {
    private val chatBot = AIChatBot(Random.nextDouble(), Random.nextDouble().pow(2), Random.nextDouble().pow(2), Random.nextDouble())

    private val handSize = myCards.size / players.size

    private val knownsNot = mutableSetOf(*publicCards)
    private var knowns: Triple<People?, Weapons?, Rooms?> = Triple(null, null, null)
    private val cardsHas = mutableMapOf<People, MutableSet<Card>>()
    private val cardsNoHas = mutableMapOf<People, MutableSet<Card>>()

    private val revealed = mutableMapOf<People, MutableSet<Card>>()

    init {
        for (player in players) {
            revealed[player.person] = mutableSetOf()
        }
    }

    private val rollExpectation = Random.nextInt(4, 10)
    private val movementToScore = Random.nextInt(0, 3) * players.size

    private lateinit var pickedRumor: Pair<People, Weapons>

    private fun addCardHas(person: People, card: Card) {
        knownsNot.add(card)
        cardsHas[person]!!.add(card)
        for (otherPlayer in players) {
            if (otherPlayer.person != person) {
                cardsNoHas[otherPlayer.person]!!.add(card)
            }
        }
    }

    private fun updateKnowledge() {
        for (player in players) {
            val playerKnowns = mutableSetOf<Card>()

            if (player.person == self) {
                knownsNot.addAll(myCards)
                playerKnowns.addAll(myCards)
            }

            cardsHas[player.person] = playerKnowns
            for (otherPlayer in players) {
                if (otherPlayer.person != player.person) {
                    cardsNoHas[otherPlayer.person]!!.addAll(playerKnowns)
                }
            }

            cardsNoHas[player.person] = mutableSetOf(*publicCards)
        }

        // More deductions available
        outer@while (true) {
            val sizeKnownsNot = knownsNot.size
            val sizeKnowns = if (knowns.first != null) { 1 } else { 0 } + if (knowns.second != null) { 1 } else { 0 } + if (knowns.third != null) { 1 } else { 0 }
            val sizeCardsHas = mutableMapOf<People, Int>()
            val sizeCardsNoHas = mutableMapOf<People, Int>()
            for (player in players) {
                sizeCardsHas[player.person] = cardsHas[player.person]!!.size
                sizeCardsNoHas[player.person] = cardsNoHas[player.person]!!.size
            }

            for (event in history) {
                if (event is RumorEvent) {
                    if (event.response != null) {
                        if (event.response.second != null) {
                            addCardHas(event.response.first, event.response.second!!)
                        } else {
                            val personCard = PersonCard(event.person)
                            val weaponCard = WeaponCard(event.weapon)
                            val roomCard = RoomCard(event.room)

                            val personNoHas = cardsNoHas[event.response.first]!!.contains(personCard)
                            val weaponNoHas = cardsNoHas[event.response.first]!!.contains(weaponCard)
                            val roomNoHas = cardsNoHas[event.response.first]!!.contains(roomCard)

                            if (weaponNoHas && roomNoHas) {
                                addCardHas(event.response.first, personCard)
                            }

                            if (personNoHas && roomNoHas) {
                                addCardHas(event.response.first, weaponCard)
                            }

                            if (personNoHas && weaponNoHas) {
                                addCardHas(event.response.first, roomCard)
                            }
                        }
                    }

                    val starterIndex = players.indexOfLast { it.person == event.rumorStarter }
                    for (i in starterIndex + 1 until starterIndex + players.size) {
                        val person = players[i].person

                        if (person == event.response?.first) {
                            break
                        }

                        cardsNoHas[person]!!.add(PersonCard(event.person))
                        cardsNoHas[person]!!.add(WeaponCard(event.weapon))
                        cardsNoHas[person]!!.add(RoomCard(event.room))
                    }
                } else {
                    event as GuessEvent

                    val personCorrect = event.person == knowns.first
                    val weaponCorrect = event.weapon == knowns.second
                    val roomCorrect = event.room == knowns.third

                    if (weaponCorrect && roomCorrect) {
                        knownsNot.add(PersonCard(event.person))
                    }

                    if (personCorrect && roomCorrect) {
                        knownsNot.add(WeaponCard(event.weapon))
                    }

                    if (personCorrect && weaponCorrect) {
                        knownsNot.add(RoomCard(event.room))
                    }
                }
            }

            for (player in players) {
                val currCardsHas = cardsHas[player.person]!!
                val currCardsNoHas = cardsNoHas[player.person]!!
                if (currCardsHas.size == handSize) {
                    for (card in allCards) {
                        if (card !in currCardsHas) {
                            currCardsNoHas.add(card)
                        }
                    }
                } else if (currCardsNoHas.size == allCards.size - handSize) {
                    for (card in allCards) {
                        if (card !in currCardsNoHas) {
                            addCardHas(player.person, card)
                        }
                    }
                }
            }

            for (card in allCards) {
                if (card in publicCards) {
                    break
                }

                for (player in players) {
                    if (card !in cardsNoHas[player.person]!!) {
                        break
                    }
                }

                knownsNot.add(card)
            }

            val possiblePeople = People.values().toMutableList()
            val possibleWeapons = Weapons.values().toMutableList()
            val possibleRooms = Rooms.values().toMutableList()

            for (card in knownsNot) {
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

            val person = if (possiblePeople.size == 1) {
                possiblePeople[0]
            } else {
                null
            }

            val weapon = if (possibleWeapons.size == 1) {
                possibleWeapons[0]
            } else {
                null
            }

            val room = if (possibleRooms.size == 1) {
                possibleRooms[0]
            } else {
                null
            }

            // Could check if this is all not null and end it, but nah
            knowns = Triple(person, weapon, room)

            if (person != null) {
                val personCard = PersonCard(person)
                for (player in players) {
                    cardsNoHas[player.person]!!.add(personCard)
                }
            }

            if (weapon != null) {
                val weaponCard = WeaponCard(weapon)
                for (player in players) {
                    cardsNoHas[player.person]!!.add(weaponCard)
                }
            }

            if (room != null) {
                val roomCard = RoomCard(room)
                for (player in players) {
                    cardsNoHas[player.person]!!.add(roomCard)
                }
            }

            if (sizeKnownsNot != knownsNot.size) {
                continue
            }

            if (sizeKnowns != if (knowns.first != null) { 1 } else { 0 } + if (knowns.second != null) { 1 } else { 0 } + if (knowns.third != null) { 1 } else { 0 }) {
                continue
            }

            for (player in players) {
                if (sizeCardsHas[player.person] != cardsHas[player.person]!!.size) {
                    continue@outer
                }

                if (sizeCardsNoHas[player.person] != cardsNoHas[player.person]!!.size) {
                    continue@outer
                }
            }

            break
        }
    }

    private fun generateShortestPath(startLocation: Location, endLocation: Location, startMoves: Int): Pair<Location, Int>? {
        val startAndSame = if (startLocation is PointLocation && endLocation is PointLocation) {
            startLocation.x == endLocation.x && startLocation.y == endLocation.y
        } else {
            false
        }

        if (startAndSame) {
            return Pair(startLocation, 0)
        }

        val currNodes = mutableListOf(PathNode(startLocation, null))
        val newNodes = mutableListOf<PathNode>()

        var moves = 1
        while (true) {
            for (node in currNodes) {
                val currMoves = if (moves == 1) {
                    startMoves
                } else {
                    rollExpectation
                }

                val currPlayers = if (moves == 1) {
                    players
                } else {
                    arrayOf()
                }

                val currNewNodes = mutableListOf<PathNode>()
                for (location in Board.getPossibleMoves(node.location, currMoves, currPlayers)) {
                    val isEnd = if (location is PointLocation && endLocation is PointLocation) {
                        location.x == endLocation.x && location.y == endLocation.y
                    } else if (location is RoomLocation && endLocation is RoomLocation) {
                        location.room == endLocation.room
                    } else {
                        false
                    }

                    if (isEnd) {
                        var curr = node
                        while (true) {
                            if (curr.prev == null) {
                                if (moves == 1) {
                                    var occupied = false
                                    if (curr.location is PointLocation) {
                                        for (testPlayer in players) {
                                            if (testPlayer.location is PointLocation) {
                                                val testLocation: PointLocation = testPlayer.location

                                                if ((curr.location as PointLocation).x == testLocation.x && (curr.location as PointLocation).y == testLocation.x) {
                                                    occupied = true
                                                }
                                            }
                                        }
                                    }

                                    if (occupied) {
                                        return null
                                    }
                                }

                                return Pair(curr.location, moves)
                            }

                            curr = node.prev!!
                        }
                    }

                    currNewNodes.add(PathNode(location, node))
                }

                newNodes.addAll(currNewNodes)
            }

            moves++
        }
    }

    private fun evaluateRumor(rumorPerson: People, rumorWeapon: Weapons, rumorRoom: Rooms): Double {
        val personCard = PersonCard(rumorPerson)
        val weaponCard = WeaponCard(rumorWeapon)
        val roomCard = RoomCard(rumorRoom)
        val cards = setOf(personCard, weaponCard, roomCard)

        var score = 0.0
        var probability = 1.0
        val starterIndex = players.indexOfLast { it.person == self }
        for (i in starterIndex + 1 until starterIndex + players.size) {
            val person = players[i].person

            val currCardsHas = cardsHas[person]!!
            if (cardsHas[person]!!.any { it in cards }) {
                break
            }

            // I think the probabilities work
            val currCardsNoHas = cardsNoHas[person]!!
            val slots = handSize - currCardsHas.size
            val peopleSize = People.values().size
            val weaponSize = Weapons.values().size
            val roomSize =  Rooms.values().size
            val totalCardsAvailable = (peopleSize + weaponSize + roomSize - currCardsHas.size - currCardsNoHas.size).toDouble()
            val probabilityIndividual = (1.0 - (1 / totalCardsAvailable)).pow(slots)
            val cardsUnknown = 3 - cardsNoHas[person]!!.intersect(cards).size

            probability *= probabilityIndividual.pow(cardsUnknown)
            // Could add some score for there being a probability that the card is found to be in cardsHas, but nah
            score += cardsUnknown * probability
        }

        return score
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

        updateKnowledge()

        if (knowns.first != null && knowns.second != null && knowns.third != null) {
            return generateShortestPath(players.find { it.person == self }!!.location, RoomLocation(null), moves)!!.first
        }

        val rumorAndValue = mutableListOf<Pair<Triple<People, Weapons, Rooms>, Double>>()
        for (person in People.values()) {
            for (room in Rooms.values()) {
                for (weapon in Weapons.values()) {
                    rumorAndValue.add(Pair(Triple(person, weapon, room), evaluateRumor(person, weapon, room)))
                }
            }
        }

        rumorAndValue.shuffle()
        rumorAndValue.sortByDescending { it.second }

        var bestRumorAndValueAndLocation: Triple<Triple<People, Weapons, Rooms>, Double, Location>? = null
        while (true) {
            val currRumorAndValue = rumorAndValue[0]

            if (bestRumorAndValueAndLocation != null) {
                if (bestRumorAndValueAndLocation.second > currRumorAndValue.second) {
                    break
                }
            }

            val path = generateShortestPath(players.find { it.person == self }!!.location, RoomLocation(currRumorAndValue.first.third), moves)
            if (path != null) {
                val value = currRumorAndValue.second - path.second * movementToScore

                if (bestRumorAndValueAndLocation == null || bestRumorAndValueAndLocation.second < value) {
                    bestRumorAndValueAndLocation = Triple(currRumorAndValue.first, value, path.first)
                }
            }

            rumorAndValue.removeAt(0)

            if (rumorAndValue.size == 0) {
                break
            }
        }

        pickedRumor = Pair(bestRumorAndValueAndLocation!!.first.first, bestRumorAndValueAndLocation.first.second)
        return bestRumorAndValueAndLocation.third
    }

    override fun startRumor(): Pair<People, Weapons> {
        return pickedRumor
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
        return Triple(knowns.first!!, knowns.second!!, knowns.third!!)
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