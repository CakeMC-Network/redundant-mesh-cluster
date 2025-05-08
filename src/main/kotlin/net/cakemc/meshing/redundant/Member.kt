package net.cakemc.meshing.redundant

class Member(
    val identifier: String,
    val address: MemberAddress
) {

    constructor(name: String, host: String, port: Int): this(name, MemberAddress(host, port))

    data class MemberAddress(val host: String, val port: Int)

}