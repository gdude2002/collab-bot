/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package me.gserv.collab.bot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import me.gserv.collab.bot.extensions.GeneralExtension

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        applicationCommands {
            defaultGuild(GUILD_ID)
        }

        chatCommands {
            defaultPrefix = "?"
            enabled = true
        }

        extensions {
            add(::GeneralExtension)
        }
    }

    bot.start()
}
