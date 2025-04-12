package net.cakemc.meshing.redundant

import net.cakemc.meshing.redundant.event.EventBus

class Event(val message: String)

fun main() {
  val eventBus = EventBus()

  eventBus.subscribe<Event> { event ->
    println(event.message)
  }

  eventBus.publish(Event("TEST OWO"))
}