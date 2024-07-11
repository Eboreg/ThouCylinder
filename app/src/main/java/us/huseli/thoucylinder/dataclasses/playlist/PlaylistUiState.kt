package us.huseli.thoucylinder.dataclasses.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.pluralStringResource
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class PlaylistUiState(
    @ColumnInfo("Playlist_playlistId") val id: String,
    @ColumnInfo("Playlist_name") val name: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val thumbnailUris: List<String>,
) {
    @Composable
    fun getSecondRow(): String {
        val trackCountString = pluralStringResource(R.plurals.x_tracks, trackCount, trackCount)
        return "$trackCountString • ${totalDurationMs.milliseconds.sensibleFormat()}"
    }

    override fun toString() = name
}
