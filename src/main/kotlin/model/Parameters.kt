package model

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import isMeta
import metaType
import kotlin.reflect.KParameter

class Parameters(private val list: List<KParameter> = mutableListOf()) :
    MutableMap<KParameter, Parameters.Optional> by (list.associateWith { Optional.Nothing }.toMutableMap()) {
    val isReady: Boolean
        get() = none { (parameter, value) -> value is Optional.Nothing && !parameter.isOptional }
    val isFilled: Boolean
        get() = values.none { it is Optional.Nothing }

    suspend fun meta(bot: Bot, message: Message) {
        map { (parameter, _) ->
            val metaType = parameter.metaType
            if (metaType != null) {
                val type = Type.get(parameter.type)
                val meta =
                    type.meta ?: throw Exception("No meta method given for type ${parameter.type}.")

                this[parameter] = Optional.Value(
                    try {
                        meta(bot, metaType, message)
                    } catch (e: Exception) {
                        throw Exception("Could not parse expected type (${type.name}) for meta type ($metaType).")
                    }
                )
            }
        }
    }

    private suspend fun set(bot: Bot, name: String?, value: String) {
        val parameter = if (name == null) null else {
            keys.find { parameter -> parameter.name == name }
                ?: throw Exception("Could not insert named parameter because no parameter with the name `$name` is needed. Ignoring input `$value`.")
        } ?: keys.find { key -> !key.isMeta }
        ?: throw Exception("Could not insert value because no empty non meta parameter was found.")

        if (parameter.isMeta) throw Exception("Cannot set meta parameter (${parameter.name}) manually.")

        val type = Type.get(parameter.type)

        this[parameter] = Optional.Value(
            try {
                type.parse(bot, value)
            } catch (e: Exception) {
                throw Exception("Could not parse expected type (${type.name}) from input string `$value`: ${e.message}")
            }
        )
    }

    private suspend fun set(bot: Bot, parameter: Parameter) = set(bot, parameter.name, parameter.value)

    suspend fun process(bot: Bot, string: String) {
        Parser.parameters(string).forEach { parameter ->
            try {
                set(bot, parameter)
            } catch (e: Exception) {
                throw Exception("Processing parameter `${parameter.name}` failed:\n" + e.message)
            }
        }
    }

    fun out(): Map<KParameter, Any?> = this
        .filter { (_, value) -> value is Optional.Value }
        .map { (parameter, value) -> parameter to (value as Optional.Value).value }
        .toMap()

    private sealed class Optional {
        object Nothing : Optional()
        data class Value(val value: Any?) : Optional()
    }

    private data class Parameter(val name: String?, val value: String, val quoted: Boolean = false)
    private object Parser {
        fun parameters(input: String): List<Parameter> {
            val output = mutableListOf<Parameter>()
            var inString = false
            var escaping = false
            var currentKey: String? = null
            var current = ""

            fun tryAdd() {
                val name = currentKey?.trim()
                val value = current.trim()
                if ((name == null || name.isNotEmpty()) && value.isNotEmpty())
                    output += Parameter(name, value)
            }

            input.forEach { char ->
                when {
                    escaping -> {
                        escaping = false
                        current += char
                    }
                    char == '\\' -> escaping = true
                    char == '"' -> inString = !inString
                    else -> {
                        when {
                            // Char in string
                            inString -> current += char
                            // Comma
                            char == ',' -> {
                                tryAdd()
                                current = ""
                                currentKey = null
                            }
                            // Named parameter
                            char == '=' -> {
                                currentKey = current
                                current = ""
                            }
                            // Char outside of string
                            else -> current += char
                        }
                    }
                }
                return@forEach
            }
            tryAdd()
            return output
        }
    }
}