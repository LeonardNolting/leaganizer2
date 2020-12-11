package model

import com.jessecorbett.diskord.api.model.User
import com.jessecorbett.diskord.dsl.Bot
import guild

enum class Permission(val roles: List<Role>) {
    EVERYBODY(listOf()),
    STAFF(listOf(Role.STAFF));
    suspend fun discord(bot: Bot) = roles.map {role -> role.discord(bot)}

    enum class Role(val id: String) {
        STAFF("773881546431528981");

        suspend fun discord(bot: Bot) = guild.getRoles().find { role -> role.id == id }
    }
}

/*
fun Member.permitted (user: User) {
    guild.get
}*/
