import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.bot
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import model.Task

val env = dotenv()

val token = env["BOT_TOKEN"]!!

class Builders(private val list: MutableList<Task.Builder> = mutableListOf()) :
    MutableList<Task.Builder> by list {
    fun active(user: User, channelId: String) = find { builder ->
        builder.user == user &&
                builder.state == Task.Builder.State.ACTIVE &&
                builder.channelId == channelId
    }

    override fun add (element: Task.Builder) = list.add(element)
}

lateinit var guild: GuildClient

suspend fun main() {
    bot(token) {
        guild = clientStore.guilds["772844585751805972"]
        val builders = Builders()
        messageCreated { message ->
            var builder = builders.active(message.author, message.channelId)
            if (message.content.isCommand) {
                if (builder != null) {
                    builder.state = Task.Builder.State.INACTIVE
                    message.reply("Stopped current command.")
                }

                builder = try {
                    Task.Builder.from(this@bot, message)
                } catch (e: Exception) {
                    message.reply("Creating command failed:\n${e.message}")
                    null
                }

                if (builder != null) builders.add(builder)
            }

            builder?.mode?.message(message.content)
        }

        /*listen("-") {
            commands("tournament") {
                command("register") {
                    val registerTournament = ::registerTournament
                    registerTournament.parameters.forEach {parameter -> parameter.type}
                }
            }

            commands("statistics") {
                commands("tournament") {
                    command("round") {

                    }
                    command("game") {

                    }
                    command("match") {

                    }
                }
                command("player") {

                }
                command {

                }
            }
            command("help") {

            }
        }*/
    }
}

/*
fun Bot.listen(prefix: String, block: CommandManager.() -> Unit) {
    val manager = CommandManager().apply(block)

    messageCreated { message ->
        playersVerify(message.usersMentioned[0])
        if (!message.content.startsWith(prefix)) return@messageCreated
        val (partsString, rest) = (message.content + " ").split(' ', limit = 2)
        val parts = partsString.split(prefix).filter { it.isNotBlank() }
        var index = 0
        var name: String?
        var currentManager = manager
        var possibleManager: CommandManager?
        var currentCommand: Command? = null
        var possibleCommand: Command? = null

        while (true) {
            name = if (index < parts.size) parts[index] else null
            possibleManager = currentManager.managers.find { manager -> manager.name == name }
            if (possibleManager != null) {
                currentManager = possibleManager
                if (index < parts.size) index++
                continue
            }

            name = if (index + 1 == parts.size) parts[index]
            else if (index == parts.size) null
            else break
            currentCommand = currentManager.commands.find { command -> command.name == name }
            break
        }

        if (currentCommand == null) message.channel.sendMessage("This command doesn't exist.")
        else {
            val parameters = rest.parseParameters()
            message.channel.sendMessage("This command does exist")
            currentCommand.action(
                CommandData(
                    message, parts, mapOf(),
                    { text ->
                        message.channel.sendMessage(text)
                    }, { text ->
                        message.channel.sendMessage("Something went wrong: $text")
                    }
                )
            )
        }
    }
}*/
