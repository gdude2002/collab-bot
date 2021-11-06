@file:Suppress("UnderscoresInNumericLiterals", "MagicNumber")

package me.gserv.collab.bot

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

internal val TOKEN = env("TOKEN")

val GUILD_ID = Snowflake(
    env("GUILD_ID").toLong()
)

val MANAGER_ROLE = Snowflake(
    envOrNull("MANAGER_ROLE")?.toLong() ?: 905218921605513276L
)
