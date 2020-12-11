package model

import com.jessecorbett.diskord.dsl.Command

/*
data class CommandManager(val name: String? = null) {
	val commands = mutableListOf<Command>()
	val managers = mutableListOf<CommandManager>()
	fun commands(part: String, block: CommandManager.() -> Unit) {
		managers += CommandManager(part).apply(block)
	}

	fun command(part: String? = null, action: suspend CommandData.() -> Unit) {
		managers += CommandManager(part).apply {
			commands += Command(null, action)
			*/
/*commands += Command(null) {
				reply("This is command -$name")
			}*//*

			commands += Command("help") {
				reply("Help for command: -$name")
			}
		}
	}
}*/
