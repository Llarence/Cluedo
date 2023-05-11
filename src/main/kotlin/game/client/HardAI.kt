package game.client

import game.shared.*
import java.net.InetAddress
import kotlin.math.pow
import kotlin.random.Random

class PathNode(val location: Location, val prev: PathNode?)

class HardAI(address: InetAddress) : AI(address) {
    private val chatBot = AIChatBot(Random.nextDouble(), Random.nextDouble().pow(2), Random.nextDouble().pow(2), Random.nextDouble())

    private var knowledgeInited = false
    private val knownsNot = mutableSetOf<Card>()
    // This could be a list
    private var knowns: Triple<People?, Weapons?, Rooms?> = Triple(null, null, null)
    private val cardsHas = mutableMapOf<People, MutableSet<Card>>()
    private val cardsNoHas = mutableMapOf<People, MutableSet<Card>>()

    private var revealedInited = false
    private val revealed = mutableMapOf<People, MutableSet<Card>>()

    private val rollExpectation = Random.nextInt(4, 10)
    private var movementToScore = Random.nextInt(0, 3)

    private lateinit var pickedRumor: Pair<People, Weapons>

    private fun addCardHas(person: People, card: Card) {
        knownsNot.add(card)
        cardsHas[person]!!.add(card)
        for (otherPlayer in playingPlayers) {
            if (otherPlayer.person != person) {
                cardsNoHas[otherPlayer.person]!!.add(card)
            }
        }
    }

    // Could just add all cardsNoHas to knownsNot
    private fun updateKnowledge() {
        if (!knowledgeInited) {
            for (player in playingPlayers) {
                cardsHas[player.person] = mutableSetOf()
                cardsNoHas[player.person] = mutableSetOf(*publicCards)
            }

            knownsNot.addAll(publicCards)

            knownsNot.addAll(myCards)
            cardsHas[self]!!.addAll(myCards)
            for (otherPlayer in playingPlayers) {
                if (otherPlayer.person != self) {
                    cardsNoHas[otherPlayer.person]!!.addAll(myCards)
                }
            }

            knowledgeInited = true
        }

        // More deductions available
        outer2@while (true) {
            val sizeKnownsNot = knownsNot.size
            val sizeKnowns = if (knowns.first != null) { 1 } else { 0 } + if (knowns.second != null) { 1 } else { 0 } + if (knowns.third != null) { 1 } else { 0 }
            val sizeCardsHas = mutableMapOf<People, Int>()
            val sizeCardsNoHas = mutableMapOf<People, Int>()
            for (player in playingPlayers) {
                sizeCardsHas[player.person] = cardsHas[player.person]!!.size
                sizeCardsNoHas[player.person] = cardsNoHas[player.person]!!.size
            }

            for (event in history) {
                if (event is RumorEvent) {
                    val personCard = PersonCard(event.person)
                    val weaponCard = WeaponCard(event.weapon)
                    val roomCard = RoomCard(event.room)

                    if (event.response != null) {
                        if (event.response.second != null) {
                            addCardHas(event.response.first, event.response.second!!)
                        } else {
                            val personNoHas = personCard in cardsNoHas[event.response.first]!!
                            val weaponNoHas = weaponCard in cardsNoHas[event.response.first]!!
                            val roomNoHas = roomCard in cardsNoHas[event.response.first]!!

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

                    val starterIndex = playingPlayers.indexOfLast { it.person == event.rumorStarter }
                    for (i in starterIndex + 1 until starterIndex + playingPlayers.size) {
                        val person = playingPlayers[i % playingPlayers.size].person

                        if (person == event.response?.first) {
                            break
                        }

                        cardsNoHas[person]!!.add(personCard)
                        cardsNoHas[person]!!.add(weaponCard)
                        cardsNoHas[person]!!.add(roomCard)
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

            // Rework so it is valid maybe
            for (player in playingPlayers) {
                val currCardsHas = cardsHas[player.person]!!
                val currCardsNoHas = cardsNoHas[player.person]!!
                if (currCardsHas.size == myCards.size) {
                    for (card in allCards) {
                        if (card !in currCardsHas) {
                            currCardsNoHas.add(card)
                        }
                    }
                } else if (currCardsNoHas.size == allCards.size - myCards.size) {
                    for (card in allCards) {
                        if (card !in currCardsNoHas) {
                            addCardHas(player.person, card)
                        }
                    }
                }
            }

            var person: People? = null
            var weapon: Weapons? = null
            var room: Rooms? = null

            outer1@for (card in allCards) {
                if (card in publicCards) {
                    continue
                }

                for (player in playingPlayers) {
                    if (card !in cardsNoHas[player.person]!!) {
                        continue@outer1
                    }
                }

                if (card is PersonCard) {
                    person = card.person
                }

                if (card is WeaponCard) {
                    weapon = card.weapon
                }

                if (card is RoomCard) {
                    room = card.room
                }
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

            if (person == null) {
                person = if (possiblePeople.size == 1) {
                    possiblePeople[0]
                } else {
                    null
                }

                if (person != null) {
                    val personCard = PersonCard(person)
                    for (player in playingPlayers) {
                        cardsNoHas[player.person]!!.add(personCard)
                    }
                }
            }

            if (weapon == null) {
                weapon = if (possibleWeapons.size == 1) {
                    possibleWeapons[0]
                } else {
                    null
                }

                if (weapon != null) {
                    val weaponCard = WeaponCard(weapon)
                    for (player in playingPlayers) {
                        cardsNoHas[player.person]!!.add(weaponCard)
                    }
                }
            }

            if (room == null) {
                room = if (possibleRooms.size == 1) {
                    possibleRooms[0]
                } else {
                    null
                }

                if (room != null) {
                    val roomCard = RoomCard(room)
                    for (player in playingPlayers) {
                        cardsNoHas[player.person]!!.add(roomCard)
                    }
                }
            }

            // Could check if this is all not null and end it, but nah
            knowns = Triple(person, weapon, room)

            if (sizeKnownsNot != knownsNot.size) {
                continue
            }

            if (sizeKnowns != if (knowns.first != null) { 1 } else { 0 } + if (knowns.second != null) { 1 } else { 0 } + if (knowns.third != null) { 1 } else { 0 }) {
                continue
            }

            for (player in playingPlayers) {
                if (sizeCardsHas[player.person] != cardsHas[player.person]!!.size) {
                    continue@outer2
                }

                if (sizeCardsNoHas[player.person] != cardsNoHas[player.person]!!.size) {
                    continue@outer2
                }
            }

            break
        }
    }

    // Should optimize
    private fun generateShortestPath(startLocation: Location, endLocation: Location, startMoves: Int): Pair<Location, Int>? {
        if (startLocation is PointLocation && endLocation is PointLocation) {
            if (startLocation.x == endLocation.x && startLocation.y == endLocation.y) {
                return Pair(startLocation, 0)
            }
        }

        // Could have old nodes but I am too lazy
        val currNodes = mutableListOf(PathNode(startLocation, null))
        val newNodes = mutableListOf<PathNode>()

        var moves = 1
        while (true) {
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

            for (node in currNodes) {
                val possibleMoves = Board.getPossibleMoves(node.location, currMoves, currPlayers)
                possibleMoves.shuffle()
                for (location in possibleMoves) {
                    val isEnd = if (location is PointLocation && endLocation is PointLocation) {
                        location.x == endLocation.x && location.y == endLocation.y
                    } else if (location is RoomLocation && endLocation is RoomLocation) {
                        location.room == endLocation.room
                    } else {
                        false
                    }

                    if (isEnd) {
                        var prev = PathNode(location, node)
                        var curr = node
                        while (true) {
                            if (curr.prev == null) {
                                if (moves == 1) {
                                    var occupied = false
                                    if (prev.location is PointLocation) {
                                        for (testPlayer in players) {
                                            if (testPlayer.location is PointLocation) {
                                                val testLocation: PointLocation = testPlayer.location

                                                if ((prev.location as PointLocation).x == testLocation.x && (prev.location as PointLocation).y == testLocation.x) {
                                                    occupied = true
                                                }
                                            }
                                        }
                                    }

                                    if (occupied) {
                                        return null
                                    }
                                }

                                return Pair(prev.location, moves)
                            }

                            prev = curr
                            curr = curr.prev!!
                        }
                    }

                    newNodes.add(PathNode(location, node))
                }
            }

            currNodes.clear()
            currNodes.addAll(newNodes)
            newNodes.clear()

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
            val person = players[i % players.size].person

            if (!playingPlayers.any { it.person == person }) {
                continue
            }

            val currCardsHas = cardsHas[person]!!
            if (cardsHas[person]!!.any { it in cards }) {
                break
            }

            // I think the probabilities work
            val currCardsNoHas = cardsNoHas[person]!!
            val slots = myCards.size - currCardsHas.size
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

        if (knowns.first != null && knowns.second != null && knowns.third != null) {
            val location = generateShortestPath(players.find { it.person == self }!!.location, RoomLocation(null), moves)!!.first
            if (location is RoomLocation) {
                val room = location.room
                if (room != null) {
                    val bestRumor = rumorAndValue.first { it.first.third == room }
                    pickedRumor = Pair(bestRumor.first.first, bestRumor.first.second)
                }
            }

            return location
        }

        var i = 0
        var bestRumorAndValueAndLocation: Triple<Triple<People, Weapons, Rooms>, Double, Location>? = null
        while (true) {
            val currRumorAndValue = rumorAndValue[i]

            // Could also skip pathfinding if the rooms are the same
            if (bestRumorAndValueAndLocation != null) {
                if (bestRumorAndValueAndLocation.second > currRumorAndValue.second) {
                    break
                }
            }

            val path = generateShortestPath(players.find { it.person == self }!!.location, RoomLocation(currRumorAndValue.first.third), moves)
            if (path != null) {
                val value = currRumorAndValue.second - path.second * movementToScore * playingPlayers.size

                if (bestRumorAndValueAndLocation == null || bestRumorAndValueAndLocation.second < value) {
                    bestRumorAndValueAndLocation = Triple(currRumorAndValue.first, value, path.first)
                }
            }

            i++

            if (rumorAndValue.size == i) {
                break
            }
        }

        if (bestRumorAndValueAndLocation!!.third is RoomLocation) {
            val room = (bestRumorAndValueAndLocation.third as RoomLocation).room
            val bestRumor = rumorAndValue.first { it.first.third == room }
            pickedRumor = Pair(bestRumor.first.first, bestRumor.first.second)
        }

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

        if (!revealedInited) {
            for (player in playingPlayers) {
                revealed[player.person] = mutableSetOf()
            }

            revealedInited = true
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