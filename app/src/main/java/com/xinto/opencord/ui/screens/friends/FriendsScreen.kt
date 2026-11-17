package com.xinto.opencord.ui.screens.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xinto.opencord.R
import com.xinto.opencord.domain.relationship.DomainRelationship
import com.xinto.opencord.ui.components.OCImage
import com.xinto.opencord.ui.viewmodel.FriendsViewModel
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Friends")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigation_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (viewModel.state) {
                FriendsViewModel.State.Loading -> {
                    FriendsMessage(
                        text = "Loading friends…",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                FriendsViewModel.State.Loaded -> {
                    if (viewModel.friends.isEmpty() && viewModel.pendingIncoming.isEmpty()) {
                        FriendsMessage(
                            text = "No friends yet. Wumpus is here for you.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (viewModel.pendingIncoming.isNotEmpty()) {
                                item(key = "PENDING_HEADER") {
                                    FriendsSectionHeader(
                                        text = "Pending — ${viewModel.pendingIncoming.size}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                                items(viewModel.pendingIncoming, key = { it.id }) { friend ->
                                    FriendItem(
                                        friend = friend,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            item(key = "FRIENDS_HEADER") {
                                FriendsSectionHeader(
                                    text = "All friends — ${viewModel.friends.size}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            items(viewModel.friends, key = { it.id }) { friend ->
                                FriendItem(
                                    friend = friend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
                FriendsViewModel.State.Error -> {
                    FriendsMessage(
                        text = "Failed to load friends.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendItem(
    friend: DomainRelationship,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        OCImage(
            url = friend.avatarUrl,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = friend.name,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FriendsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun FriendsMessage(
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
