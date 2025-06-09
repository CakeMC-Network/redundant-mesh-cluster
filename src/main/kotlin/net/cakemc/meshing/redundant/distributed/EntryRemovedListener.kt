package net.cakemc.meshing.redundant.distributed

interface EntryRemovedListener<K, V> {
    fun entryRemoved(key: K, oldValue: V)
}