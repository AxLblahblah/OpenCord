package com.xinto.opencord.domain.relationship

import androidx.compose.runtime.Immutable
import com.xinto.opencord.rest.models.ApiRelationship
import com.xinto.opencord.rest.service.DiscordCdnServiceImpl

@Immutable
data class DomainRelationship(
    val id: Long,
    val name: String,
    val avatarUrl: String,
    val type: Int,
)

fun ApiRelationship.toDomain(): DomainRelationship? {
    val user = user ?: return null

    return DomainRelationship(
        id = id.value,
        name = nickname
            ?: user.globalName
            ?: user.username,
        avatarUrl = user.avatar
            ?.let { DiscordCdnServiceImpl.getUserAvatarUrl(user.id.value, it) }
            ?: DiscordCdnServiceImpl.getDefaultAvatarUrl(user.discriminator.toInt().rem(5)),
        type = type,
    )
}
