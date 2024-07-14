package us.huseli.thoucylinder.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo
import us.huseli.thoucylinder.interfaces.IExternalTrack
import kotlin.time.Duration

data class MusicBrainzRecordingSearch(
    val count: Int,
    val offset: Int,
    val recordings: List<Recording>,
) {
    data class Recording(
        override val id: String,
        override val title: String,
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val length: Int,
        @SerializedName("first-release-date")
        val firstReleaseDate: String?,
        val releases: List<RecordingRelease>,
        val tags: List<Tag>,
    ) : AbstractMusicBrainzItem(), IExternalTrack {
        data class Tag(val count: Int, val name: String)

        data class RecordingRelease(
            @SerializedName("artist-credit")
            override val artistCredit: List<MusicBrainzArtistCredit>,
            override val country: String?,
            override val date: String?,
            override val id: String,
            val media: List<Media>,
            @SerializedName("release-group")
            val releaseGroup: ReleaseGroup,
            override val status: MusicBrainzReleaseStatus?,
            override val title: String,
            @SerializedName("track-count")
            override val trackCount: Int,
        ) : AbstractMusicBrainzRelease() {
            data class Media(
                override val format: String?,
                @SerializedName("track-count")
                override val trackCount: Int,
                @SerializedName("track-offset")
                val trackOffset: Int,
                val position: Int,
                val track: List<RecordingReleaseTrack>,
            ) : AbstractMusicBrainzMedia() {
                data class RecordingReleaseTrack(
                    override val length: Int,
                    override val number: String,
                    override val id: String,
                    val title: String,
                ) : AbstractMusicBrainzTrack()
            }

            data class ReleaseGroup(
                override val id: String,
                @SerializedName("primary-type")
                val primaryType: MusicBrainzReleaseGroupPrimaryType?,
                @SerializedName("primary-type-id")
                val primaryTypeId: String?,
                @SerializedName("release-group")
                val releaseGroup: ReleaseGroup,
                val title: String,
                @SerializedName("type-id")
                val typeId: String?,
            ) : AbstractMusicBrainzItem() {
                data class ReleaseGroup(
                    override val id: String,
                    @SerializedName("primary-type")
                    val primaryType: MusicBrainzReleaseGroupPrimaryType?,
                    @SerializedName("primary-type-id")
                    val primaryTypeId: String?,
                    val title: String,
                    @SerializedName("type-id")
                    val typeId: String?,
                ) : AbstractMusicBrainzItem()
            }

            override val duration: Duration?
                get() = null

            override val releaseGroupId: String
                get() = releaseGroup.id
        }

        val year: Int?
            get() = firstReleaseDate
                ?.substringBefore('-')
                ?.takeIf { it.matches(Regex("^\\d{4}$")) }
                ?.toInt()

        override fun toTrackCombo(isInLibrary: Boolean, album: IAlbum?): UnsavedTrackCombo {
            val track = Track(
                musicBrainzId = id,
                title = title,
                isInLibrary = isInLibrary,
                durationMs = length.toLong(),
                year = year,
                albumId = album?.albumId,
            )

            return UnsavedTrackCombo(
                track = track,
                trackArtists = artistCredit.toNativeTrackArtists(trackId = track.trackId),
                album = album,
            )
        }
    }
}
