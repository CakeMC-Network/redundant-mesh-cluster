package net.cakemc.meshing.redundant.event.impl

import io.netty.channel.Channel
import net.cakemc.skrilla.networking.packet.Packet

class PacketReceivedEvent(val channel: Channel, val packet: Packet)