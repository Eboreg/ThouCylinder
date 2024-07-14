package us.huseli.thoucylinder.dataclasses.album

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.enums.AlbumType
import java.util.UUID

@Entity(indices = [Index("Album_isInLibrary")])
@Parcelize
@Immutable
data class Album(
    @PrimaryKey @ColumnInfo("Album_albumId") override val albumId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Album_title") override val title: String,
    @ColumnInfo("Album_isInLibrary") override val isInLibrary: Boolean,
    @ColumnInfo("Album_isLocal") override val isLocal: Boolean,
    @ColumnInfo("Album_year") override val year: Int? = null,
    @ColumnInfo("Album_isHidden") override val isHidden: Boolean = false,
    @ColumnInfo("Album_musicBrainzReleaseId") override val musicBrainzReleaseId: String? = null,
    @ColumnInfo("Album_musicBrainzReleaseGroupId") override val musicBrainzReleaseGroupId: String? = null,
    @ColumnInfo("Album_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("Album_albumType") override val albumType: AlbumType? = null,
    @ColumnInfo("Album_trackCount") override val trackCount: Int? = null,
    @Embedded("Album_youtubePlaylist_") override val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") override val albumArt: MediaStoreImage? = null,
    @Embedded("Album_spotifyImage_") override val spotifyImage: MediaStoreImage? = null,
) : Parcelable, IAlbum {
    override val isSaved: Boolean
        get() = true

    override fun withAlbumArt(albumArt: MediaStoreImage?) = copy(albumArt = albumArt)

    override fun mergeWith(other: IAlbum) = copy(
        albumArt = other.albumArt ?: albumArt,
        albumType = other.albumType ?: albumType,
        musicBrainzReleaseGroupId = other.musicBrainzReleaseGroupId ?: musicBrainzReleaseGroupId,
        musicBrainzReleaseId = other.musicBrainzReleaseId ?: musicBrainzReleaseId,
        spotifyId = other.spotifyId ?: spotifyId,
        spotifyImage = other.spotifyImage ?: spotifyImage,
        youtubePlaylist = other.youtubePlaylist ?: youtubePlaylist,
    )

    override fun asSavedAlbum() = this

    override fun asUnsavedAlbum(): UnsavedAlbum = UnsavedAlbum(
        albumId = albumId,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        title = title,
        albumArt = albumArt,
        albumType = albumType,
        isHidden = isHidden,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        spotifyImage = spotifyImage,
        trackCount = trackCount,
        year = year,
        youtubePlaylist = youtubePlaylist,
    )

    override fun toString(): String = title
}

data class UnsavedAlbum(
    override val albumId: String,
    override val isInLibrary: Boolean,
    override val isLocal: Boolean,
    override val title: String,
    override val albumArt: MediaStoreImage? = null,
    override val albumType: AlbumType? = null,
    override val isHidden: Boolean = false,
    override val musicBrainzReleaseGroupId: String? = null,
    override val musicBrainzReleaseId: String? = null,
    override val spotifyId: String? = null,
    override val spotifyImage: MediaStoreImage? = null,
    override val trackCount: Int? = null,
    override val year: Int? = null,
    override val youtubePlaylist: YoutubePlaylist? = null,
) : IAlbum {
    override val isSaved: Boolean
        get() = false

    override fun withAlbumArt(albumArt: MediaStoreImage?) = copy(albumArt = albumArt)

    override fun mergeWith(other: IAlbum) = copy(
        albumArt = other.albumArt ?: albumArt,
        albumType = other.albumType ?: albumType,
        musicBrainzReleaseGroupId = other.musicBrainzReleaseGroupId ?: musicBrainzReleaseGroupId,
        musicBrainzReleaseId = other.musicBrainzReleaseId ?: musicBrainzReleaseId,
        spotifyId = other.spotifyId ?: spotifyId,
        spotifyImage = other.spotifyImage ?: spotifyImage,
        youtubePlaylist = other.youtubePlaylist ?: youtubePlaylist,
    )

    override fun asSavedAlbum() = Album(
        albumArt = albumArt,
        albumId = albumId,
        albumType = albumType,
        isHidden = isHidden,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        spotifyImage = spotifyImage,
        title = title,
        trackCount = trackCount,
        year = year,
        youtubePlaylist = youtubePlaylist,
    )

    override fun asUnsavedAlbum(): UnsavedAlbum = this
}
