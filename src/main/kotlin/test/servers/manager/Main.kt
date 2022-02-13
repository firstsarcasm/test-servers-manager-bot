package test.servers.manager

import test.servers.manager.bot.Bot
import test.servers.manager.jdbc.DatabaseClient
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            DatabaseClient.initDatabase()
            initBot()
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun initBot(): Bot {
        ApiContextInitializer.init()
        return Bot().also { bot ->
            TelegramBotsApi().registerBot(bot)
        }
    }
}