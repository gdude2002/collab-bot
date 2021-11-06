package me.gserv.collab.bot

import com.kotlindiscord.kord.extensions.checks.*
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import mu.KotlinLogging

suspend fun CheckContext<*>.inCollabGuild() {
    val logger = KotlinLogging.logger("me.gserv.collab.bot.inCollabGuild")
    val guild = guildFor(event)

    if (guild == null) {
        logger.nullGuild(event)

        fail("Must be in the Quilt collab server!")
    } else {
        if (guild.id != GUILD_ID) {
            fail("Must be in the Quilt collab server!")
        }
    }
}

suspend fun CheckContext<*>.isManager() {
    inCollabGuild()

    if (!this.passed) {
        return
    }

    val logger = KotlinLogging.logger("me.gserv.collab.bot.isManager")
    val member = memberFor(event)?.asMemberOrNull()

    if (member == null) {  // Shouldn't happen, but you never know
        logger.nullMember(event)

        fail()
    } else {
        if (!member.roleIds.contains(MANAGER_ROLE)) {
            logger.failed("Member does not have the Manager role")

            fail("Must be a Quilt collab manager!")
        }
    }
}

suspend fun CheckContext<*>.isNotManager() {
    inCollabGuild()

    if (!this.passed) {
        return
    }

    val logger = KotlinLogging.logger("me.gserv.collab.bot.isNotManager")
    val member = memberFor(event)?.asMemberOrNull()

    if (member == null) {  // Shouldn't happen, but you never know
        logger.nullMember(event)

        fail()
    } else {
        if (member.roleIds.contains(MANAGER_ROLE)) {
            logger.failed("Member has the Manager role")

            fail("Must **not** be a Quilt collab manager!")
        }
    }
}
