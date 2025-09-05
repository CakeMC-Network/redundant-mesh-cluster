package testing.yaml.aoi

import testing.yaml.YamlParser
import testing.yaml.YamlSerializer
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class YamlConfig(private val data: MutableMap<String, Any>) {

    companion object {
        fun fromYaml(yaml: String): YamlConfig {
            return YamlConfig(YamlParser.parse(yaml))
        }

        fun fromFile(filePath: String): YamlConfig {
            val path: Path = Paths.get(filePath)
            if (!Files.exists(path))
                throw IllegalArgumentException("File does not exist: $filePath")

            val content = Files.readString(path, StandardCharsets.UTF_8)
            return fromYaml(content)
        }


        fun <T : Any> fromObject(obj: T): YamlConfig {
            val container = YamlConfig(mutableMapOf())
            val clazz = obj.javaClass

            for (field in clazz.declaredFields) {
                if (Modifier.isStatic(field.modifiers) ||
                    Modifier.isTransient(field.modifiers)) {
                    continue
                }

                field.isAccessible = true
                val value = field.get(obj)
                if (value != null) {
                    container.append(field.name, value)
                }
            }
            return container
        }
    }

    fun append(key: String, value: Any) {
        data[key] = value
    }

    fun remove(key: String) {
        data.remove(key)
    }

    fun contains(key: String): Boolean = data.containsKey(key)

    fun get(key: String): Any? = data[key]

    fun <T : Any> getAsObject(clazz: Class<T>): T {
        val instance = clazz.getDeclaredConstructor().newInstance()

        for (field in clazz.declaredFields) {
            if (Modifier.isStatic(field.modifiers) || Modifier.isTransient(field.modifiers)) {
                continue
            }
            if (contains(field.name)) {
                field.isAccessible = true
                val value = get(field.name)

                try {
                    field.set(instance, value)
                } catch (e: Exception) {
                    println("Skipping field ${field.name}: ${e.message}")
                }
            }
        }
        return instance
    }

    fun getString(key: String): String? = data[key] as? String

    fun getInt(key: String): Int? = when (val v = data[key]) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

    fun getLong(key: String): Long? = when (val v = data[key]) {
        is Long -> v
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }

    fun getShort(key: String): Short? = when (val v = data[key]) {
        is Short -> v
        is Number -> v.toShort()
        is String -> v.toShortOrNull()
        else -> null
    }

    fun getByte(key: String): Byte? = when (val v = data[key]) {
        is Byte -> v
        is Number -> v.toByte()
        is String -> v.toByteOrNull()
        else -> null
    }

    fun getDouble(key: String): Double? = when (val v = data[key]) {
        is Double -> v
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }

    fun getFloat(key: String): Float? = when (val v = data[key]) {
        is Float -> v
        is Number -> v.toFloat()
        is String -> v.toFloatOrNull()
        else -> null
    }

    fun getBoolean(key: String): Boolean? = when (val v = data[key]) {
        is Boolean -> v
        is String -> v.lowercase() in listOf("true", "yes", "1")
        is Number -> v.toInt() != 0
        else -> null
    }

    fun getStringList(key: String): List<String>? = getList(key)?.mapNotNull { it as? String }

    fun getIntList(key: String): List<Int>? = getList(key)?.mapNotNull {
        when (it) {
            is Int -> it
            is Number -> it.toInt()
            is String -> it.toIntOrNull()
            else -> null
        }
    }

    fun getLongList(key: String): List<Long>? = getList(key)?.mapNotNull {
        when (it) {
            is Long -> it
            is Number -> it.toLong()
            is String -> it.toLongOrNull()
            else -> null
        }
    }

    fun getShortList(key: String): List<Short>? = getList(key)?.mapNotNull {
        when (it) {
            is Short -> it
            is Number -> it.toShort()
            is String -> it.toShortOrNull()
            else -> null
        }
    }

    fun getByteList(key: String): List<Byte>? = getList(key)?.mapNotNull {
        when (it) {
            is Byte -> it
            is Number -> it.toByte()
            is String -> it.toByteOrNull()
            else -> null
        }
    }

    fun getFloatList(key: String): List<Float>? = getList(key)?.mapNotNull {
        when (it) {
            is Float -> it
            is Number -> it.toFloat()
            is String -> it.toFloatOrNull()
            else -> null
        }
    }

    fun getDoubleList(key: String): List<Double>? = getList(key)?.mapNotNull {
        when (it) {
            is Double -> it
            is Number -> it.toDouble()
            is String -> it.toDoubleOrNull()
            else -> null
        }
    }

    fun getBooleanList(key: String): List<Boolean>? = getList(key)?.mapNotNull {
        when (it) {
            is Boolean -> it
            is String -> it.lowercase() in listOf("true", "yes", "1")
            is Number -> it.toInt() != 0
            else -> null
        }
    }

    fun getStringArray(key: String): Array<String>? = getStringList(key)?.toTypedArray()
    fun getIntArray(key: String): IntArray? = getIntList(key)?.toIntArray()
    fun getLongArray(key: String): LongArray? = getLongList(key)?.toLongArray()
    fun getShortArray(key: String): ShortArray? = getShortList(key)?.toShortArray()
    fun getByteArray(key: String): ByteArray? = getByteList(key)?.toByteArray()
    fun getFloatArray(key: String): FloatArray? = getFloatList(key)?.toFloatArray()
    fun getDoubleArray(key: String): DoubleArray? = getDoubleList(key)?.toDoubleArray()
    fun getBooleanArray(key: String): BooleanArray? = getBooleanList(key)?.toBooleanArray()

    fun getMap(key: String): YamlConfig? {
        val map = data[key]
        return if (map is MutableMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (YamlConfig(map as MutableMap<String, Any>))
        } else null
    }

    fun getList(key: String): MutableList<Any>? {
        val list = data[key]
        return if (list is MutableList<*>) {
            @Suppress("UNCHECKED_CAST")
            list as MutableList<Any>
        } else null
    }

    fun keys(): Set<String> = data.keys

    fun toYaml(): String = YamlSerializer.serialize(data)

    fun writeToFile(filePath: String) {
        val path: Path = Paths.get(filePath)
        Files.writeString(path, toYaml(), StandardCharsets.UTF_8)
    }
}