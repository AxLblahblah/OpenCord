package com.xinto.opencord.rest.models

import com.xinto.opencord.rest.models.user.ApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiRelationship(
    @SerialName("id")
    val id: ApiSnowflake,

    @SerialName("type")
    val type: Int,

    @SerialName("nickname")
    val nickname: String? = null,

    @SerialName("user")
    val user: ApiUser? = null,
)
