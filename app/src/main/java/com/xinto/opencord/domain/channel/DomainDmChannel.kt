package com.xinto.opencord.domain.channel

import androidx.compose.runtime.Immutable

@Immutable
data class DomainDmChannel(
    override val id: Long,
    override val guildId: Long?,
    override val name: String,
    override val position: Int,
    override val parentId: Long?,
) : DomainChannel() {
    override val sortingPriority: Short get() = 2
}
