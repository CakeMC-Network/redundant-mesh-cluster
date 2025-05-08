package net.cakemc.meshing.redundant.logger

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class StandardLoggerFactory : LoggerFactorySupport(), LoggerFactory {

    override fun createLogger(name: String?): ILogger {
        val logger = Logger.getLogger(name)

        logger.useParentHandlers = false // ⛔ Prevents default console output

        // Remove existing handlers (just to be safe)
        for (handler in logger.handlers) {
            logger.removeHandler(handler)
        }

        // Add our own handler with custom formatting
        val handler = ConsoleHandler().apply {
            formatter = LogFormatter()
            level = Level.ALL
        }
        logger.addHandler(handler)

        logger.level = Level.ALL

        return StandardLogger(logger)
    }

    internal inner class StandardLogger(private val logger: Logger) : ILogger {
        override fun log(level: Level, message: String) {
            log(level, message, null)
        }

        override fun log(level: Level, message: String, thrown: Throwable?) {
            val logRecord = LogRecord(level, message)
            logRecord.loggerName = logger.name
            logRecord.thrown = thrown
            logRecord.sourceClassName = logger.name
            logger.log(logRecord)
        }

        override fun log(logEvent: LogEvent) {
            logger.log(logEvent.logRecord)
        }

        override val level: Level
            get() = logger.level

        override fun isLoggable(level: Level?): Boolean {
            return logger.isLoggable(level)
        }
    }
}
