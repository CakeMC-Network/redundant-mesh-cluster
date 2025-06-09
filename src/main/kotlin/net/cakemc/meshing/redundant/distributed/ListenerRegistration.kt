package net.cakemc.meshing.redundant.distributed

import java.util.*

data class ListenerRegistration<K, V>(
    val id: UUID = UUID.randomUUID(),
    val type: EntryEventType,
    val listener: Any, // One of the listener interfaces
    val keyFilter: ((K) -> Boolean)? = null
)