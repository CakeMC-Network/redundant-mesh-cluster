package net.cakemc.meshing.redundant.distributed

import net.cakemc.meshing.redundant.event.impl.PacketReceivedEvent
import net.cakemc.meshing.redundant.networking.EndPoint
import net.cakemc.meshing.redundant.networking.codec.Packet
import net.cakemc.meshing.redundant.networking.codec.PacketType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.thread

class SimpleDistributedMap<K, V>(
    private val endpoint: EndPoint,
    private val nodeId: String,
    private val mapName: String,
    private val autoPropagate: Boolean = true
): DistributedMap<K, V>() {
    private val map = ConcurrentHashMap<K, DistributedValue<V>>()

    private val listeners = CopyOnWriteArraySet<ListenerRegistration<K, V>>()

    init {
        endpoint.eventBus().subscribe<PacketReceivedEvent> {
            val packet = it.packet
            if (packet.channel != "distributed-map") return@subscribe

            val payload = unwrapPayload(packet.payload) ?: return@subscribe

            when (packet.topic) {
                "put" -> {
                    val (k, v, ts) = parseEntry(payload)
                    val oldValue = map[k]?.value
                    val merged = map.merge(k, DistributedValue(v, ts)) { old, new ->
                        if (new.timestamp > old.timestamp) new else old
                    }

                    if (merged != null && merged.value == v) {
                        if (oldValue == null) {
                            notifyListeners(k, null, v, EntryEventType.ADDED)
                        } else if (oldValue != v) {
                            notifyListeners(k, oldValue, v, EntryEventType.UPDATED)
                        }
                    }
                }

                "remove" -> {
                    val (k, ts) = parseKeyWithTimestamp(payload)
                    val oldValue = map[k]?.value
                    val removed = map.computeIfPresent(k) { _, old ->
                        if (ts > old.timestamp) null else old
                    }
                    if (removed == null && oldValue != null) {
                        notifyListeners(k, oldValue, null, EntryEventType.REMOVED)
                    }
                }

                "sync-request" -> {
                    for ((k, dv) in map) {
                        val payload = serializeEntry(k, dv.value, dv.timestamp)
                        endpoint.handler().sendToAllSync(
                            Packet(PacketType.NORMAL, nodeId, "distributed-map", "sync-data", wrapPayload(payload))
                        )
                    }
                }

                "sync-data" -> {
                    val (k, v, ts) = parseEntry(payload)
                    val oldValue = map[k]?.value
                    val merged = map.merge(k, DistributedValue(v, ts)) { old, new ->
                        if (new.timestamp > old.timestamp) new else old
                    }
                    if (merged != null && merged.value == v) {
                        if (oldValue == null) {
                            notifyListeners(k, null, v, EntryEventType.ADDED)
                        } else if (oldValue != v) {
                            notifyListeners(k, oldValue, v, EntryEventType.UPDATED)
                        }
                    }
                }
            }
        }

        thread(start = true, isDaemon = true) {
            requestSync()
        }
    }

    private fun requestSync() {
        endpoint.handler().sendToAllSync(
            Packet(PacketType.NORMAL, nodeId, "distributed-map", "sync-request", "")
        )
    }

    override fun put(key: K, value: V) {
        val timestamp = System.currentTimeMillis()
        val oldValue = map[key]?.value
        map[key] = DistributedValue(value, timestamp)
        if (autoPropagate) {
            val payload = serializeEntry(key, value, timestamp)
            endpoint.handler().sendToAllSync(
                Packet(PacketType.NORMAL, nodeId, "distributed-map", "put", wrapPayload(payload))
            )
        }
        if (oldValue == null) {
            notifyListeners(key, null, value, EntryEventType.ADDED)
        } else if (oldValue != value) {
            notifyListeners(key, oldValue, value, EntryEventType.UPDATED)
        }
    }

    override fun remove(key: K) {
        val timestamp = System.currentTimeMillis()
        val oldValue = map.remove(key)?.value
        if (autoPropagate) {
            val payload = serializeKeyWithTimestamp(key, timestamp)
            endpoint.handler().sendToAllSync(
                Packet(PacketType.NORMAL, nodeId, "distributed-map", "remove", wrapPayload(payload))
            )
        }
        if (oldValue != null) {
            notifyListeners(key, oldValue, null, EntryEventType.REMOVED)
        }
    }

    override fun removeIf(predicate: (K, V) -> Boolean) {
        val toRemove = mutableListOf<Pair<K, Long>>()
        map.forEach { (k, dv) ->
            if (predicate(k, dv.value)) {
                toRemove.add(k to dv.timestamp)
            }
        }
        toRemove.forEach { (k, ts) ->
            val oldValue = map[k]?.value
            val removed = map.computeIfPresent(k) { _, old ->
                if (ts >= old.timestamp) null else old
            }
            if (removed == null && oldValue != null) {
                notifyListeners(k, oldValue, null, EntryEventType.REMOVED)
                if (autoPropagate) {
                    val payload = serializeKeyWithTimestamp(k, System.currentTimeMillis())
                    endpoint.handler().sendToAllSync(
                        Packet(PacketType.NORMAL, nodeId, "distributed-map", "remove", wrapPayload(payload))
                    )
                }
            }
        }
    }

    override fun clear() {
        val now = System.currentTimeMillis()
        val keysSnapshot = map.keys.toList()
        val oldValues = keysSnapshot.mapNotNull { k -> map[k]?.value }
        map.clear()
        if (autoPropagate) {
            keysSnapshot.forEach { key ->
                val payload = serializeKeyWithTimestamp(key, now)
                endpoint.handler().sendToAllSync(
                    Packet(PacketType.NORMAL, nodeId, "distributed-map", "remove", wrapPayload(payload))
                )
            }
        }
        keysSnapshot.zip(oldValues).forEach { (key, oldValue) ->
            notifyListeners(key, oldValue, null, EntryEventType.REMOVED)
        }
    }

    override fun get(key: K): V? = map[key]?.value

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean = map.values.any { it.value == value }

    override fun size(): Int = map.size

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun keys(): Set<K> = map.keys

    override fun values(): Collection<V> = map.values.map { it.value }

    private fun wrapPayload(payload: String): String {
        return "$mapName###$payload"
    }

    private fun unwrapPayload(wrapped: String): String? {
        val parts = wrapped.split("###", limit = 2)
        return if (parts.size == 2 && parts[0] == mapName) parts[1] else null
    }

    private fun notifyListeners(key: K, oldValue: V?, newValue: V?, eventType: EntryEventType) {
        @Suppress("UNCHECKED_CAST")
        listeners.forEach { reg ->
            if (reg.type != eventType) return@forEach
            if (reg.keyFilter != null && !reg.keyFilter.invoke(key)) return@forEach

            when (eventType) {
                EntryEventType.ADDED -> {
                    (reg.listener as EntryAddedListener<K, V>).entryAdded(key, newValue!!)
                }
                EntryEventType.UPDATED -> {
                    (reg.listener as EntryUpdatedListener<K, V>).entryUpdated(key, oldValue!!, newValue!!)
                }
                EntryEventType.REMOVED -> {
                    (reg.listener as EntryRemovedListener<K, V>).entryRemoved(key, oldValue!!)
                }
            }
        }
    }

    override fun addEntryAddedListener(
        listener: EntryAddedListener<K, V>,
        keyFilter: ((K) -> Boolean)?
    ): UUID {
        val reg = ListenerRegistration<K, V>(UUID.randomUUID(), EntryEventType.ADDED, listener, keyFilter)
        listeners.add(reg)
        return reg.id
    }

    override fun addEntryRemovedListener(
        listener: EntryRemovedListener<K, V>,
        keyFilter: ((K) -> Boolean)?
    ): UUID {
        val reg = ListenerRegistration<K, V>(UUID.randomUUID(), EntryEventType.REMOVED, listener, keyFilter)
        listeners.add(reg)
        return reg.id
    }

    override fun addEntryUpdatedListener(
        listener: EntryUpdatedListener<K, V>,
        keyFilter: ((K) -> Boolean)?
    ): UUID {
        val reg = ListenerRegistration<K, V>(UUID.randomUUID(), EntryEventType.UPDATED, listener, keyFilter)
        listeners.add(reg)
        return reg.id
    }

    override fun removeEntryListener(id: UUID): Boolean {
        return listeners.removeIf { it.id == id }
    }

    private fun serializeEntry(k: K, v: V, timestamp: Long): String {
        return "$k::$v::$timestamp"
    }

    private fun parseEntry(payload: String): Triple<K, V, Long> {
        val parts = payload.split("::")
        @Suppress("UNCHECKED_CAST")
        return Triple(parts[0] as K, parts[1] as V, parts[2].toLong())
    }

    private fun serializeKeyWithTimestamp(k: K, ts: Long): String = "$k::$ts"

    private fun parseKeyWithTimestamp(payload: String): Pair<K, Long> {
        val parts = payload.split("::")
        @Suppress("UNCHECKED_CAST")
        return parts[0] as K to parts[1].toLong()
    }

    override fun resolveName(): String {
        return mapName
    }
}