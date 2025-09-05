package testing.yaml

import testing.yaml.aoi.YamlParser
import testing.yaml.aoi.YamlSerializer

fun main() {
    val yamlText = """
        name: ChatGPT
        version: 1.0
        features:
          - parsing
          - serialization
          - kotlin
        database:
          host: localhost
          port: 5432
          credentials:
            user: admin
            password: secret
    """.trimIndent()

    // Parsing YAML to Map
    val config = YamlParser.parse(yamlText)
    println("Parsed Map:")
    println(config)

    // Modifying the map
    config["version"] = 2.0
    (config["features"] as MutableList<String>).add("custom YAML")

    // Serializing Map back to YAML
    val yamlString = YamlSerializer.serialize(config)
    println("\nSerialized YAML:")
    println(yamlString)
}

// ------------------ YAML Parser ------------------
object YamlParser {

    fun parse(yaml: String): MutableMap<String, Any> {
        val lines = yaml.lines()
        return parseLines(lines)
    }

    private fun parseLines(lines: List<String>, indentLevel: Int = 0): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++
                continue
            }

            val currentIndent = line.indexOfFirst { !it.isWhitespace() }
            if (currentIndent < indentLevel) break

            if (trimmed.startsWith("- ")) {
                // Top-level lists are not supported in map context
                i++
                continue
            }

            val keyValue = trimmed.split(":", limit = 2)
            val key = keyValue[0].trim()
            val valuePart = keyValue.getOrNull(1)?.trim()

            if (valuePart.isNullOrEmpty()) {
                // Could be nested map or list
                val nestedLines = mutableListOf<String>()
                var j = i + 1
                while (j < lines.size && lines[j].isNotBlank() &&
                    lines[j].indexOfFirst { !it.isWhitespace() } > currentIndent
                ) {
                    nestedLines.add(lines[j])
                    j++
                }
                map[key] = parseNested(nestedLines, currentIndent + 2)
                i = j
            } else {
                map[key] = parseValue(valuePart)
                i++
            }
        }
        return map
    }

    private fun parseNested(lines: List<String>, indentLevel: Int): Any {
        if (lines.all { it.trimStart().startsWith("-") }) {
            // It's a list
            return lines.map { parseValue(it.trimStart().removePrefix("-").trim()) }.toMutableList()
        } else {
            // It's a nested map
            return parseLines(lines, indentLevel)
        }
    }

    private fun parseValue(value: String): Any {
        return when {
            value.matches(Regex("""\d+""")) -> value.toInt()
            value.matches(Regex("""\d+\.\d+""")) -> value.toDouble()
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> value
        }
    }
}

// ------------------ YAML Serializer ------------------
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
