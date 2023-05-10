package game.server

import game.shared.*
import java.net.ServerSocket
import java.util.regex.Pattern
import kotlin.random.Random
import kotlin.random.nextInt

// Should only be used once
class Server(private val numPlayers: Int) {
    private val serverSocket = ServerSocket(5566)

    private val listenerConnections = mutableListOf<Connection>()
    private val players = mutableListOf<Triple<Connection?, PlayerData, Array<Card>?>>()
    private val eliminatedPlayers = mutableListOf<Triple<Connection?, PlayerData, Array<Card>?>>()
    private lateinit var truth: Triple<People, Weapons, Rooms>

    private val chatThreads = mutableListOf<Thread>()
    private val pattern = Pattern.compile("[A-Za-z0-9\".,'_ ]")

    private fun <T : Any?> tryUntil(function: () -> T): T {
        while (true) {
            try {
                return function()
            } catch (e: Exception) {
                if (e !is InterruptedException) {
                    println(e.stackTrace.toString())
                }
            }
        }
    }

    private fun sendAlert(data: Packet) {
        for (listener in listenerConnections) {
            listener.sendPacket(data)
        }
    }

    private fun runChat(player: Pair<Connection, PlayerData>) {
        try {
            while (true) {
                val chatMessage = tryUntil {
                    val chatMessage = player.first.receiveChatMessage()

                    // This doesn't really make sense, but maybe checks the unchecked cast
                    if (chatMessage.person != player.second.person) {
                        throw Exception("TextDoesn'tMatchSender")
                    }

                    if (chatMessage.text == "" || !pattern.matcher(chatMessage.text).find()) {
                        throw Exception("InvalidText")
                    }

                    chatMessage
                }

                for (listener in listenerConnections) {
                    listener.sendChatMessage(chatMessage.person, chatMessage.text)
                }
            }
        } catch (_: InterruptedException) {
        }
    }

    private fun start() {
        println("Starting sever")

        val connections = mutableListOf<Connection>()
        while (connections.size < numPlayers) {
            val connection = Connection(serverSocket.accept())
            val observerPacket = connection.receivePacket()

            if (observerPacket is IsObserver) {
                listenerConnections.add(connection)

                if (observerPacket.isObserver) {
                    println("Observer connected")
                } else {
                    connections.add(connection)

                    println("Player connected")
                }
            }
        }

        println("Everyone connected")

        connections.shuffle()

        val cards = genCards().toMutableList()
        cards.shuffle()

        // Could use find
        var truePerson: People? = null
        var trueWeapon: Weapons? = null
        var trueRoom: Rooms? = null
        var curr = 0
        while(true) {
            val card = cards[curr]
            if (card is PersonCard && truePerson == null) {
                truePerson = card.person
                cards.remove(card)
            }

            if (card is WeaponCard && trueWeapon == null) {
                trueWeapon = card.weapon
                cards.remove(card)
            }

            if (card is RoomCard && trueRoom == null) {
                trueRoom = card.room
                cards.remove(card)
            }

            if (truePerson != null && trueWeapon != null && trueRoom != null) {
                break
            }

            curr++
        }

        truth = Triple(truePerson!!, trueWeapon!!, trueRoom!!)

        println("Picked true story")

        val numPublicCards = cards.size % numPlayers
        val publicCards = mutableListOf<Card>()
        for (i in 0 until numPublicCards) {
            publicCards.add(cards[0])
            cards.removeAt(0)
        }

        val people = People.values().toMutableList()
        val hands = cards.chunked(cards.size / numPlayers)
        for (i in connections.indices) {
            val connection = connections[i]
            val person = tryUntil {
                connection.sendPacket(RequestPlayerPick())

                val person = (connection.receivePacket() as RespondPlayerPick).person

                if (person !in people) {
                    throw Exception("PersonTaken")
                }

                person
            }

            people.remove(person)
            sendAlert(AlertPlayerPick(person))

            println("$person picked")

            val hand = hands[i].toTypedArray()
            connection.sendPacket(AlertHand(hand))
            players.add(Triple(connection, Board.personToDataAtStart(person), hand))

            println("Dealt hand")
        }

        sendAlert(AlertPublicCards(publicCards.toTypedArray()))

        println("Dealt public cards")

        for (person in people) {
            val player = Triple(null, Board.personToDataAtStart(person), null)
            players.add(player)
            eliminatedPlayers.add(player)
        }

        println("Added empty players")

        for (player in players) {
            if (player.first != null) {
                val thread = Thread { runChat(Pair(player.first!!, player.second)) }
                thread.start()
                chatThreads.add(thread)
            }
        }

        println("Started chat")
    }

    private fun getMove(i: Int) {
        val player = players[i]

        val moves = Random.Default.nextInt(1..6) + Random.Default.nextInt(1..6)
        val location = tryUntil {
            player.first!!.sendPacket(RequestMoveTo(moves))

            val location = (player.first!!.receivePacket() as RespondMoveTo).location

            if (!Board.isLegal(player.second.location, location, moves, players.map { it.second }.toTypedArray())) {
                throw Exception("InvalidMove")
            }

            location
        }

        val playerData = PlayerData(player.second.person, location)
        sendAlert(AlertMoveTo(playerData))
        players[i] = Triple(players[i].first, playerData, players[i].third)

        if (location is PointLocation) {
            println("   They moved to ${location.x}, ${location.y}")
        } else {
            val room = (location as RoomLocation).room
            if (room != null) {
                println("   They moved to $room")
            } else {
                println("   They moved to the Cluedo")
            }
        }
    }

    private fun doRumor(playerIndex: Int) {
        val player = players[playerIndex]
        val room = (player.second.location as RoomLocation).room

        val rumorData = tryUntil {
            player.first!!.sendPacket(RequestRumor())

            (player.first!!.receivePacket() as RespondRumor)
        }

        sendAlert(AlertRumor(rumorData.person, rumorData.weapon))

        println("   They started a rumor that ${rumorData.person} did it in $room with the ${rumorData.weapon}")

        val accusedIndex = players.indexOfFirst { it.second.person == rumorData.person }
        val accused = players[accusedIndex]
        val newAccused = Triple(accused.first, PlayerData(players[accusedIndex].second.person, RoomLocation(room)), accused.third)
        players[accusedIndex] = newAccused
        if (accused in eliminatedPlayers) {
            eliminatedPlayers.remove(accused)
            eliminatedPlayers.add(newAccused)
        }

        var wasCounterExample = false
        for (i in playerIndex + 1 until playerIndex + numPlayers) {
            val respondingPlayer = players[i % numPlayers]

            var hasCounterExample = false
            for (testCard in respondingPlayer.third!!) {
                if (testCard is PersonCard) {
                    if (testCard.person == rumorData.person) {
                        hasCounterExample = true
                    }
                }

                if (testCard is WeaponCard) {
                    if (testCard.weapon == rumorData.weapon) {
                        hasCounterExample = true
                    }
                }

                if (testCard is RoomCard) {
                    if (testCard.room == room) {
                        hasCounterExample = true
                    }
                }
            }

            val card = if (!hasCounterExample) {
                null
            } else{
                tryUntil {
                    respondingPlayer.first!!.sendPacket(RequestCounterExample())

                    val card = (respondingPlayer.first!!.receivePacket() as RespondCounterExample).card

                    var haveCard = false
                    for (testCard in respondingPlayer.third!!) {
                        if (testCard is PersonCard) {
                            if (testCard.person == rumorData.person) {
                                haveCard = true
                                break
                            }
                        }

                        if (testCard is WeaponCard) {
                            if (testCard.weapon == rumorData.weapon) {
                                haveCard = true
                                break
                            }
                        }

                        if (testCard is RoomCard) {
                            if (testCard.room == room) {
                                haveCard = true
                                break
                            }
                        }
                    }

                    if (!haveCard) {
                        throw Exception("Doesn'tHaveCard")
                    }

                    card
                }
            }

            if (card != null) {
                for (listener in listenerConnections) {
                    if (player.first == listener) {
                        listener.sendPacket(AlertCounterExample(respondingPlayer.second.person, card))
                    } else {
                        listener.sendPacket(AlertCounterExample(respondingPlayer.second.person, null))
                    }
                }

                wasCounterExample = true

                println("   ${respondingPlayer.second.person} had a counter example $card")
                break
            }
        }

        if (!wasCounterExample) {
            println("   Nobody had a counter example")
        }
    }

    private fun doGuess(player: Triple<Connection?, PlayerData, Array<Card>?>): Boolean {
        val guessData = tryUntil {
            player.first!!.sendPacket(RequestClueGuess())

            player.first!!.receivePacket() as RespondClueGuess
        }

        sendAlert(AlertClueGuess(guessData.person, guessData.weapon, guessData.room))

        return if (guessData.person == truth.first && guessData.weapon == truth.second && guessData.room == truth.third) {
            sendAlert(AlertWin(player.second.person))
            println("   They won with the guess ${guessData.person} in ${guessData.room} with ${guessData.weapon}")
            true
        } else {
            sendAlert(AlertElimination(player.second.person))
            eliminatedPlayers.add(player)
            println("   They lost with the guess ${guessData.person} in ${guessData.room} with ${guessData.weapon}")
            false
        }
    }

    private fun closeAll() {
        listenerConnections.clear()
        players.clear()
        eliminatedPlayers.clear()
    }

    fun run() {
        start()

        var done = false
        while (!done) {
            for (i in players.indices) {
                var player = players[i]
                println("Doing ${player.second.person}'s turn")

                if (player !in eliminatedPlayers) {
                    getMove(i)

                    player = players[i]
                    val playerData = player.second
                    if (playerData.location is RoomLocation) {
                        if (playerData.location.room != null) {
                            println("   They are starting a rumor")
                            doRumor(i)
                        } else {
                            println("   They are guessing")
                            if (doGuess(player)) {
                                done = true
                                break
                            }
                        }
                    }
                } else {
                    println("   They are out")
                }
            }

            if (eliminatedPlayers.size == 6) {
                println("No players left")
                break
            }
        }

        for (chatThread in chatThreads) {
            chatThread.interrupt()
            chatThread.join()
        }
        println("Chat Closed")

        closeAll()
        println("Closed connections")
    }
}