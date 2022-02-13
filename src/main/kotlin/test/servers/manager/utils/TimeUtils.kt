package test.servers.manager.utils

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtils {
    public fun getMoscowNow(): LocalDate {
        val clock = Clock.system(ZoneId.of("Europe/Moscow"))
        return LocalDate.now(clock)
    }
    public fun getMoscowTimeNow(): LocalDateTime {
        val clock = Clock.system(ZoneId.of("Europe/Moscow"))
        return LocalDateTime.now(clock)
    }
}