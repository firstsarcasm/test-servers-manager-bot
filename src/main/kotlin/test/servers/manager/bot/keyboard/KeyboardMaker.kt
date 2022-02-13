package test.servers.manager.bot.keyboard

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

object KeyboardMaker {
    private val keyboard = createKeyboard()
    const val getListButton = "Список"
    const val keepServerButton = "Забрать любой свободный"
    const val cancelButton = "Отпустить"
    const val helpButton = "Help"

    private val rawKeyboardMessage = SendMessage()
            .setReplyMarkup(keyboard)
            .setText("------------------")

    public fun getKeyboardMessage(chatId: Long): SendMessage {
        return rawKeyboardMessage.setChatId(chatId)
    }

    private fun createKeyboard(): ReplyKeyboardMarkup {
        val row = KeyboardRow()
        val row2 = KeyboardRow()
        val row3 = KeyboardRow()
        row.add(KeyboardButton().apply { text = getListButton })
        row2.add(KeyboardButton().apply { text = keepServerButton })
        row2.add(KeyboardButton().apply { text = cancelButton })
        row3.add(KeyboardButton().apply { text = helpButton })
        val keyboard = ReplyKeyboardMarkup().apply {
            keyboard = listOf(row, row2, row3)
            resizeKeyboard = true
        }
        return keyboard
    }
}