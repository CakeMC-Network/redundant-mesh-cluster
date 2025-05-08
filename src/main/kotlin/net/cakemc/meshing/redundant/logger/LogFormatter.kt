package net.cakemc.meshing.redundant.logger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Formatter
import java.util.logging.LogRecord

class LogFormatter : Formatter() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    override fun format(record: LogRecord): String {
        val timestamp = dateFormat.format(Date(record.millis))
        return "[$timestamp] [${record.loggerName}] ${record.message}\n"
    }
}
