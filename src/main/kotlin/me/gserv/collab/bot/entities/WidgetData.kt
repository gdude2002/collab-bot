package me.gserv.collab.bot.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WidgetData(
    val id: Snowflake,
    val name: String,

    @SerialName("instant_invite")
    val instantInvite: String,

    @SerialName("presence_count")
    val presenceCount: Long,

    val members: List<WidgetMember>
)

@Serializable
data class WidgetMember(
    @SerialName("id")
    val order: Long,

    val username: String,
    val status: String,

    @SerialName("avatar_url")
    val avatarUrl: String
)
