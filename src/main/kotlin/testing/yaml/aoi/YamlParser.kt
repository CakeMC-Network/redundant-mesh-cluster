package testing.yaml.aoi

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
            if (trimmed.isEmpty() || trimmed.startsWith("#")) { i++; continue }

            val currentIndent = line.indexOfFirst { !it.isWhitespace() }
            if (currentIndent < indentLevel) break

            val keyValue = trimmed.split(":", limit = 2)
            val key = keyValue[0].trim()
            val valuePart = keyValue.getOrNull(1)?.trim()

            if (valuePart.isNullOrEmpty()) {
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
            return lines.map { parseValue(it.trimStart().removePrefix("-").trim()) }.toMutableList()
        } else {
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