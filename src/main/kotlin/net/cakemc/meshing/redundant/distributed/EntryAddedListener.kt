package net.cakemc.meshing.redundant.distributed

interface EntryAddedListener<K, V> {
    fun entryAdded(key: K, value: V)
}