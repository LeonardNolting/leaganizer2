package model

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.sendMessage
import parameters
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Command(
    vararg val parts: String,
    val mode: KClass<out Mode> = Mode.Default::class,
    val permission: Permission = Permission.EVERYBODY,
    val description: String = ""
) {
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Meta(val type: Type) {
        enum class Type {
            AUTHOR,
            CHANNEL,
            MESSAGE
        }
    }

    abstract class Mode(
        protected val builder: Task.Builder,
        private val bot: Bot,
        private val channelId: String,
    ) {
        var counter = 0

        suspend fun message(string: String) {
            process(string, counter)
            counter++
        }

        suspend fun run() = try {
            builder.run()
        } catch (e: Exception) {
            reply("Running command failed:\n" + e.message)
        }

        fun exit() = builder.exit()

        suspend fun processParameters(string: String) {
            builder.parameters.process(bot, parameters(string))
        }

        suspend fun reply(text: String) = bot.clientStore.channels[channelId].sendMessage(text)

        abstract suspend fun process(string: String, counter: Int)

        class Default(
            builder: Task.Builder,
            bot: Bot,
            channelId: String,
        ) : Mode(builder, bot, channelId) {
            override suspend fun process(string: String, counter: Int) {
                processParameters(string)
                run()
            }
        }

        sealed class Continuous(
            builder: Task.Builder,
            bot: Bot,
            channelId: String,
            keywords: List<Keyword> = listOf()
        ) : Mode(builder, bot, channelId) {
            private val keywords = keywords + listOf(
                Keyword("help", "Get help with continuous mode.") { reply(help) },
                Keyword("submit", "Run command.") { run() },
                Keyword("exit", "Exit continuous mode.") {
                    reply("Exit continuous mode.")
                    exit()
                }
            )

            private suspend fun info() =
                reply("Switched to continuous mode. If you need help with this, type `help`.\nPossible keywords:\n" +
                        keywords.joinToString("\n") { keyword -> keyword.toStringDiscord() })

            final override suspend fun process(string: String, counter: Int) {
                val action = keywords.find { keyword -> keyword.value == string.trim() }?.action
                if (action != null) action()
                else {
                    processParameters(string)
                    when {
                        shouldRun(string, counter) -> run()
                        couldRun(
                            string,
                            counter
                        ) -> reply("All necessary parameters have values. If you want to execute the command, type `submit`.")
                        else -> {
                            if (counter == 0) info()
                            reply("The following parameters are missing: TODO") // TODO
                        }
                    }
                }
            }

            abstract fun couldRun(string: String, counter: Int): Boolean
            abstract fun shouldRun(string: String, counter: Int): Boolean

            private val help: String
                get() = """
                    Messages quickly get messy when you have more than 4 parameters. Continuous mode addresses this problem by allowing you to add parameters 'message by message'. This means, instead of having to write one long text, you can now just type the command and add any parameters afterwards!
                    $helpAddition
                    For example (bot responses omitted):
                    `-tournament-register`
                    `team_size = 1`
                    `max_players = 30`
                """.trimIndent()
            abstract val helpAddition: String

            class Possible(
                builder: Task.Builder,
                bot: Bot,
                channelId: String,
            ) : Continuous(builder, bot, channelId) {
                override fun couldRun(string: String, counter: Int) = builder.isReady
                override fun shouldRun(string: String, counter: Int) = builder.isFilled
                override val helpAddition =
                    "The command will be automatically executed as soon as all necessary parameters are given!"
            }

            class Always(
                builder: Task.Builder,
                bot: Bot,
                channelId: String,
            ) : Continuous(
                builder, bot, channelId
            ) {
                override fun couldRun(string: String, counter: Int) = false
                override fun shouldRun(string: String, counter: Int) = false
                override val helpAddition = "Type `submit` to finish the command."
            }

            private data class Keyword(
                val value: String,
                val description: String = "",
                val action: suspend Mode.() -> Unit
            ) {
                fun toStringDiscord() = "`$value`: $description"
            }
        }
    }
}

//data class Command(val name: String?, val action: suspend CommandData.() -> Unit)