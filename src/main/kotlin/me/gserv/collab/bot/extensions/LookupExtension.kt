@file:Suppress("StringLiteralDuplication", "MagicNumber")

package me.gserv.collab.bot.extensions

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.translate
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import me.gserv.collab.bot.entities.WidgetData
import me.gserv.collab.bot.getName
import me.gserv.collab.bot.replaceParams
import kotlin.time.ExperimentalTime

const val WIDGET_URL = "https://discord.com/api/guilds/:id/widget.json"

@OptIn(KordPreview::class)
class LookupExtension : Extension() {
    override val name = "lookup"

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        publicSlashCommand(::SnowflakeArguments) {
            name = "user"
            description = "Look up information about the given user ID"

            action {
                val user = event.kord.getUser(arguments.id)?.fetchUser()

                if (user == null) {
                    respond {
                        content = "Unknown user ID: `${arguments.id.value}`"
                    }

                    return@action
                }

                val flags = user.publicFlags?.flags ?: listOf()

                respond {
                    embed {
                        color = DISCORD_BLURPLE

                        description = "**__Basic Information__**\n" +
                                "**Tag:** `${user.tag}`\n" +
                                "**ID:** `${user.id.value}`\n" +
                                "**Mention:** ${user.mention}\n\n" +

                                if (user.avatar != null) {
                                    "**Avatar URL:** ${user.avatar?.url}\n\n"
                                } else {
                                    ""
                                } +

                                "**Created:** ${user.id.timestamp.toDiscord(TimestampType.LongDateTime)} " +
                                "(${user.id.timestamp.toDiscord(TimestampType.RelativeTime)})\n\n" +
                                "__**Flags**__ " +

                                if (flags.isNotEmpty()) {
                                    "(${flags.size})\n" +
                                            flags.joinToString(", ") { it.getName() }
                                } else {
                                    "\nNo flags."
                                }

                        author {
                            name = user.tag
                            icon = user.avatar?.url
                        }

                        timestamp = Clock.System.now()
                    }
                }
            }
        }

        publicSlashCommand(::SnowflakeArguments) {
            name = "widget"
            description = "Look up the widget for the given guild ID, if enabled"

            action {
                val widget = getWidget(arguments.id)

                if (widget == null) {
                    respond {
                        content = "Invalid guild ID, or widget disabled: `${arguments.id}`"
                    }

                    return@action
                }

                editingPaginator {
                    timeoutSeconds = 120

                    page {
                        title = "Guild: ${widget.name}"
                        description = "**ID:** `${widget.id}`\n" +
                                "**Apprx. Online:** ${widget.presenceCount}\n\n"

                        if (widget.instantInvite != null) {
                            description += "**Invite:** `${widget.instantInvite.split("/").last()}`"
                        }
                    }

                    if (widget.channels.isNotEmpty()) {
                        page {
                            title = "Channels: ${widget.name}"

                            val builder = StringBuilder("")

                            builder.append("Position | ID | Name\n\n")

                            widget.channels.forEach { channel ->
                                builder.append("${channel.position.toString().padStart(3, '0')} ")
                                builder.append("`${channel.id}` | ${channel.name}")
                                builder.append("\n")
                            }

                            description = builder.toString()
                        }
                    }

                    widget.members.sortedBy { it.order }.forEach { member ->
                        page {
                            title = "Members: ${widget.name}"

                            description = "**Username:** `${member.username}`\n" +
                                    "**Status:** `${member.status}`"

                            image = member.avatarUrl

                            footer {
                                text = "Member ${member.order}"
                            }
                        }
                    }
                }.send()
            }
        }

        publicSlashCommand(::InviteArguments) {
            name = "invite"
            description = "Look up information about the given invite"

            action {
                var code = arguments.code

                if (code.contains("/")) {
                    code = code.split("/").last()
                }

                val invite = event.kord.getInvite(code, true)

                if (invite == null) {
                    respond {
                        content = "Invalid invite code: `$code`"
                    }

                    return@action
                }

                val user = invite.getInviter()?.fetchUser()
                val channel = invite.channel
                val guild = invite.partialGuild

                val builder = StringBuilder("")

                if (guild != null) {
                    builder.append("**Type:** Server Invite\n\n")

                    builder.append("**Server Name:** ${guild.name}\n")
                    builder.append("**Server ID:** `${guild.id.value}`\n\n")

                    builder.append("**Apprx. Members:** `${invite.approximateMemberCount ?: "N/A"}`\n")
                    builder.append("**Apprx. Online:** `${invite.approximatePresenceCount ?: "N/A"}`\n\n")

                    val features = guild.data.features

                    if (features.isNotEmpty()) {
                        builder.append("**Features (${features.size}):** ")

                        builder.append(
                            features.sortedBy { it.value }.joinToString { "`${it.value}`" }
                        )

                        builder.append("\n\n")
                    }

                    when (guild.owner) {
                        true -> builder.append("Invite was created by the server owner.\n\n")
                        false -> builder.append("Invite was **not** created by the server owner.\n\n")

                        else -> {}  // Nothing to do here
                    }

                    if (guild.welcomeScreen != null) {
                        val screen = guild.welcomeScreen!!

                        builder.append("**__Welcome Screen__**\n")

                        if (screen.description != null) {
                            builder.append(
                                screen.description!!.lines().joinToString("\n") { "> $it" }
                            )

                            builder.append("\n\n")
                        }

                        val channels = screen.welcomeScreenChannels

                        if (channels.isEmpty()) {
                            builder.append("**No welcome screen channels configured.**\n\n")
                        } else {
                            channels.forEach {
                                builder.append("`${it.id.value}` -> ${it.mention}\n")
                                builder.append("> ${it.description}\n\n")
                            }
                        }
                    }
                } else {
                    builder.append("**Type:** DM Group Invite\n\n")
                }

                builder.append("**__Channel Information__**\n")

                builder.append("**ID:** `${channel.id.value}`\n")
                builder.append("**Mention:** ${channel.mention}\n\n")

                if (user != null) {
                    val flags = user.publicFlags?.flags ?: listOf()

                    builder.append(
                        "**__Inviter Information__**\n" +
                                "**Tag:** `${user.tag}`\n" +
                                "**ID:** `${user.id.value}`\n" +
                                "**Mention:** ${user.mention}\n\n" +

                                if (user.avatar != null) {
                                    "**Avatar URL:** ${user.avatar?.url}\n\n"
                                } else {
                                    ""
                                } +

                                "**Created:** ${user.id.timestamp.toDiscord(TimestampType.LongDateTime)} " +
                                "(${user.id.timestamp.toDiscord(TimestampType.RelativeTime)})\n\n" +
                                "__**Inviter Flags**__ " +

                                if (flags.isNotEmpty()) {
                                    "(${flags.size})\n" +
                                            flags.joinToString(", ") { it.getName() }
                                } else {
                                    "\nNo flags."
                                }
                    )

                    val perms = guild?.permissions

                    if (perms != null) {
                        builder.append("\n\n")

                        val permList = perms.values.map { it.translate(getLocale()) }

                        if (permList.isNotEmpty()) {
                            builder.append("**Server permissions: ${permList.joinToString { it }}**")
                        }
                    }
                }

                respond {
                    embed {
                        color = DISCORD_BLURPLE
                        description = builder.toString()
                        title = "Invite: $code"

                        if (guild?.iconHash != null) {
                            thumbnail {
                                url = guild.getIconUrl(Image.Format.PNG)!!
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getWidget(id: Snowflake): WidgetData? =
        try {
            client.get<WidgetData>(WIDGET_URL.replaceParams("id" to id))
        } catch (e: ResponseException) {
            if (e.response.status.value == 403) {
                null
            } else {
                throw e
            }
        }

    inner class SnowflakeArguments : Arguments() {
        val id by snowflake("id", "ID to look up")
    }

    inner class InviteArguments : Arguments() {
        val code by string("code", "Invite code or URL")
    }
}
