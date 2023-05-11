package game

import com.formdev.flatlaf.FlatDarculaLaf
import game.client.DisplayClient
import game.client.EasyAI
import game.client.HardAI
import game.client.MediumAI
import game.server.Server
import java.net.InetAddress
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.system.exitProcess


val onTopFrame = JFrame().apply { isAlwaysOnTop = true }

fun initServer() {
    val numPlayers = JOptionPane.showOptionDialog(onTopFrame, "How many players?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (3..6).toList().toTypedArray(), 0) + 3
    if (numPlayers == 2) {
        exitProcess(0)
    }

    val numEasyAIs = JOptionPane.showOptionDialog(onTopFrame, "How many easy ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers).toList().toTypedArray(), 0)
    if (numEasyAIs == -1) {
        exitProcess(0)
    }

    val numMediumAIs = JOptionPane.showOptionDialog(onTopFrame, "How many medium ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers - numEasyAIs).toList().toTypedArray(), 0)
    if (numMediumAIs == -1) {
        exitProcess(0)
    }

    val numHardAIs = JOptionPane.showOptionDialog(onTopFrame, "How many hard ais?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, (0..numPlayers - numEasyAIs - numMediumAIs).toList().toTypedArray(), 0)
    if (numHardAIs == -1) {
        exitProcess(0)
    }

    val choice = if (numPlayers - numEasyAIs - numMediumAIs - numHardAIs > 0) {
        JOptionPane.showOptionDialog(onTopFrame, "What would you like to do?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arrayOf("Observe", "Join"), 0)
    } else {
        0
    }

    val client: DisplayClient
    val serverThread: Thread
    val aiClientThreads: MutableList<Thread>
    try {
        serverThread = Thread {
            Server(numPlayers).run()
        }

        serverThread.start()

        Thread.sleep(500)

        client = if (choice == 0) {
            DisplayClient(InetAddress.getLocalHost(), true)
        } else {
            DisplayClient(InetAddress.getLocalHost(), false)
        }

        aiClientThreads = mutableListOf()

        for (i in 0 until numEasyAIs) {
            val aiClient = Thread {
                EasyAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClientThreads.add(aiClient)
        }

        for (i in 0 until numMediumAIs) {
            val aiClient = Thread {
                MediumAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClientThreads.add(aiClient)
        }

        for (i in 0 until numHardAIs) {
            val aiClient = Thread {
                HardAI(InetAddress.getLocalHost()).run()
            }
            aiClient.start()

            aiClientThreads.add(aiClient)
        }
    } catch (_: Exception) {
        JOptionPane.showMessageDialog(onTopFrame, "Failed to connect")
        exitProcess(0)
    }

    val clientThread = Thread { client.run() }
    clientThread.start()

    serverThread.join()
    for (aiClient in aiClientThreads) {
        aiClient.interrupt()
        aiClient.join()
    }

    clientThread.join()
}

fun initClient() {
    val text = JOptionPane.showInputDialog(onTopFrame, "Server address", "Please select", JOptionPane.NO_OPTION)

    if (text != null) {
        val client: DisplayClient
        try {
            client = DisplayClient(InetAddress.getByName(text), false)
        } catch (_: Exception) {
            JOptionPane.showMessageDialog(onTopFrame, "Failed to connect")
            exitProcess(0)
        }

        client.run()
    }
}

// Replace all Reentrant locks with Semaphores
fun main() {
    try {
        UIManager.setLookAndFeel(FlatDarculaLaf())
    } catch (_: Exception) {
    }

    val choice = JOptionPane.showOptionDialog(onTopFrame, "What would you like to do?", "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arrayOf("Host", "Join"), 0)
    if (choice == 0) {
        initServer()
    }

    if (choice == 1) {
        initClient()
    }

    exitProcess(0)
}