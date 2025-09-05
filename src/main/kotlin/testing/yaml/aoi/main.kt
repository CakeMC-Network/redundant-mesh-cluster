package testing.yaml.aoi

fun main() {
    writeObjectToFile()
    readObjectFromFile()
}

fun writeObjectToFile() {
    val config = YamlConfig.fromObject(TestObject(
        "lisa", 10243, listOf("test", "test2", "test3"),
        mapOf(Pair("test", "test123"), Pair("port", 10213))
    ))

    config.writeToFile("config.yml")
}

fun readObjectFromFile() {
    val config = YamlConfig.fromFile("config.yml")
    val test = config.getAsObject(TestObject::class.java)
    println(config.getInt("age"))

    println(test)
}

fun defaultYaml() {
    val yamlText = """
        name: Test
        version: 1.0
        features:
          - test1
          - test2
          - test3
        database:
          host: localhost
          port: 5432
          credentials:
            user: admin
            password: secret
    """.trimIndent()

    val config = YamlConfig.fromYaml(yamlText)

    println("Original Config:")
    println(config.toYaml())

    // Using mapper methods
    config.append("author", "OpenAI")
    config.getList("features")?.add("test4")
    config.getMap("database")?.remove("credentials")

    println("\nModified Config:")
    println(config.toYaml())

    println("\nKeys at top level: ${config.keys()}")
    println("Contains 'version'? ${config.contains("version")}")
    println("Version: ${config.get("version")}")

    println(config.getDouble("version")?.plus(2))

    config.writeToFile("config.yml")
}

data class TestObject(val name: String, val age: Int, val list: List<String>, val map: Map<String, Any>) {
    constructor(): this("", 0, listOf(), mapOf())
}