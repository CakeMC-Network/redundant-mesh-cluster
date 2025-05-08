package net.cakemc.meshing.redundant.logger

import java.util.logging.Level

interface ILogger {
    fun log(level: Level, message: String)

    fun log(level: Level, message: String, thrown: Throwable?)

    fun log(logEvent: LogEvent)

    val level: Level

    fun isLoggable(level: Level?): Boolean
}
