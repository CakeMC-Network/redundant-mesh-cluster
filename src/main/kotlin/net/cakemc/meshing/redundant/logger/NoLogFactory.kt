package net.cakemc.meshing.redundant.logger

import java.util.logging.Level

class NoLogFactory : LoggerFactory {
    val noLogger: ILogger = NoLogger()

    override fun getLogger(name: String): ILogger {
        return noLogger
    }

    internal inner class NoLogger : ILogger {
        override fun log(level: Level, message: String) {
        }

        override fun log(level: Level, message: String, thrown: Throwable?) {
        }

        override fun log(logEvent: LogEvent) {
        }

        override val level: Level
            get() = Level.OFF

        override fun isLoggable(level: Level?): Boolean {
            return false
        }
    }
}
