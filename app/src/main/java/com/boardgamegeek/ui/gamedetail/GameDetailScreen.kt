package com.boardgamegeek.ui.gamedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.ui.game.GameViewModel
import com.boardgamegeek.ui.game.GameViewModel.ProducerType
import com.boardgamegeek.ui.navigation.GameRoute
import com.boardgamegeek.ui.navigation.LocalAppNavigator
import com.boardgamegeek.ui.navigation.PersonRoute
import androidx.compose.foundation.shape.CircleShape

@Composable
fun GameDetailScreen(
    viewModel: GameViewModel,
    paddingValues: PaddingValues,
) {
    val navigator = LocalAppNavigator.current
    val type by viewModel.producerType.collectAsStateWithLifecycle()
    val producers by viewModel.producers.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (producers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_generic),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(producers, key = { it.id }) { producer ->
                    GameDetailRow(
                        type = type,
                        item = producer,
                        onClick = {
                            when (type) {
                                ProducerType.EXPANSION,
                                ProducerType.BASE_GAME -> navigator.navigate(GameRoute(producer.id, producer.name))
                                ProducerType.PUBLISHER -> navigator.navigate(PersonRoute(producer.id, producer.name, "PUBLISHER"))
                                ProducerType.ARTIST -> navigator.navigate(PersonRoute(producer.id, producer.name, "ARTIST"))
                                ProducerType.DESIGNER -> navigator.navigate(PersonRoute(producer.id, producer.name, "DESIGNER"))
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameDetailRow(
    type: GameViewModel.ProducerType,
    item: GameDetail,
    onClick: () -> Unit,
) {
    val showAvatar = type != ProducerType.UNKNOWN
    val avatarSize = dimensionResource(R.dimen.thumbnail_list_size_small)
    val placeholderRes = when (type) {
        ProducerType.EXPANSION,
        ProducerType.BASE_GAME,
        ProducerType.PUBLISHER -> R.drawable.thumbnail_image_empty
        ProducerType.ARTIST,
        ProducerType.DESIGNER -> R.drawable.person_image_empty
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar && placeholderRes != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                placeholder = painterResource(placeholderRes),
                error = painterResource(placeholderRes),
                contentDescription = stringResource(R.string.avatar),
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = if (item.description.isBlank()) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
