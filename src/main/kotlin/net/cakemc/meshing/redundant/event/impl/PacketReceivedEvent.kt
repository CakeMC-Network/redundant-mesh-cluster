package net.cakemc.meshing.redundant.event.impl

import io.netty.channel.Channel
import net.cakemc.meshing.redundant.networking.codec.Packet

class PacketReceivedEvent(val channel: Channel, val packet: Packet)