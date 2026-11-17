package com.xinto.opencord.store

import com.xinto.opencord.db.database.CacheDatabase
import com.xinto.opencord.db.entity.guild.toEntity
import com.xinto.opencord.domain.guild.DomainGuild
import com.xinto.opencord.domain.guild.toDomain
import com.xinto.opencord.gateway.DiscordGateway
import com.xinto.opencord.gateway.event.GuildCreateEvent
import com.xinto.opencord.gateway.event.GuildDeleteEvent
import com.xinto.opencord.gateway.event.GuildUpdateEvent
import com.xinto.opencord.gateway.event.ReadyEvent
import com.xinto.opencord.gateway.onEvent
import com.xinto.opencord.rest.service.DiscordApiService
import com.xinto.opencord.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext

typealias GuildEvent = Event<DomainGuild, DomainGuild, Long>

interface GuildStore {
    fun observeGuild(guildId: Long): Flow<GuildEvent>
    fun observeGuilds(): Flow<GuildEvent>

    suspend fun fetchGuild(guildId: Long): DomainGuild?
    suspend fun fetchGuilds(): List<DomainGuild>
}

class GuildStoreImpl(
    gateway: DiscordGateway,
    private val api: DiscordApiService,
    private val cache: CacheDatabase,
    private val logger: Logger,
) : GuildStore {
    private val events = MutableSharedFlow<GuildEvent>()

    override fun observeGuild(guildId: Long): Flow<GuildEvent> {
        return events.filter { event ->
            event.fold(
                onAdd = { it.id == guildId },
                onUpdate = { it.id == guildId },
                onDelete = { it == guildId },
            )
        }
    }

    override fun observeGuilds() = events

    override suspend fun fetchGuild(guildId: Long): DomainGuild? {
        return withContext(Dispatchers.IO) {
            cache.guilds().getGuild(guildId)?.toDomain()
        }
    }

    override suspend fun fetchGuilds(): List<DomainGuild> {
        return withContext(Dispatchers.IO) {
            cache.guilds().getGuilds().map { it.toDomain() }
        }
    }

    init {
        gateway.onEvent<ReadyEvent> { _ ->
            val guilds = try {
                api.getGuilds()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("GuildStore", "Failed to sync guilds", e)
                emptyList()
            }

            val entities = guilds.map { it.toEntity() }

            for (guild in entities) {
                events.emit(GuildEvent.Add(guild.toDomain()))
            }

            cache.guilds().replaceAllGuilds(entities)
        }

        gateway.onEvent<GuildCreateEvent> {
            events.emit(GuildEvent.Add(it.data.toDomain()))
            cache.guilds().insertGuild(it.data.toEntity())
        }

        gateway.onEvent<GuildUpdateEvent> {
            events.emit(GuildEvent.Update(it.data.toDomain()))
            cache.guilds().insertGuild(it.data.toEntity())
        }

        gateway.onEvent<GuildDeleteEvent> {
            val guildId = it.data.id.value

            events.emit(GuildEvent.Delete(guildId))

            cache.runInTransaction {
                cache.guilds().deleteGuild(guildId)
                cache.channels().deleteChannelsByGuild(guildId)
            }
        }
    }
}
