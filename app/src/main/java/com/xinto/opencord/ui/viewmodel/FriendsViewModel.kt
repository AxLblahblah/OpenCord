package com.xinto.opencord.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinto.opencord.domain.relationship.DomainRelationship
import com.xinto.opencord.domain.relationship.toDomain
import com.xinto.opencord.rest.service.DiscordApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class FriendsViewModel(
    private val api: DiscordApiService,
) : ViewModel() {
    sealed interface State {
        object Loading : State
        object Loaded : State
        object Error : State
    }

    var state by mutableStateOf<State>(State.Loading)
        private set

    val friends = mutableStateListOf<DomainRelationship>()
    val pendingIncoming = mutableStateListOf<DomainRelationship>()

    fun load() {
        viewModelScope.launch {
            state = State.Loading
            withContext(Dispatchers.IO) {
                try {
                    val relationships = api.getRelationships()
                        .mapNotNull { it.toDomain() }

                    withContext(Dispatchers.Main) {
                        friends.clear()
                        friends.addAll(
                            relationships
                                .filter { it.type == 1 }
                                .sortedBy { it.name.lowercase() },
                        )
                        pendingIncoming.clear()
                        pendingIncoming.addAll(
                            relationships
                                .filter { it.type == 3 }
                                .sortedBy { it.name.lowercase() },
                        )
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

    init {
        load()
    }
}
