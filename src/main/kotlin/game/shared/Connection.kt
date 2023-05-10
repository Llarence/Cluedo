package game.shared

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock

// Could be unsafe
// Also no memory limit
class Connection(private val socket: Socket) {
    private val writeLock = ReentrantLock()

    private val output = ObjectOutputStream(socket.getOutputStream())

    private val input = ObjectInputStream(socket.getInputStream())

    private val packetInput = LinkedBlockingQueue<Packet>()
    private val chatMessageInput = LinkedBlockingQueue<ChatMessage>()

    private val thread = Thread(::readSteam)

    init {
        output.flush()

        thread.start()
    }

    private fun readSteam() {
        try {
            while (true) {
                val received = input.readUnshared()

                if (received is Packet) {
                    packetInput.add(received)
                } else {
                    chatMessageInput.add(received as ChatMessage)
                }
            }
        } catch (_: InterruptedException) {
        }
    }

    fun sendPacket(data: Packet) {
        writeLock.lock()
        output.writeUnshared(data)
        writeLock.unlock()
    }

    fun receivePacket(): Packet {
        return packetInput.take()
    }

    fun sendChatMessage(speaker: People, text: String) {
        writeLock.lock()
        output.writeUnshared(ChatMessage(speaker, text))
        writeLock.unlock()
    }

    fun receiveChatMessage(): ChatMessage {
        return chatMessageInput.take()
    }

    private fun finalize() {
        thread.interrupt()
        thread.join()
        socket.close()
    }
}