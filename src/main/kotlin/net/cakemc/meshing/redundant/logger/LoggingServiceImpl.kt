package net.cakemc.meshing.redundant.logger

import net.cakemc.meshing.redundant.Member
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.concurrent.Volatile

class LoggingServiceImpl(
    private val groupName: String, loggingType: String?, private val thisMember: Member
) : LoggingService {

    private val lsListeners
            : CopyOnWriteArrayList<LogListenerRegistration> = CopyOnWriteArrayList<LogListenerRegistration>()

    private val thisAddressString =
        ("[" + thisMember.address.host).toString() + "]:" + thisMember.address.port

    private val mapLoggers: ConcurrentMap<String, ILogger> = ConcurrentHashMap<String, ILogger>(100)
    private val loggerFactory: LoggerFactory =
        Logger.newLoggerFactory(loggingType)

    @Volatile
    private var minLevel: Level = Level.OFF

    override fun getLogger(name: String): ILogger {
        var logger: ILogger? = mapLoggers.get(name)
        if (logger == null) {
            val newLogger: ILogger = DefaultLogger(name)
            logger = mapLoggers.putIfAbsent(name, newLogger)
            if (logger == null) {
                logger = newLogger
            }
        }
        return logger
    }

    override fun addLogListener(level: Level, logListener: LogListener?) {
        lsListeners.add(LogListenerRegistration(level, logListener))
        if (level.intValue() < minLevel.intValue()) {
            minLevel = level
        }
    }

    override fun removeLogListener(logListener: LogListener?) {
        lsListeners.remove(LogListenerRegistration(Level.ALL, logListener))
    }

    fun handleLogEvent(logEvent: LogEvent) {
        for (logListenerRegistration in lsListeners) {
            if (logEvent.logRecord.level.intValue() >= logListenerRegistration.level.intValue()) {
                logListenerRegistration.logListener!!.log(logEvent)
            }
        }
    }

    internal inner class LogListenerRegistration(var level: Level, var logListener: LogListener?) {
        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null) return false
            if (javaClass != obj.javaClass) return false
            val other = obj as LogListenerRegistration
            if (logListener == null) {
                if (other.logListener != null) return false
            } else if (logListener != other.logListener) return false
            return true
        }
    }

    internal inner class DefaultLogger(val name: String) : ILogger {
        val logger: ILogger? = loggerFactory.getLogger(name)

        override fun log(level: Level, message: String) {
            log(level, message, null)
        }

        override fun log(level: Level, message: String, thrown: Throwable?) {
            var message = message
            val loggable = logger!!.isLoggable(level)
            if (loggable || level.intValue() >= minLevel.intValue()) {
                message = "$thisAddressString [$groupName] $message"
                val logRecord: LogRecord = LogRecord(level, message)
                logRecord.setThrown(thrown)
                logRecord.setLoggerName(name)
                logRecord.setSourceClassName(name)
                val logEvent = LogEvent(logRecord, groupName, thisMember)
                if (loggable) {
                    logger.log(logEvent)
                }
                if (lsListeners.size > 0) {
                    handleLogEvent(logEvent)
                }
            }
        }

        override fun log(logEvent: LogEvent) {
            handleLogEvent(logEvent)
        }

        override val level: Level
            get() = logger?.level ?: Level.ALL

        override fun isLoggable(level: Level?): Boolean {
            return logger!!.isLoggable(level)
        }
    }
}
