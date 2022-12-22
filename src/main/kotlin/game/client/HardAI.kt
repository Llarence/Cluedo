package game.client

import game.shared.*
import java.net.InetAddress
import kotlin.math.pow
import kotlin.random.Random

class Knowledge(val knowns: Array<Card>, val revealed: Array<Card>, val cardsHas: Map<People, Array<Card>>, val cardsNoHas: Map<People, Array<Card>>)

class PathNode(val location: Location, val prev: PathNode?)

class HardAI(address: InetAddress) : AI(address) {
    private val chatBot = AIChatBot(Random.nextDouble(), Random.nextDouble().pow(2), Random.nextDouble().pow(2), Random.nextDouble())

    private lateinit var knowledge: Knowledge

    private val rollExpectation = Random.nextInt(4, 10)

    private fun updateKnowledge() {
        val knownCards = mutableSetOf(*publicCards)
        val revealed = mutableSetOf<Card>()
        val cardsHas = mutableMapOf<People, MutableSet<Card>>()
        val cardsNoHas = mutableMapOf<People, MutableSet<Card>>()
        for (player in players) {
            val knowns = mutableSetOf<Card>()

            if (player.person == self) {
                knownCards.addAll(myCards)
                knowns.addAll(myCards)
            }

            cardsHas[player.person] = knowns
            for (otherPlayer in players) {
                if (otherPlayer.person != player.person) {
                    cardsNoHas[otherPlayer.person]!!.addAll(knowns)
                }
            }

            cardsNoHas[player.person] = mutableSetOf(*publicCards)
        }

        // More deductions available
        // Like if player hand full then their cardsNoHas is also known
        var prevSize = knownCards.size
        while (true) {
            for (event in history) {
                if (event is RumorEvent) {
                    if (event.response != null) {
                        knownCards.add(event.response.second)
                        cardsHas[event.response.first]!!.add(event.response.second)
                        for (otherPlayer in players) {
                            if (otherPlayer.person != event.response.first) {
                                cardsNoHas[otherPlayer.person]!!.add(event.response.second)
                            }
                        }

                        if (event.response.first == self) {
                            revealed.add(event.response.second)
                        }
                    }

                    val starterIndex = players.indexOfLast { it.person == event.rumorStarter }
                    for (i in starterIndex + 1 until starterIndex + players.size) {
                        val person = players[i].person

                        if (person == event.response?.first) {
                            break
                        }

                        val personCard = PersonCard(event.person)
                        val weaponCard = WeaponCard(event.weapon)
                        val roomCard = RoomCard(event.room)

                        val personIn = cardsHas[person]!!.contains(personCard)
                        val weaponIn = cardsHas[person]!!.contains(weaponCard)
                        val roomIn = cardsHas[person]!!.contains(roomCard)

                        if (weaponIn && roomIn) {
                            knownCards.add(personCard)
                            cardsNoHas[person]!!.add(personCard)
                        }

                        if (personIn && roomIn) {
                            knownCards.add(weaponCard)
                            cardsNoHas[person]!!.add(weaponCard)
                        }

                        if (personIn && weaponIn) {
                            knownCards.add(roomCard)
                            cardsNoHas[person]!!.add(roomCard)
                        }
                    }
                } else {
                    event as GuessEvent

                    val personCard = PersonCard(event.person)
                    val weaponCard = WeaponCard(event.weapon)
                    val roomCard = RoomCard(event.room)

                    val personIn = knownCards.contains(personCard)
                    val weaponIn = knownCards.contains(weaponCard)
                    val roomIn = knownCards.contains(roomCard)

                    if (weaponIn && roomIn) {
                        knownCards.add(personCard)
                    }

                    if (personIn && roomIn) {
                        knownCards.add(weaponCard)
                    }

                    if (personIn && weaponIn) {
                        knownCards.add(roomCard)
                    }
                }
            }

            if (prevSize != knownCards.size) {
                break
            }
            prevSize = knownCards.size
        }

        val cardsHasArray = mutableMapOf<People, Array<Card>>()
        val cardsNoHasArray = mutableMapOf<People, Array<Card>>()
        for (player in players) {
            cardsHasArray[player.person] = cardsHas[player.person]!!.toTypedArray()
            cardsNoHasArray[player.person] = cardsHas[player.person]!!.toTypedArray()
        }

        knowledge = Knowledge(knownCards.toTypedArray(), revealed.toTypedArray(), cardsHasArray, cardsNoHasArray)
    }

    fun hasGuess(): Triple<People?, Weapons?, Rooms?> {
        val possiblePeople = People.values().toMutableList()
        val possibleWeapons = Weapons.values().toMutableList()
        val possibleRooms = Rooms.values().toMutableList()

        for (card in knowledge.knowns) {
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

        return Triple(if (possiblePeople.size == 1) { possiblePeople[0] } else { null }, if (possibleWeapons.size == 1) { possibleWeapons[0] } else { null }, if (possibleRooms.size == 1) { possibleRooms[0] } else { null })
    }

    private fun generateShortestPath(startLocation: Location, endLocation: Location, startMoves: Int): Pair<Location, Int>? {
        throw Exception("Fix me (null stuff)")

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
    }

    override fun startRumor(): Pair<People, Weapons> {
        
    }

    override fun getCounterExample(): Card {
    }

    override fun getGuess(): Triple<People, Weapons, Rooms> {
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