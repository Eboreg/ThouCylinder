package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.umlautify
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TrackListCard(
    state: TrackUiState,
    downloadStateFlow: StateFlow<TrackDownloadTask.UiState?>,
    showAlbum: Boolean,
    showArtist: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    extraIcons: @Composable () -> Unit = {},
    extraBottomSheetItems: @Composable () -> Unit = {},
    containerColor: Color? = null,
) {
    val downloadState by downloadStateFlow.collectAsStateWithLifecycle()

    ItemListCardWithThumbnail(
        thumbnailModel = state,
        thumbnailPlaceholder = Icons.Sharp.MusicNote,
        isSelected = { state.isSelected },
        onClick = onClick,
        onLongClick = onLongClick,
        height = 50.dp,
        containerColor = containerColor,
    ) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val secondRow = state.getSecondaryInfo(
                        showAlbum = showAlbum,
                        showArtist = showArtist,
                        showYear = true,
                    )

                    Text(
                        text = state.title.umlautify(),
                        maxLines = if (secondRow == null) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        style = FistopyTheme.bodyStyles.primaryBold,
                    )
                    secondRow?.also {
                        Text(
                            text = it.umlautify(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = FistopyTheme.bodyStyles.secondarySmall,
                        )
                    }
                }

                state.durationMs?.also { durationMs ->
                    Text(
                        text = durationMs.milliseconds.sensibleFormat(),
                        modifier = Modifier.padding(start = 5.dp),
                        style = FistopyTheme.bodyStyles.primarySmall,
                    )
                }

                TrackBottomSheetWithButton(state = state, extraItems = extraBottomSheetItems)
                extraIcons()
            }

            Row(modifier = Modifier.height(2.dp).fillMaxWidth()) {
                downloadState?.also { DownloadStateProgressIndicator(it) }
            }
        }
    }
}
