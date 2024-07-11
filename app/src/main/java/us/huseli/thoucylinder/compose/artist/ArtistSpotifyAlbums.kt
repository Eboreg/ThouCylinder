package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.HorizontalCardList
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumType
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.thoucylinder.dataclasses.spotify.getThumbnailUrl
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ArtistSpotifyAlbumsHeader(
    expand: Boolean,
    onExpandToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        text()
        SmallOutlinedButton(
            onClick = onExpandToggleClick,
            text = stringResource(if (!expand) R.string.show_all else R.string.hide),
        )
    }
}

@Composable
fun ArtistSpotifyAlbumsPreview(
    albums: ImmutableList<SpotifySimplifiedAlbum>,
    onClick: (SpotifySimplifiedAlbum) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalCardList(
        things = { albums },
        key = { it.id },
        thumbnailModel = { it.images.getThumbnailUrl() },
        text = {
            Text(
                text = it.name.umlautify(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                style = FistopyTheme.bodyStyles.primarySmall.copy(lineHeight = 18.sp),
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun ArtistSpotifyAlbumTypeSelection(
    albumTypes: ImmutableList<SpotifyAlbumType>,
    onAlbumTypeClick: (SpotifyAlbumType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(bottom = 5.dp)
    ) {
        for (albumType in SpotifyAlbumType.entries) {
            InputChip(
                selected = albumType in albumTypes,
                onClick = { onAlbumTypeClick(albumType) },
                label = { Text(stringResource(albumType.stringRes)) },
            )
        }
    }
}

@Suppress("FunctionName")
fun LazyGridScope.ArtistSpotifyAlbumGrid(
    albums: ImmutableList<SpotifySimplifiedAlbum>,
    previewAlbums: ImmutableList<SpotifySimplifiedAlbum>,
    expand: Boolean,
    albumTypes: ImmutableList<SpotifyAlbumType>,
    onClick: (SpotifySimplifiedAlbum) -> Unit,
    onAlbumTypeClick: (SpotifyAlbumType) -> Unit,
    onExpandToggleClick: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        ArtistSpotifyAlbumsHeader(
            expand = expand,
            onExpandToggleClick = onExpandToggleClick,
            text = {
                Text(
                    text = stringResource(R.string.available_albums),
                    style = FistopyTheme.typography.titleMedium,
                    maxLines = 2,
                )
            },
        )
    }

    if (!expand) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ArtistSpotifyAlbumsPreview(albums = previewAlbums, onClick = onClick)
        }
    } else {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ArtistSpotifyAlbumTypeSelection(albumTypes = albumTypes, onAlbumTypeClick = onAlbumTypeClick)
        }

        items(albums, key = { it.id }) { spotifyAlbum ->
            val secondRow = listOfNotNull(
                pluralStringResource(
                    R.plurals.x_tracks,
                    spotifyAlbum.trackCount,
                    spotifyAlbum.trackCount
                ),
                spotifyAlbum.year?.toString(),
                spotifyAlbum.spotifyAlbumType?.let { stringResource(it.stringRes) },
            ).joinToString(" • ").nullIfBlank()

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                onClick = { onClick(spotifyAlbum) },
            ) {
                Box(modifier = Modifier.aspectRatio(1f)) {
                    Thumbnail(
                        model = spotifyAlbum.images.getThumbnailUrl(),
                        placeholderIcon = Icons.Sharp.Album,
                        borderWidth = null,
                        shape = RectangleShape,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxHeight().padding(10.dp).height(56.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = spotifyAlbum.title.umlautify(),
                        maxLines = if (secondRow != null) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = FistopyTheme.bodyStyles.primaryBold,
                    )
                    if (secondRow != null) Text(
                        text = secondRow,
                        style = FistopyTheme.bodyStyles.secondarySmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
fun LazyListScope.ArtistSpotifyAlbumList(
    albumTypes: ImmutableList<SpotifyAlbumType>,
    albums: ImmutableList<SpotifySimplifiedAlbum>,
    expand: Boolean,
    header: @Composable () -> Unit,
    onAlbumTypeClick: (SpotifyAlbumType) -> Unit,
    onClick: (SpotifySimplifiedAlbum) -> Unit,
    previewAlbums: ImmutableList<SpotifySimplifiedAlbum>,
) {
    if (!expand) {
        item { Column(modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp), content = { header() }) }
        item { ArtistSpotifyAlbumsPreview(albums = previewAlbums, onClick = onClick) }
    } else {
        stickyHeader {
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(vertical = 5.dp)
            ) {
                header()
                ArtistSpotifyAlbumTypeSelection(
                    albumTypes = albumTypes,
                    onAlbumTypeClick = onAlbumTypeClick,
                )
            }
        }

        items(albums, key = { it.id }) { spotifyAlbum ->
            val secondRow = listOfNotNull(
                pluralStringResource(
                    R.plurals.x_tracks,
                    spotifyAlbum.trackCount,
                    spotifyAlbum.trackCount
                ),
                spotifyAlbum.year?.toString(),
                spotifyAlbum.spotifyAlbumType?.let { stringResource(it.stringRes) },
            ).joinToString(" • ").nullIfBlank()

            ItemListCardWithThumbnail(
                thumbnailModel = spotifyAlbum.images.getThumbnailUrl(),
                thumbnailPlaceholder = Icons.Sharp.Album,
                onClick = { onClick(spotifyAlbum) },
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text(
                            text = spotifyAlbum.title.umlautify(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = FistopyTheme.bodyStyles.primaryBold,
                        )
                        if (secondRow != null) Text(
                            text = secondRow,
                            style = FistopyTheme.bodyStyles.secondarySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
