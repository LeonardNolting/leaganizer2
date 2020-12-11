import model.Command
import kotlin.reflect.KParameter

val String.isCommand: Boolean
    get() = startsWith(Config.Commands.prefix)

fun parameters(string: String) = splitCommand(string).last()

val KParameter.meta: Command.Meta?
    get () = this.annotations.find { annotation -> annotation is Command.Meta } as Command.Meta?

val KParameter.metaType: Command.Meta.Type?
    get() = meta?.type

val KParameter.isMeta: Boolean
    get() = this.metaType != null

fun splitCommand(string: String) =
    if (string.isCommand) ("$string ").split(' ', limit = 2)
    else listOf(string)