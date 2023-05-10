package game.rendering

import game.shared.ChatMessage
import java.awt.BorderLayout
import java.awt.Panel
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.text.*


class ChatWindow(observer: Boolean, private val onSend: (String) -> Unit) : JFrame() {
    private val textBox = JTextPane()

    val button: JButton?

    init {
        title = "Chat"

        defaultCloseOperation = EXIT_ON_CLOSE

        textBox.isEditable = false
        textBox.text = "Chat:"

        add(JScrollPane(textBox), BorderLayout.CENTER)

        if (!observer) {
            val panel = Panel()
            panel.layout = BorderLayout()

            val input = JTextField()
            (input.document as PlainDocument).documentFilter = object : DocumentFilter() {
                private val pattern = Pattern.compile("[A-Za-z0-9\".,'_ ]")

                override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
                    if (text == "" || pattern.matcher(text).find()) {
                        super.replace(fb, offset, length, text, attrs)
                    }
                }

                override fun insertString(fb: FilterBypass, offset: Int, text: String, attr: AttributeSet?) {
                    if (pattern.matcher(text).find()) {
                        super.insertString(fb, offset, text, attr)
                    }
                }
            }

            panel.add(input, BorderLayout.CENTER)

            button = JButton("Send!")
            button.addActionListener {
                if (input.text != "") {
                    onSend(input.text)
                    input.text = ""
                }
            }

            button.isEnabled = false

            panel.add(button, BorderLayout.LINE_END)

            add(panel, BorderLayout.PAGE_END)
        } else {
            button = null
        }

        pack()

        setSize(500, 500)

        isVisible = true
    }

    fun enableSending() {
        button?.isEnabled = true
    }

    fun addChatMessage(chatMessage: ChatMessage) {
        textBox.caretPosition = textBox.document.length
        textBox.setCharacterAttributes(StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, chatMessage.person.color), false)

        textBox.isEditable = true
        textBox.replaceSelection("\n${chatMessage.person}: ${chatMessage.text}")
        textBox.isEditable = false
    }
}