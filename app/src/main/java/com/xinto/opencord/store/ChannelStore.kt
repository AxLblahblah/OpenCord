package com.xinto.opencord.store

import com.xinto.opencord.db.database.CacheDatabase
import com.xinto.opencord.db.entity.channel.toEntity
import com.xinto.opencord.domain.channel.DomainChannel
import com.xinto.opencord.domain.channel.toDomain
import com.xinto.opencord.gateway.DiscordGateway
import com.xinto.opencord.gateway.event.ChannelCreateEvent
import com.xinto.opencord.gateway.event.ChannelDeleteEvent
import com.xinto.opencord.gateway.event.ChannelUpdateEvent
import com.xinto.opencord.gateway.event.GuildCreateEvent
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

typealias ChannelEvent = Event<DomainChannel, DomainChannel, Long>

interface ChannelStore {
    fun observeChannel(channelId: Long): Flow<ChannelEvent>
    fun observeChannels(guildId: Long): Flow<ChannelEvent>
    fun observeChannelsReplace(guildId: Long): Flow<List<DomainChannel>>

    suspend fun fetchChannel(channelId: Long): DomainChannel?
    suspend fun fetchChannels(guildId: Long): List<DomainChannel>
}

class ChannelStoreImpl(
    gateway: DiscordGateway,
    private val api: DiscordApiService,
    private val cache: CacheDatabase,
    private val logger: Logger,
) : ChannelStore {
    private val events = MutableSharedFlow<ChannelEvent>()
    private val replaceEvents = MutableSharedFlow<List<DomainChannel>>()

    override fun observeChannel(channelId: Long): Flow<ChannelEvent> {
        return events.filter { event ->
            event.fold(
                onAdd = { it.id == channelId },
                onUpdate = { it.id == channelId },
                onDelete = { it == channelId },
            )
        }
    }

    override fun observeChannels(guildId: Long): Flow<ChannelEvent> {
        return events.filter { event ->
            event.fold(
                onAdd = { it.guildId == guildId },
                onUpdate = { it.guildId == guildId },
                onDelete = { it == guildId },
            )
        }
    }

    override fun observeChannelsReplace(guildId: Long): Flow<List<DomainChannel>> {
        return replaceEvents.filter {
            it.getOrNull(0)?.guildId == guildId
        }
    }

    override suspend fun fetchChannel(channelId: Long): DomainChannel? {
        return withContext(Dispatchers.IO) {
            cache.channels().getChannel(channelId)?.toDomain()
        }
    }

    override suspend fun fetchChannels(guildId: Long): List<DomainChannel> {
        return withContext(Dispatchers.IO) {
            cache.channels().getChannels(guildId)
                .map { it.toDomain() }
        }
    }

    init {
        gateway.onEvent<ReadyEvent> { _ ->
            val guilds = try {
                api.getGuilds()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("ChannelStore", "Failed to sync guilds", e)
                emptyList()
            }

            val channels = guilds.flatMap { guild ->
                val guildId = guild.id.value
                try {
                    api.getGuildChannels(guildId).map { it.toEntity(guildId) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("ChannelStore", "Failed to sync channels for guild $guildId", e)
                    emptyList()
                }
            }

            val replaceChannels = channels.groupByTo(
                mutableMapOf(),
                keySelector = { it.guildId },
                valueTransform = { it.toDomain() },
            )

            replaceChannels.values.forEach { replaceEvents.emit(it) }

            cache.channels().replaceAllChannels(channels)
        }

        gateway.onEvent<GuildCreateEvent> { event ->
            val guildId = event.data.id.value
            val channels = event.data.channels
                ?.map { it.toEntity(guildId) }
                ?: return@onEvent

            channels.map { it.toDomain() }
                .groupByTo(mutableMapOf(), keySelector = { it.guildId })
                .values.forEach { replaceEvents.emit(it) }

            cache.runInTransaction {
                cache.channels().deleteChannelsByGuild(guildId)
                cache.channels().insertChannels(channels)
            }
        }

        gateway.onEvent<ChannelCreateEvent> {
            val guildId = it.data.guildId?.value
                ?: error("no guild id on channel create event")

            events.emit(ChannelEvent.Add(it.data.toDomain()))
            cache.channels().insertChannel(it.data.toEntity(guildId))
        }

        gateway.onEvent<ChannelUpdateEvent> {
            val guildId = it.data.guildId?.value
                ?: error("no guild id on channel update event")

            events.emit(ChannelEvent.Update(it.data.toDomain()))
            cache.channels().insertChannel(it.data.toEntity(guildId))
        }

        gateway.onEvent<ChannelDeleteEvent> {
            val channelId = it.data.id.value

            events.emit(ChannelEvent.Delete(channelId))

            cache.runInTransaction {
                cache.channels().deleteChannel(channelId)
                cache.messages().deleteByChannel(channelId)
            }
        }
    }
}
