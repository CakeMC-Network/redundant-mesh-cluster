package net.cakemc.meshing.redundant.logger

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class LoggerFactorySupport : LoggerFactory {
    val mapLoggers: ConcurrentMap<String, ILogger> = ConcurrentHashMap(100)

    override fun getLogger(name: String): ILogger {
        var logger = mapLoggers[name]
        if (logger == null) {
            val newLogger = createLogger(name)
            logger = mapLoggers.putIfAbsent(name, newLogger)
            if (logger == null) {
                logger = newLogger
            }
        }
        return logger
    }

    protected abstract fun createLogger(name: String?): ILogger
}
