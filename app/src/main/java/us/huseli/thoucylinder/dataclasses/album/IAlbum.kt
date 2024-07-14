package us.huseli.thoucylinder.dataclasses.album

import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.interfaces.IAlbumArtOwner
import us.huseli.thoucylinder.interfaces.IHasMusicBrainzIds

interface IAlbum : IAlbumArtOwner, IHasMusicBrainzIds {
    val albumId: String
    val isHidden: Boolean
    val isInLibrary: Boolean
    val isLocal: Boolean
    val isSaved: Boolean
    val title: String
    val year: Int?
    val albumArt: MediaStoreImage?
    val albumType: AlbumType?
    override val musicBrainzReleaseGroupId: String?
    override val musicBrainzReleaseId: String?
    val spotifyId: String?
    val spotifyImage: MediaStoreImage?
    val trackCount: Int?
    val youtubePlaylist: YoutubePlaylist?

    override val fullImageUrl: String?
        get() = albumArt?.fullUriString ?: spotifyImage?.fullUriString ?: youtubePlaylist?.fullImage?.url

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/album/${it}" }

    override val thumbnailUrl: String?
        get() = albumArt?.thumbnailUriString ?: spotifyImage?.thumbnailUriString ?: youtubePlaylist?.thumbnailUrl

    val youtubeWebUrl: String?
        get() = youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" }

    fun toImportableUiState(playCount: Int? = null) = ImportableAlbumUiState(
        albumId = albumId,
        albumType = albumType,
        fullImageUrl = fullImageUrl,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        isOnYoutube = youtubePlaylist != null,
        isSaved = isSaved,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        playCount = playCount,
        spotifyWebUrl = spotifyWebUrl,
        thumbnailUrl = thumbnailUrl,
        title = title,
        trackCount = trackCount,
        yearString = year?.toString(),
        youtubeWebUrl = youtubeWebUrl,
    )

    fun toUiState(isSelected: Boolean = false) = AlbumUiState(
        albumId = albumId,
        albumType = albumType,
        fullImageUrl = fullImageUrl,
        isInLibrary = isInLibrary && !isHidden,
        isLocal = isLocal,
        isOnYoutube = youtubePlaylist != null,
        isSaved = isSaved,
        isSelected = isSelected,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        spotifyWebUrl = spotifyWebUrl,
        thumbnailUrl = thumbnailUrl,
        title = title,
        trackCount = trackCount,
        yearString = year?.toString(),
        youtubePlaylistId = youtubePlaylist?.id,
        youtubeWebUrl = youtubeWebUrl,
    )

    fun asSavedAlbum(): Album
    fun asUnsavedAlbum(): UnsavedAlbum
    fun mergeWith(other: IAlbum): IAlbum
    fun withAlbumArt(albumArt: MediaStoreImage?): IAlbum
}
