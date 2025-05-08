package net.cakemc.meshing.redundant.logger

interface LoggerFactory {
    fun getLogger(name: String): ILogger
}
