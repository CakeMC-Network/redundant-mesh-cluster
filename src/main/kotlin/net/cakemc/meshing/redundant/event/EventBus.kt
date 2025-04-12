package net.cakemc.meshing.redundant.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

class EventBus {

    @PublishedApi
    internal val listeners = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<(Any) -> Unit>>()

    inline fun <reified T : Any> subscribe(noinline handler: (T) -> Unit) {
        val list = listeners.computeIfAbsent(T::class) { CopyOnWriteArrayList() }
        list.add { event -> handler(event as T) }
    }

    inline fun <reified T : Any> unsubscribe(noinline handler: (T) -> Unit) {
        listeners[T::class]?.removeIf { listener ->
            listener == handler as (Any) -> Unit
        }
    }

    fun <T : Any> publish(event: T) {
        listeners[event::class]?.forEach { handler ->
            handler(event)
        }
    }

    fun clearAll() {
        listeners.clear()
    }
}
