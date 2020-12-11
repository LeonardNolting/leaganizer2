package model

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.dsl.Bot
import java.util.*

class Tournament {
    @Command(
        "tournament", "register",
        mode = Command.Mode.Continuous.Possible::class,
        permission = Permission.STAFF
    )
    class Register(
        @Command.Meta(Command.Meta.Type.AUTHOR)
        val user: User,
        val test: String,
        val test2: String = "foo"
    ) : Task() {
        override val description = "Registers a new tournament."
        override fun execute(): String {
            return "Executed command Tournament.Register; Parameters: author=${user.username}, test=$test, test2=$test2"
        }

        /*class Mode : Command.Mode() {
            override suspend fun Task.Builder.process(bot: Bot, message: Message) {
                println("Message arrived")
            }
        }*/
    }
}