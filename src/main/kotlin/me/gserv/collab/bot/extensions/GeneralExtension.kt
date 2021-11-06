package me.gserv.collab.bot.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview

@OptIn(KordPreview::class)
class GeneralExtension : Extension() {
    override val name = "general"

    override suspend fun setup() {
        TODO("Actually add things")
    }
}
