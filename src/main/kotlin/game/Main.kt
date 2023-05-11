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
    try {
        Thread {
            Server(numPlayers).run()
        }.start()

        Thread.sleep(500)

        client = if (choice == 0) {
            DisplayClient(InetAddress.getLocalHost(), true)
        } else {
            DisplayClient(InetAddress.getLocalHost(), false)
        }

        for (i in 0 until numEasyAIs) {
            Thread {
                EasyAI(InetAddress.getLocalHost()).run()
            }.start()
        }

        for (i in 0 until numMediumAIs) {
            Thread {
                MediumAI(InetAddress.getLocalHost()).run()
            }.start()
        }

        for (i in 0 until numHardAIs) {
            Thread {
                HardAI(InetAddress.getLocalHost()).run()
            }.start()
        }
    } catch (_: Exception) {
        JOptionPane.showMessageDialog(null, "Failed to connect")
        exitProcess(0)
    }

    client.run()
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