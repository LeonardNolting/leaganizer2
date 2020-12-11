package model

import Config
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.sendMessage
import isCommand
import org.reflections.Reflections
import splitCommand
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class Task {
    abstract fun execute(): String
    open val description: String = ""
    fun syntax(parts: List<String>) = "**Syntax:**\n`" +
            (listOf("") + parts).joinToString("-") + " " +
            this::class.primaryConstructor!!.parameters.joinToString(", ") { parameter -> "[${parameter.name}]" + if (parameter.isOptional) "?" else "" } + "`"

    fun description() = if (description.isBlank()) "" else "**Description:**\n$description"
    fun help(parts: List<String>) = description() + syntax(parts)

    class Addition(vararg val strings: String, val action: suspend Builder.() -> Unit)
    data class Builder(
        val bot: Bot,
        val user: User,
        val channelId: String,
        val clazz: KClass<Task>,
        val addition: Addition
    ) {
        var state = State.ACTIVE
        private val command = clazz.annotations.find { it is Command } as Command
        val mode = command.mode.primaryConstructor!!.call(this, bot, channelId)
        val parameters = Parameters(clazz.primaryConstructor!!.parameters)
        val isReady: Boolean
            get() = parameters.isReady
        val isFilled: Boolean
            get() = parameters.isFilled

        fun syntax() = "**Syntax:**\n`" +
                (listOf("") + command.parts.toList()).joinToString("-") + " " +
                this::class.primaryConstructor!!.parameters.joinToString(", ") { parameter -> "[${parameter.name}]" + if (parameter.isOptional) "?" else "" }

        fun description() = if (command.description.isBlank()) "" else "**Description:**\n${command.description}"
        fun help() = description() + syntax()

        suspend fun run() {
            addition.action(this)
        }

        private suspend fun reply(text: String) = bot.clientStore.channels[channelId].sendMessage(text)

        fun exit() {
            state = State.INACTIVE
        }

        enum class State {
            ACTIVE,
            INACTIVE
        }

        companion object {
            private val reflections = Reflections("")
            private val annotation = Command::class.java

            @Suppress("UNCHECKED_CAST")
            val commands = reflections.getTypesAnnotatedWith(annotation).map { clazz ->
                clazz.getAnnotation(annotation).parts.toList() to (clazz.kotlin as KClass<Task>)
            }.toMap()

            private val additions = listOf(
                Addition("help") {
                    reply(help())
                },
                Addition {
                    if (!parameters.isReady) throw Exception("There are missing parameters.")
                    val task = clazz.primaryConstructor!!.callBy(parameters.out())
                    reply(task.execute())
                    exit()
                }
            )

            suspend fun from(bot: Bot, message: Message): Builder {
                if (!message.content.isCommand) throw Exception("Cannot generate Task from message because it's not a command.")
                val (partsString, rest) = splitCommand(message.content)
                val parts = partsString.split(Config.Commands.prefix).filter { it.isNotBlank() }
                val addition = additions.find { addition -> parts.takeLast(addition.strings.size) == addition.strings.toList() }!!
                val clazz = commands[parts.dropLast(addition.strings.size)]
                    ?: throw Exception("Command not found.")

                val builder = Builder(bot, message.author, message.channelId, clazz, addition)

                builder.parameters.meta(bot, message)

                return builder
            }
        }
    }
}