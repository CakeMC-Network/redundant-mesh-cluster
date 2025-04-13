package net.cakemc.meshing.redundant.session

data class Session(
    val token: String,
    val username: String,
    val expiresAt: Long
)

