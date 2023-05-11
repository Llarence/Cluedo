package game

import com.formdev.flatlaf.FlatDarculaLaf
import game.client.DisplayClient
import game.client.EasyAI
import game.client.HardAI
import game.client.MediumAI
import game.server.Server
import java.net.InetAddress
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.system.exitProcess

fun initServer() {
    val numPlayers = JOptionPane.showOptionDialog(null, "How many players?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (3..6).toList().toTypedArray(), 0) + 3
    if (numPlayers == 2) {
        exitProcess(0)
    }

    val numEasyAIs = JOptionPane.showOptionDialog(null, "How many easy ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers).toList().toTypedArray(), 0)
    if (numEasyAIs == -1) {
        exitProcess(0)
    }

    val numMediumAIs = JOptionPane.showOptionDialog(null, "How many medium ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers - numEasyAIs).toList().toTypedArray(), 0)
    if (numMediumAIs == -1) {
        exitProcess(0)
    }

    val numHardAIs = JOptionPane.showOptionDialog(null, "How many hard ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers - numEasyAIs - numMediumAIs).toList().toTypedArray(), 0)
    if (numHardAIs == -1) {
        exitProcess(0)
    }

    val choice = if (numPlayers - numEasyAIs - numMediumAIs - numHardAIs > 0) {
        JOptionPane.showOptionDialog(null, "What would you like to do?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arrayOf("Observe", "Join"), 0)
    } else {
        0
    }

    val client: DisplayClient
    val server: Thread
    val aiClients: MutableList<Thread>
    try {
        server = Thread {
            Server(numPlayers).run()
        }

        server.start()

        Thread.sleep(500)

        client = if (choice == 0) {
            DisplayClient(InetAddress.getLocalHost(), true)
        } else {
            DisplayClient(InetAddress.getLocalHost(), false)
        }

        aiClients = mutableListOf()

        for (i in 0 until numEasyAIs) {
            val aiClient = Thread {
                EasyAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClients.add(aiClient)
        }

        for (i in 0 until numMediumAIs) {
            val aiClient = Thread {
                MediumAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClients.add(aiClient)
        }

        for (i in 0 until numHardAIs) {
            val aiClient = Thread {
                HardAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClients.add(aiClient)
        }
    } catch (_: Exception) {
        JOptionPane.showMessageDialog(null, "Failed to connect")
        exitProcess(0)
    }

    Thread { client.run() }.start()

    server.join()
    for (aiClient in aiClients) {
        aiClient.interrupt()
        aiClient.join()
    }
}

fun initClient() {
    val text = JOptionPane.showInputDialog(null, "Server address", "Please select", JOptionPane.NO_OPTION)

    if (text != null) {
        val client: DisplayClient
        try {
            client = DisplayClient(InetAddress.getByName(text), false)
        } catch (_: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to connect")
            exitProcess(0)
        }

        client.run()
    }
}

// Replace all Reentrant locks with semaphores
fun main() {
    try {
        UIManager.setLookAndFeel(FlatDarculaLaf())
    } catch (_: Exception) {
    }

    val choice = JOptionPane.showOptionDialog(null, "What would you like to do?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arrayOf("Host", "Join"), 0)
    if (choice == 0) {
        initServer()
    }

    if (choice == 1) {
        initClient()
    }
}