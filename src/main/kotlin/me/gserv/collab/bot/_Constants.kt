package me.gserv.collab.bot

import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake

internal val TOKEN = env("TOKEN")

val GUILD_ID = Snowflake(
    env("GUILD_ID").toLong()  // Get the test server ID from the env vars or a .env file
)
