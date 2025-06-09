package testing

import net.cakemc.meshing.redundant.networking.EndPoint

fun main() {
    val map1 = EndPoint.createEndPointAndConnect().map<String, String>("test")
    val map2 = EndPoint.createEndPointAndConnect().map<String, String>("test")
    val map3 = EndPoint.createEndPointAndConnect().map<String, String>("test")
    val map4 = EndPoint.createEndPointAndConnect().map<String, String>("test")

    Thread.sleep(2000) // allow sync

    map1.put("user", "alice")
    Thread.sleep(500)
    map2.put("user2", "bob") // conflicting put

    Thread.sleep(2000)

    println("map1[user] = ${map1.get("user2")}")
    println("map2[user] = ${map2.get("user")}")
    println("map2[user] = ${map3.get("user")}")
    println("map2[user] = ${map3.get("user2")}")
    println("map2[user] = ${map4.get("user2")}")
}

