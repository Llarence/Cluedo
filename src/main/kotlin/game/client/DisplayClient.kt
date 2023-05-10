package game.client

import game.rendering.ChatWindow
import game.rendering.GameWindow
import game.shared.*
import java.net.InetAddress
import javax.swing.SwingUtilities

class DisplayClient(address: InetAddress, observer: Boolean) : Client(address, observer) {
    private val window = GameWindow(observer)

    private val chatWindow = ChatWindow(observer, ::sendChatMessage)

    private val peopleLeft = People.values().toMutableList()
    
    private val players: Array<PlayerData>

    private lateinit var myCards: Array<Card>

    private lateinit var currRumor: Triple<People, Weapons, Rooms>

    private var lastMovedIndex = -1

    init {
        val playersList = mutableListOf<PlayerData>()
        for (person in peopleLeft) {
            playersList.add(Board.personToDataAtStart(person))
        }
        
        players = playersList.toTypedArray()

        updateWindow {
            window.players = players
        }
    }
    
    private fun updateWindow(function: () -> Unit) {
        // This feels unsafe
        window.graphicsLock.lock()
        function()
        window.graphicsLock.unlock()

        SwingUtilities.invokeLater {
            window.repaint()
        }
    }

    override fun onChat(chatMessage: ChatMessage) {
        SwingUtilities.invokeLater {
            chatWindow.addChatMessage(chatMessage)
            chatWindow.repaint()
        }
    }

    override fun onPersonPicked(person: People) {
        peopleLeft.remove(person)
    }

    override fun onHand(cards: Array<Card>) {
        myCards = cards

        updateWindow {
            window.cards = cards
        }
    }

    override fun onPublicCards(cards: Array<Card>) {
        updateWindow {
            window.publicCards = cards
            window.inited = true
        }
    }

    override fun onMoveTo(player: PlayerData) {
        lastMovedIndex = players.indexOfFirst { it.person == player.person }
        players[lastMovedIndex] = player

        updateWindow {
            window.players = players
        }
    }

    override fun onRumor(person: People, weapon: Weapons) {
        val currPlayer = players[lastMovedIndex]
        val room = (currPlayer.location as RoomLocation).room!!
        players[players.indexOfFirst { it.person == person }] = PlayerData(person, RoomLocation(room))

        currRumor = Triple(person, weapon, room)

        updateWindow {
            window.players = players
        }

        window.displayRequest("${currPlayer.person} has started a rumor that $person did the murder with the $weapon in the $room", arrayOf("Ok"))
    }

    override fun onCounterExample(person: People, card: Card?) {
        if (card == null) {
            window.displayRequest("$person has revealed that they have a counter example", arrayOf("Ok"))
        } else {
            window.displayRequest("$person has revealed that they have $card", arrayOf("Ok"))
        }
    }

    override fun onClueGuess(person: People, weapon: Weapons, room: Rooms) {
        val currPlayer = players[lastMovedIndex]
        window.displayRequest("${currPlayer.person} has accused $person of the murder with the $weapon in the $room", arrayOf("Ok"))
    }

    override fun onWin(person: People) {
        val currPlayer = players[lastMovedIndex]
        window.displayRequest("${currPlayer.person} has won", arrayOf("Ok"))
    }

    override fun onElimination(person: People) {
        val currPlayer = players[lastMovedIndex]
        window.displayRequest("${currPlayer.person} has been eliminated", arrayOf("Ok"))
    }

    override fun pick(): People {
        val person = window.displayRequest("Which person?", peopleLeft.toTypedArray())

        updateWindow {
            window.playerIndex = players.indexOfFirst { it.person == person }
        }

        chatWindow.enableSending()

        return person
    }

    override fun getMove(moves: Int): Location {
        updateWindow {
            window.startGetMove(moves)
        }

        window.choosingMove.await()

        return window.output
    }

    override fun startRumor(): Pair<People, Weapons> {
        val person = window.displayRequest("Who do you want to start the rumor about", People.values())
        val weapon = window.displayRequest("What weapon did $person use", Weapons.values())
        return Pair(person, weapon)
    }

    override fun getCounterExample(): Card {
        val counterExamples = mutableListOf<Card>()

        for (card in myCards) {
            if (card is PersonCard) {
                if (currRumor.first == card.person) {
                    counterExamples.add(card)
                }
            }

            if (card is WeaponCard) {
                if (currRumor.second == card.weapon) {
                    counterExamples.add(card)
                }
            }

            if (card is RoomCard) {
                if (currRumor.third == card.room) {
                    counterExamples.add(card)
                }
            }
        }

        return window.displayRequest("What counter example do you want to give", counterExamples.toTypedArray())
    }

    override fun getGuess(): Triple<People, Weapons, Rooms> {
        val person = window.displayRequest("Who do you want to accuse", People.values())
        val room = window.displayRequest("Where did $person do it", Rooms.values())
        val weapon = window.displayRequest("What weapon did $person use", Weapons.values())
        return Triple(person, weapon, room)
    }
}