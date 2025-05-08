package net.cakemc.meshing.redundant.logger

import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

object Logger {
    @Volatile
    private var loggerFactory: LoggerFactory? = null
    private val factoryLock = Any()

    fun getLogger(name: String): ILogger {
        if (loggerFactory == null) {
            synchronized(factoryLock) {
                if (loggerFactory == null) {
                    val loggerType = System.getProperty("logging.type")
                    loggerFactory = newLoggerFactory(loggerType)
                }
            }
        }
        return loggerFactory!!.getLogger(name)
    }

    fun getLogger(name: KClass<Any>): ILogger {
        if (loggerFactory == null) {
            synchronized(factoryLock) {
                if (loggerFactory == null) {
                    val loggerType = System.getProperty("logging.type")
                    loggerFactory = newLoggerFactory(loggerType)
                }
            }
        }
        return loggerFactory!!.getLogger(name.qualifiedName?: "class is not there?")
    }

    fun getLogger(name: Class<Any>): ILogger {
        if (loggerFactory == null) {
            synchronized(factoryLock) {
                if (loggerFactory == null) {
                    val loggerType = System.getProperty("logging.type")
                    loggerFactory = newLoggerFactory(loggerType)
                }
            }
        }
        return loggerFactory!!.getLogger(name.name?: "class is not there?")
    }

    fun newLoggerFactory(loggerType: String?): LoggerFactory {
        val loggerFactory = if (loggerType != null) {
            if ("jdk" == loggerType) {
                return StandardLoggerFactory()
            } else if ("none" == loggerType) {
                return NoLogFactory()
            } else {
                StandardLoggerFactory()
            }
        } else {
            StandardLoggerFactory()
        }
        return loggerFactory;
    }
}
