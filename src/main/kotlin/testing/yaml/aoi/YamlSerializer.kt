package testing.yaml.aoi

object YamlSerializer {

    fun serialize(map: Map<String, Any>, indent: Int = 0): String {
        val builder = StringBuilder()
        val indentStr = " ".repeat(indent)
        for ((key, value) in map) {
            when (value) {
                is Map<*, *> -> {
                    builder.appendLine("$indentStr$key:")
                    @Suppress("UNCHECKED_CAST")
                    builder.append(serialize(value as Map<String, Any>, indent + 2))
                }
                is List<*> -> {
                    builder.appendLine("$indentStr$key:")
                    for (item in value) {
                        builder.appendLine("$indentStr  - $item")
                    }
                }
                else -> builder.appendLine("$indentStr$key: $value")
            }
        }
        return builder.toString()
    }
}