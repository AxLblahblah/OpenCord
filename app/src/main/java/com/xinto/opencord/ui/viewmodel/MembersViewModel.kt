package com.xinto.opencord.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinto.opencord.domain.member.DomainGuildMember
import com.xinto.opencord.domain.member.toDomain
import com.xinto.opencord.rest.service.DiscordApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class MembersViewModel(
    private val api: DiscordApiService,
) : ViewModel() {
    sealed interface State {
        object Loading : State
        object Loaded : State
        object Error : State
    }

    var state by mutableStateOf<State>(State.Loading)
        private set

    val members = mutableStateListOf<DomainGuildMember>()

    fun load(guildId: Long) {
        viewModelScope.launch {
            if (guildId <= 0L) {
                members.clear()
                state = State.Loaded
                return@launch
            }

            state = State.Loading
            withContext(Dispatchers.IO) {
                try {
                    val fetched = api.getGuildMembers(guildId)
                        .mapNotNull { it.toDomain() }
                        .sortedBy { (it.nick ?: it.user?.username ?: "").lowercase() }

                    withContext(Dispatchers.Main) {
                        members.clear()
                        members.addAll(fetched)
                        state = State.Loaded
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        state = State.Error
                    }
                }
            }
        }
    }
}
