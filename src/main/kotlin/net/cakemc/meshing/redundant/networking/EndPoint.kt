package net.cakemc.meshing.redundant.networking

import net.cakemc.meshing.redundant.event.EventBus
import net.cakemc.skrilla.networking.handler.ConnectionHandler

interface EndPoint {
    fun start()
    fun close()
    fun handler(): ConnectionHandler
    fun eventBus(): EventBus
}

