package model

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.Bot
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class Type<T>(
    val kType: KType,
    val name: String,
    val unit: String,
    val description: String,
    val examples: List<String>,
    val meta: (suspend Bot.(Command.Meta.Type, Message) -> T)?,
    private val parseString: (suspend Bot.(String) -> T)?
) {
    suspend fun parse(bot: Bot, string: String): T? {
//        if (string == "NOTHING") return null
        return (parseString ?: throw Exception("No parse method given for type $name.")).invoke(bot, string)
    }
    companion object {
        @Suppress("RemoveExplicitTypeArguments")
        val types = listOf(
            type<ChannelClient>("Channel", meta = { type, message ->
                if (type == Command.Meta.Type.CHANNEL) message.channel
                else throw Exception()
            }) { string ->
                val match = Regex("^<#(\\d+)>$").find(string) ?: throw Exception("Cannot parse channel.")
                return@type (clientStore.channels[match.groupValues[1]])
            },
            type<Message>("Message", meta = { type, message ->
                if (type == Command.Meta.Type.MESSAGE) message
                else throw Exception()
            }),
            type<User>("Mention", meta = { type, message ->
                if (type == Command.Meta.Type.AUTHOR) message.author
                else throw Exception()
            }) { string ->
                val match = Regex("^<@!?(\\d+)>$").find(string) ?: throw Exception("Cannot parse mention.")
                clientStore.discord.getUser(match.groupValues[1])
            },
            type<Boolean>("Switch") { string ->
                when (string) {
                    "true", "yes" -> true
                    "false", "no" -> false
                    else -> throw Exception()
                }
            },
            type<Int>("Integer") { string -> string.toInt() },
            type<Float>("Float") { string -> string.toFloat() },
            type<String>("Text") { string -> string },
        )

        fun get(kType: KType) = types.find { type -> type.kType == kType } ?: throw Exception("No type object given for type ${kType}.")

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> get(): Type<out T>? = types.find { type -> type.kType == typeOf<T>() } as Type<T>?

        private inline fun <reified T> type(
            name: String,
            unit: String = "",
            description: String = "",
            examples: List<String> = listOf(),
            noinline meta: (suspend Bot.(Command.Meta.Type, Message) -> T)? = null,
            noinline parseString: (suspend Bot.(String) -> T)? = null
        ) = Type(typeOf<T>(), name, unit, description, examples, meta, parseString)
    }
}