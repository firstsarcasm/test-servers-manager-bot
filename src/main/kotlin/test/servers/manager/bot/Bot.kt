package test.servers.manager.bot

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import test.servers.manager.ServerDto
import test.servers.manager.bot.keyboard.KeyboardMaker
import test.servers.manager.jdbc.DatabaseClient
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Bot : TelegramLongPollingBot() {

    val helpMessage = "Чтобы занять определенный стенд, отправьте его id, например c4('c' как русская, так и английская)." +
            "\nСтенд с0 НЕ занимается по нажатию 'Забрать любой свободный'." +
            "\nФормат отображаемого времени: dd.MM.yy HH:mm."
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

    override fun onUpdateReceived(update: Update) {

        val chatId = update.extractChatId()
        sendMainKeyboard(chatId)

        if (update.hasTextMessage()) {
            if (update.message.text.startsWith("kick_all")) {
                DatabaseClient.Server.releaseAllServers()
                sendTextMessage(chatId, "Вы выкинули всех")
                return
            }

            if (update.message.text.startsWith(KeyboardMaker.keepServerButton)) {
                val freeServerId = transaction {
                    DatabaseClient.Server.select {
                        DatabaseClient.Server.user.isNull() and DatabaseClient.Server.id.neq("c0")
                    }.firstOrNull()?.get(DatabaseClient.Server.id)
                }
                keepServer(update, freeServerId, chatId)
                return
            }

            if (update.message.text.startsWith(KeyboardMaker.cancelButton)) {
                val server = DatabaseClient.Server.selectByChatId(chatId.toString())
                if (server == null) {
                    sendTextMessage(chatId, "Вы пока не заняли сервер")
                    return
                }
                DatabaseClient.Server.releaseServers(chatId.toString())
                sendTextMessage(chatId, "Вы освободили занятые вами серверы")
                return
            }

            if (update.message.text.startsWith(KeyboardMaker.getListButton)) {
                val servers = transaction {
                    DatabaseClient.Server.selectAll().sortedBy { it[DatabaseClient.Server.id] }.map {
                        val user = it[DatabaseClient.Server.user]
                                ?: return@map "${it[DatabaseClient.Server.id]}  Свободен."
                        val time = it[DatabaseClient.Server.time]
                        val parsedTime = LocalDateTime.parse(time)
                        val formattedTime = parsedTime.format(DATE_TIME_FORMATTER)
                        return@map "${it[DatabaseClient.Server.id]}  $user  Занят с: $formattedTime"
                    }.joinToString("\n")
                }
                sendTextMessage(chatId,servers)
                return
            }

            var userText = update.message.text.toLowerCase()
            if (userText.startsWith(KeyboardMaker.helpButton.toLowerCase())) {
                sendTextMessage(chatId, helpMessage)
                return
            }

            val availableServers = transaction {
                DatabaseClient.Server.selectAll().map {
                    ServerDto(
                            it[DatabaseClient.Server.id],
                            it[DatabaseClient.Server.user],
                            it[DatabaseClient.Server.time],
                            it[DatabaseClient.Server.chatId]
                    )
                }
            }

            //replace russian 'с' to english 'c'
            if (userText.length < 4 && userText.contains("с")) {
                userText = userText.replace("с", "c")
            }
            if (availableServers.map { it.id }.contains(userText)) {
                val targetServer = availableServers.firstOrNull { it.id == userText }
                if (targetServer?.chatId == null || targetServer.chatId.isNullOrEmpty()) {
                    keepServer(update, userText, chatId)
                    return
                } else {
                    sendTextMessage(chatId, "Сервер ${userText} занят пользователем ${targetServer.user}")
                    return
                }
            } else {
                sendTextMessage(chatId, "Сервера с названием ${update.message.text} не существует")
                return
            }
            return
        }
    }

    private fun keepServer(update: Update, serverId: String?, chatId: Long) {
        val username = update.extractUsername()

        if (serverId == null) {
            sendTextMessage(chatId, "Все серверы заняты")
            return
        }

        DatabaseClient.Server.update(serverId, username ?: "unknown", chatId.toString())

        sendTextMessage(chatId, "Вы заняли стенд $serverId")
    }

    private fun tryExecute(message: SendMessage) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun tryExecute(message: SendDocument) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    override fun getBotUsername(): String = "botname"
    override fun getBotToken(): String = "bottoken"

    private fun sendTextMessage(chatId: Long, text: String) {
        val message = SendMessage()
                .setText(text)
                .setChatId(chatId)
        tryExecute(message)
    }

    private fun sendMainKeyboard(chatId: Long) {
        tryExecute(KeyboardMaker.getKeyboardMessage(chatId))
    }

    private fun sendFile(chatId: Long, file: File) {
        val message = SendDocument().setChatId(chatId).setDocument(file)
        tryExecute(message)
    }

    private fun Update.hasTextMessage() = this.hasMessage() && this.message.hasText()

    private fun Update.extractChatId() =
            if (this.hasTextMessage()) message.chatId
            else callbackQuery.message.chatId

    private fun Update.extractUsername() =
            if (this.hasTextMessage()) message?.from?.userName ?: message?.from?.lastName ?: message?.from?.firstName ?: message?.from?.id?.toString()
            else callbackQuery?.from?.userName ?: callbackQuery?.from?.lastName ?: callbackQuery?.from?.firstName ?: callbackQuery?.from?.id?.toString()
}