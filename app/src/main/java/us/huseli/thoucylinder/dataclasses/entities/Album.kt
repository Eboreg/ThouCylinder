package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import java.util.UUID

@Entity(indices = [Index("Album_isInLibrary")])
@Parcelize
data class Album(
    @PrimaryKey @ColumnInfo("Album_albumId") val albumId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Album_title") val title: String,
    @ColumnInfo("Album_isInLibrary") val isInLibrary: Boolean,
    @ColumnInfo("Album_isLocal") val isLocal: Boolean,
    @ColumnInfo("Album_year") val year: Int? = null,
    @ColumnInfo("Album_isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo("Album_isHidden") val isHidden: Boolean = false,
    @ColumnInfo("Album_musicBrainzReleaseId") val musicBrainzReleaseId: String? = null,
    @ColumnInfo("Album_musicBrainzReleaseGroupId") val musicBrainzReleaseGroupId: String? = null,
    @ColumnInfo("Album_spotifyId") val spotifyId: String? = null,
    @Embedded("Album_youtubePlaylist_") val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") val albumArt: MediaStoreImage? = null,
    @Embedded("Album_spotifyImage_") val spotifyImage: MediaStoreImage? = null,
) : Parcelable {
    @Immutable
    data class ViewState(
        val album: Album,
        val trackCount: Int,
        val yearString: String?,
        val artists: ImmutableList<AlbumArtistCredit>,
        val isPartiallyDownloaded: Boolean,
    )

    val isOnYoutube: Boolean
        get() = youtubePlaylist != null

    val isPlayable: Boolean
        get() = isLocal || youtubePlaylist != null

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/album/${it}" }

    val youtubeWebUrl: String?
        get() = youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" }

    override fun toString(): String = title
}
