@file:Suppress("StringLiteralDuplication", "MagicNumber")

package me.gserv.collab.bot.extensions

import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.channel.thread.TextChannelThreadCreateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.gserv.collab.bot.MANAGER_ROLE
import me.gserv.collab.bot.inCollabGuild
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(KordPreview::class)
class GeneralExtension : Extension() {
    override val name = "general"

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "    "

        encodeDefaults = false
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        event<TextChannelThreadCreateEvent> {
            check { inCollabGuild() }
            check { failIf(event.channel.ownerId == kord.selfId) }
            check { failIf(event.channel.member != null) }  // We only want thread creation, not join

            action {
                val owner = event.channel.owner.asUser()
                val role = event.channel.guild.getRole(MANAGER_ROLE)

                val message = event.channel.createMessage {
                    content = "Oh hey, that's a nice thread you've got there! Let me just get the managers in on " +
                            "this sweet discussion..."
                }

                event.channel.withTyping {
                    delay(3.seconds)
                }

                message.edit {
                    content = "Hey, ${role.mention}s, you've gotta check this thread out!"
                }

                event.channel.withTyping {
                    delay(3.seconds)
                }

                message.edit {
                    content = "Welcome to your new thread, ${owner.mention}! This message is at the " +
                            "start of the thread. Remember, you're welcome to use `/archive` and `/rename` at any " +
                            "time, as well as the pin and unpin right-click commands!"
                }

                message.pin("First message in the thread.")
            }
        }

        ephemeralMessageCommand {
            name = "Raw JSON"

            action {
                val messages = targetMessages.map { it.data }
                val data = json.encodeToString(messages)

                respond {
                    content = "Raw message data attached below."

                    addFile("message.json", data.byteInputStream())
                }
            }
        }

        ephemeralSlashCommand(::RenameArguments) {
            name = "rename"
            description = "Rename the current thread, if you have permission"

            check { isInThread() }

            action {
                val channel = channel.asChannel() as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (roles.contains(MANAGER_ROLE)) {
                    channel.edit {
                        name = arguments.name

                        reason = "Renamed by ${member.tag}"
                    }

                    respond { content = "Thread renamed." }

                    return@action
                }

                if (channel.ownerId != user.id) {
                    respond { content = "This is not your thread." }

                    return@action
                }

                channel.edit {
                    name = arguments.name

                    reason = "Renamed by ${member.tag}"
                }

                respond { content = "Thread renamed." }
            }
        }

        ephemeralSlashCommand(::ArchiveArguments) {
            name = "archive"
            description = "Archive the current thread, if you have permission"

            check { isInThread() }

            action {
                val channel = channel.asChannel() as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (roles.contains(MANAGER_ROLE)) {
                    channel.edit {
                        this.archived = true
                        this.locked = arguments.lock

                        reason = "Archived by ${user.asUser().tag}"
                    }

                    respond {
                        content = "Thread archived"

                        if (arguments.lock) {
                            content += " and locked"
                        }

                        content += "."
                    }

                    return@action
                }

                if (channel.ownerId != user.id) {
                    respond { content = "This is not your thread." }

                    return@action
                }

                if (channel.isArchived) {
                    respond { content = "This channel is already archived." }

                    return@action
                }

                if (arguments.lock) {
                    respond { content = "Only members of the community team may lock threads." }

                    return@action
                }

                channel.edit {
                    archived = true

                    reason = "Archived by ${user.asUser().tag}"
                }

                respond { content = "Thread archived." }
            }
        }

        ephemeralMessageCommand {
            name = "Pin in thread"

            check { isInThread() }

            action {
                val channel = channel.asChannel() as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (roles.contains(MANAGER_ROLE)) {
                    targetMessages.forEach { it.pin("Pinned by ${member.tag}") }
                    edit { content = "Messages pinned." }

                    return@action
                }

                if (channel.ownerId != user.id) {
                    respond { content = "This is not your thread." }

                    return@action
                }

                targetMessages.forEach { it.pin("Pinned by ${member.tag}") }

                edit { content = "Messages pinned." }
            }
        }

        ephemeralMessageCommand {
            name = "Unpin in thread"

            check { isInThread() }

            action {
                val channel = channel.asChannel() as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (roles.contains(MANAGER_ROLE)) {
                    targetMessages.forEach { it.unpin("Unpinned by ${member.tag}") }
                    edit { content = "Messages unpinned." }

                    return@action
                }

                if (channel.ownerId != user.id) {
                    respond { content = "This is not your thread." }

                    return@action
                }

                targetMessages.forEach { it.unpin("Unpinned by ${member.tag}") }

                edit { content = "Messages unpinned." }
            }
        }
    }

    inner class RenameArguments : Arguments() {
        val name by string("name", "Name to give the current thread")
    }

    inner class ArchiveArguments : Arguments() {
        val lock by defaultingBoolean(
            "lock",
            "Whether to lock the thread, if you're staff - defaults to false",
            false
        )
    }

    inner class LockArguments : Arguments() {
        val channel by optionalChannel("channel", "Channel to lock, if not the current one")
    }
}
