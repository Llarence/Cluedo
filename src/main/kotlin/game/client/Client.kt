package game.client

import game.shared.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

// Should only be used once
abstract class Client(address: InetAddress, private val observer: Boolean) {
    private val socket = Socket()

    init {
        socket.connect(InetSocketAddress(address.hostAddress, 5566), 1000)
    }

    private val connection = Connection(socket)

    internal lateinit var self: People

    private var done = false

    private fun runChat() {
        while (true) {
            val chatMessage = connection.receiveChatMessage()

            if (chatMessage != null) {
                onChat(chatMessage)
            } else {
                Thread.sleep(100)
            }
        }
    }

    internal abstract fun onChat(chatMessage: ChatMessage)

    internal fun sendChatMessage(text: String) {
        connection.sendChatMessage(self, text)
    }

    fun run() {
        val chatThread = Thread { runChat() }
        chatThread.start()

        connection.sendPacket(IsObserver(observer))

        while (true) {
            val packet: Packet
            try {
                packet = connection.receivePacket()
            } catch (_: java.lang.Exception) {
                done = true
                chatThread.join()
                return
            }

            when (packet) {
                is AlertPlayerPick -> {
                    onPersonPicked(packet.person)
                }

                is AlertHand -> {
                    onHand(packet.cards)
                }

                is AlertPublicCards -> {
                    onPublicCards(packet.cards)
                }

                is AlertMoveTo -> {
                    onMoveTo(packet.playerData)
                }

                is AlertRumor -> {
                    onRumor(packet.person, packet.weapon)
                }

                is AlertCounterExample -> {
                    onCounterExample(packet.person, packet.card)
                }

                is AlertClueGuess -> {
                    onClueGuess(packet.person, packet.weapon, packet.room)
                }

                is AlertWin -> {
                    onWin(packet.winner)
                }

                is AlertElimination -> {
                    onElimination(packet.eliminatee)
                }
            }

            if (!observer) {
                when (packet) {
                    is RequestPlayerPick -> {
                        self = pick()
                        connection.sendPacket(RespondPlayerPick(self))
                    }

                    is RequestMoveTo -> {
                        connection.sendPacket(RespondMoveTo(getMove(packet.moves)))
                    }

                    is RequestRumor -> {
                        val data = startRumor()
                        connection.sendPacket(RespondRumor(data.first, data.second))
                    }

                    is RequestCounterExample -> {
                        connection.sendPacket(RespondCounterExample(getCounterExample()))
                    }

                    is RequestClueGuess -> {
                        val data = getGuess()
                        connection.sendPacket(RespondClueGuess(data.first, data.second, data.third))
                    }
                }
            }
        }
    }

    internal abstract fun onPersonPicked(person: People)

    internal abstract fun onHand(cards: Array<Card>)

    internal abstract fun onPublicCards(cards: Array<Card>)

    internal abstract fun onMoveTo(player: PlayerData)

    internal abstract fun onRumor(person: People, weapon: Weapons)

    internal abstract fun onCounterExample(person: People, card: Card)

    internal abstract fun onClueGuess(person: People, weapon: Weapons, room: Rooms)

    internal abstract fun onWin(person: People)

    internal abstract fun onElimination(person: People)

    internal abstract fun pick(): People

    internal abstract fun getMove(moves: Int): Location

    internal abstract fun startRumor(): Pair<People, Weapons>

    internal abstract fun getCounterExample(): Card

    internal abstract fun getGuess(): Triple<People, Weapons, Rooms>
}