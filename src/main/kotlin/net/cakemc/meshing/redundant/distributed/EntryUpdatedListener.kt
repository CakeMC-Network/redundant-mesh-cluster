package net.cakemc.meshing.redundant.distributed

interface EntryUpdatedListener<K, V> {
    fun entryUpdated(key: K, oldValue: V, newValue: V)
}