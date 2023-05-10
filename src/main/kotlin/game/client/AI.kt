package game.client

import game.shared.*
import java.net.InetAddress

abstract class Event

class RumorEvent(val rumorStarter: People, val person: People, val weapon: Weapons, val room: Rooms, val response: Pair<People, Card?>?) : Event()

class GuessEvent(val guesser: People, val person: People, val weapon: Weapons, val room: Rooms) : Event()

// Counter example not happening in history
abstract class AI(address: InetAddress) : Client(address, false) {
    internal val peopleLeft = People.values().toMutableList()

    internal val players: Array<PlayerData>

    internal lateinit var publicCards: Array<Card>

    internal lateinit var myCards: Array<Card>

    internal var currRumor: RumorEvent? = null

    internal var lastMovedIndex = -1

    internal val history = mutableListOf<Event>()

    init {
        val playersList = mutableListOf<PlayerData>()
        for (person in peopleLeft) {
            playersList.add(Board.personToDataAtStart(person))
        }

        players = playersList.toTypedArray()
    }

    override fun onPersonPicked(person: People) {
        peopleLeft.remove(person)
    }

    override fun onHand(cards: Array<Card>) {
        myCards = cards
    }

    override fun onPublicCards(cards: Array<Card>) {
        publicCards = cards
    }

    override fun onMoveTo(player: PlayerData) {
        if (currRumor != null) {
            history.add(RumorEvent(currRumor!!.rumorStarter, currRumor!!.person, currRumor!!.weapon, currRumor!!.room, null))
            currRumor = null
        }

        lastMovedIndex = players.indexOfFirst { it.person == player.person }
        players[lastMovedIndex] = player
    }

    override fun onRumor(person: People, weapon: Weapons) {
        val currPlayer = players[lastMovedIndex]
        val room = (currPlayer.location as RoomLocation).room!!
        players[players.indexOfFirst { it.person == person }] = PlayerData(person, RoomLocation(room))

        currRumor = RumorEvent(currPlayer.person, person, weapon, room, null)
    }

    // Maybe it should also record the card if the AI sent it
    override fun onCounterExample(person: People, card: Card?) {
        history.add(RumorEvent(currRumor!!.rumorStarter, currRumor!!.person, currRumor!!.weapon, currRumor!!.room, Pair(person, card)))
        currRumor = null
    }

    // History will only be used if the guess is wrong, so it is fair to always assume the guess is wrong
    override fun onClueGuess(person: People, weapon: Weapons, room: Rooms) {
        history.add(GuessEvent(players[lastMovedIndex].person, person, weapon, room))
    }

    override fun onWin(person: People) {
    }
}