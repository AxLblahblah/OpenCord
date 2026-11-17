package com.xinto.opencord.ui.screens.home.panels.member

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xinto.opencord.domain.member.DomainGuildMember
import com.xinto.opencord.ui.components.OCImage
import com.xinto.opencord.ui.viewmodel.MembersViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun MembersList(
    guildId: Long,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = getViewModel(),
) {
    LaunchedEffect(guildId) {
        viewModel.load(guildId)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = modifier,
    ) {
        when (viewModel.state) {
            MembersViewModel.State.Loading -> {
                MembersListMessage(modifier = Modifier.fillMaxSize(), text = "Loading members…")
            }
            MembersViewModel.State.Loaded -> {
                if (viewModel.members.isEmpty()) {
                    MembersListMessage(modifier = Modifier.fillMaxSize(), text = "No members")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(viewModel.members, key = { it.user?.id ?: 0L }) { member ->
                            MemberItem(
                                member = member,
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            MembersViewModel.State.Error -> {
                MembersListMessage(modifier = Modifier.fillMaxSize(), text = "Failed to load members.")
            }
        }
    }
}

@Composable
private fun MemberItem(
    member: DomainGuildMember,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        OCImage(
            url = member.user?.avatarUrl ?: member.avatarUrl,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = member.nick ?: member.user?.username ?: "",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MembersListMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
