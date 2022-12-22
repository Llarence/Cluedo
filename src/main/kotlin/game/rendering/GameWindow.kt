package game.rendering

import game.shared.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.math.roundToInt
import kotlin.system.exitProcess

val roomToPointLocation = mapOf(
    Rooms.KITCHEN to PointLocation(1, 20),
    Rooms.BALL_ROOM to PointLocation(10, 19),
    Rooms.CONSERVATORY to PointLocation(19, 20),
    Rooms.BILLIARD_ROOM to PointLocation(20, 13),
    Rooms.LIBRARY to PointLocation(19, 7),
    Rooms.STUDY to PointLocation(19, 0),
    Rooms.HALL to PointLocation(11, 2),
    Rooms.LOUNGE to PointLocation(2, 2),
    Rooms.DINING_ROOM to PointLocation(2, 11),
    null to PointLocation(11, 10))

class GameWindow(private val observer: Boolean) : JFrame() {
    val graphicsLock = ReentrantLock()
    val choosingMove = CyclicBarrier(2)

    private var currMoves = -1
    private var moving = false

    var inited = false
    lateinit var cards: Array<Card>
    lateinit var publicCards: Array<Card>
    var players = arrayOf<PlayerData>()
    var playerIndex = -1

    lateinit var output: Location

    private val image = ImageIO.read(GameWindow::class.java.getResource("/board.png"))

    init {
        title = "Game"

        defaultCloseOperation = EXIT_ON_CLOSE

        setSize(750, 750)

        isVisible = true

        addMouseListener(object : MouseListener {
            override fun mouseClicked(event: MouseEvent) {
                if (moving) {
                    val trueWidth = width - insets.left - insets.right
                    val trueHeight = height - insets.top - insets.bottom
                    val rectWidth = 55.7 * trueWidth / 1500
                    val rectHeight = 55.7 * trueHeight / 1500
                    val offsetX = 83 * trueWidth / 1500
                    val offsetY = 55 * trueHeight / 1500

                    val tileX = ((event.x - insets.left - offsetX) / rectWidth).toInt()
                    val tileY = 24 - ((event.y - insets.top - offsetY) / rectHeight).toInt()
                    val location = if (Board.isTile(tileX, tileY)) {
                        PointLocation(tileX, tileY)
                    } else {
                        val room = Board.getRoom(tileX, tileY)
                        if (room != null) {
                            RoomLocation(room)
                        } else {
                            if (Board.isInClue(tileX, tileY)) {
                                RoomLocation(null)
                            } else {
                                null
                            }
                        }
                    }

                    if (location != null && Board.isLegal(players[playerIndex].location, location, currMoves, players)) {
                        output = location
                        moving = false
                        choosingMove.await()
                    }
                }
            }

            override fun mousePressed(event: MouseEvent) {
            }

            override fun mouseReleased(event: MouseEvent) {
            }

            override fun mouseEntered(event: MouseEvent) {
            }

            override fun mouseExited(event: MouseEvent) {
            }
        })
    }

    fun <T : Any> displayRequest(message: String, options: Array<T>): T {
        val index = JOptionPane.showOptionDialog(null, message, "Please select", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0)
        if (index == -1) {
            exitProcess(0)
        }
        return options[index]
    }

    fun startGetMove(moves: Int) {
        currMoves = moves
        moving = true

        choosingMove.reset()
    }

    override fun paint(graphics: Graphics) {
        if (inited) {
            graphicsLock.lock()

            val graphics2d = graphics as Graphics2D

            val trueWidth = width - insets.left - insets.right
            val trueHeight = height - insets.top - insets.bottom
            graphics2d.drawImage(image, insets.left, insets.top, trueWidth, trueHeight, null)

            val rectWidth = 55.7 * trueWidth / 1500
            val rectHeight = 55.7 * trueHeight / 1500
            val offsetX = 83 * trueWidth / 1500
            val offsetY = 55 * trueHeight / 1500
            for (player in players) {
                graphics2d.color = player.person.color

                val location: PointLocation = if (player.location is PointLocation) {
                    player.location
                } else {
                    val roomPointLocation = roomToPointLocation[(player.location as RoomLocation).room]!!
                    PointLocation(roomPointLocation.x + player.person.ordinal % 3, roomPointLocation.y + player.person.ordinal / 3)
                }

                graphics2d.fillRect((location.x * rectWidth + offsetX).roundToInt() + insets.left, ((24 - location.y) * rectHeight + offsetY).roundToInt() + insets.top, rectWidth.roundToInt(), rectHeight.roundToInt())
            }

            if (!observer) {
                graphics2d.color = players[playerIndex].person.color
                graphics2d.font = Font("TimesRoman", Font.BOLD, 12)
                var fontHeight = graphics2d.fontMetrics.maxAscent + graphics2d.fontMetrics.descent
                for (i in cards.indices) {
                    val text = cards[i].toString()
                    val length = graphics2d.fontMetrics.stringWidth(text)
                    graphics2d.drawString(text, width - insets.right - length - 1, ((i + 0.5) * fontHeight).toInt() + insets.top + 3)
                }

                graphics2d.font = Font("TimesRoman", Font.BOLD, 32)
                fontHeight = graphics2d.fontMetrics.maxAscent + graphics2d.fontMetrics.descent
                if (moving) {
                    graphics2d.drawString("$currMoves", insets.left - 1, (0.5 * fontHeight).toInt() + insets.top + 5)
                }
            }

            val offset = if (observer) {
                0
            } else {
                cards.size
            }

            graphics2d.color = Color.BLACK
            graphics2d.font = Font("TimesRoman", Font.BOLD, 12)
            val fontHeight = graphics2d.fontMetrics.maxAscent + graphics2d.fontMetrics.descent
            for (i in publicCards.indices) {
                val text = publicCards[i].toString()
                val length = graphics2d.fontMetrics.stringWidth(text)
                graphics2d.drawString(text, width - insets.right - length - 1, ((i + offset + 0.5) * fontHeight).toInt() + insets.top + 3)
            }

            graphicsLock.unlock()
        }
    }
}