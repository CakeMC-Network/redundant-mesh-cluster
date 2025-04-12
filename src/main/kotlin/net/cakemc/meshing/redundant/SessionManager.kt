package net.cakemc.meshing.redundant

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SessionManager {

    private const val EXPIRATION_MS = 10 * 60 * 1000 // 10 minutes

    private val sessions = ConcurrentHashMap<String, Session>()

    fun createSession(username: String): String {
        val token = UUID.randomUUID().toString()
        val expiration = System.currentTimeMillis() + EXPIRATION_MS
        val session = Session(token, username, expiration)
        sessions[token] = session
        return token
    }

    fun validateSession(token: String): Boolean {
        val session = sessions[token]
        return session != null && session.expiresAt > System.currentTimeMillis()
    }

    fun renewSession(token: String): Session? {
        val session = sessions[token]
        return if (session != null && session.expiresAt > System.currentTimeMillis()) {
            val newSession = session.copy(expiresAt = System.currentTimeMillis() + EXPIRATION_MS)
            sessions[token] = newSession
            newSession
        } else {
            null
        }
    }

    fun invalidateSession(token: String) {
        sessions.remove(token)
    }

    fun getSessionUser(token: String): String? {
        return sessions[token]?.username
    }
}