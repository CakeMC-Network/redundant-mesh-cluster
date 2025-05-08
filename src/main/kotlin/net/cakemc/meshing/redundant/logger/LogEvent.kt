package net.cakemc.meshing.redundant.logger

import net.cakemc.meshing.redundant.Member
import java.util.*
import java.util.logging.LogRecord

class LogEvent(
    val logRecord: LogRecord,
    val groupName: String,
    var member: Member
) : EventObject(member) {}
