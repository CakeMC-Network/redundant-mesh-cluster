package net.cakemc.meshing.redundant.distributed

import java.util.*

abstract class DistributedMap<K, V> {
    abstract fun put(key: K, value: V)

    abstract fun remove(key: K)
    abstract fun removeIf(predicate: (K, V) -> Boolean)

    abstract fun clear()
    abstract fun get(key: K): V?

    abstract fun containsKey(key: K): Boolean
    abstract fun containsValue(value: V): Boolean

    abstract fun size(): Int
    abstract fun isEmpty(): Boolean
    abstract fun keys(): Set<K>
    abstract fun values(): Collection<V>

    abstract fun addEntryAddedListener(
        listener: EntryAddedListener<K, V>,
        keyFilter: ((K) -> Boolean)? = null
    ): UUID
    abstract fun addEntryRemovedListener(
        listener: EntryRemovedListener<K, V>,
        keyFilter: ((K) -> Boolean)? = null
    ): UUID

    abstract fun addEntryUpdatedListener(
        listener: EntryUpdatedListener<K, V>,
        keyFilter: ((K) -> Boolean)? = null
    ): UUID

    abstract fun removeEntryListener(id: UUID): Boolean

    abstract fun resolveName(): String
}