package com.xinto.opencord.rest.models

import com.xinto.opencord.rest.models.user.ApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiChannel(
    @SerialName("id")
    val id: ApiSnowflake,

    @SerialName("guild_id")
    val guildId: ApiSnowflake? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("recipients")
    val recipients: List<ApiUser>? = null,

    @SerialName("type")
    val type: Int,

    @SerialName("position")
    val position: Int = 0,

    @SerialName("parent_id")
    val parentId: ApiSnowflake? = null,

    @SerialName("nsfw")
    val nsfw: Boolean = false,

    @SerialName("last_message_id")
    val lastMessageId: ApiSnowflake? = null,
)
