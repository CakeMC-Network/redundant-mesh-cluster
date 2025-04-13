package net.cakemc.meshing.redundant.session

import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthResponsePacket
import net.cakemc.meshing.redundant.networking.packet.packets.auth.AuthStatus
import java.util.UUID

object AuthService {

    private val users = mapOf(
        "test" to "test123",
        "admin" to "securepass"
    )

    fun authenticate(username: String, password: String): AuthResponsePacket {
        val actualPassword = users[username]
        return when {
            actualPassword == null -> AuthResponsePacket(
                status = AuthStatus.USER_NOT_FOUND.ordinal,
                message = "User '$username' does not exist."
            )

            actualPassword != password -> AuthResponsePacket(
                status = AuthStatus.INVALID_CREDENTIALS.ordinal,
                message = "Invalid password."
            )

            else -> AuthResponsePacket(
                status = AuthStatus.SUCCESS.ordinal,
                message = "Authentication successful.",
                sessionToken = generateSessionToken(username)
            )
        }
    }

    private fun generateSessionToken(username: String): String {
        return "$username-${UUID.randomUUID()}"
    }
}
