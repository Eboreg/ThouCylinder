package us.huseli.thoucylinder.dataclasses.musicBrainz

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import kotlin.math.abs

@Suppress("unused")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official") OFFICIAL,
    @SerializedName("Promotion") PROMOTION,
    @SerializedName("Bootleg") BOOTLEG,
    @SerializedName("Pseudo-release") PSEUDO_RELEASE,
    @SerializedName("Withdrawn") WITHDRAWN,
    @SerializedName("Cancelled") CANCELLED,
}

data class MusicBrainzRelease(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    val country: String?,
    val date: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val media: List<Media>,
    val packaging: String?,
    @SerializedName("packaging-id")
    val packagingId: String?,
    val quality: String?,
    @SerializedName("release-group")
    val releaseGroup: ReleaseGroup,
    val status: MusicBrainzReleaseStatus?,
    @SerializedName("status-id")
    val statusId: String?,
    val title: String,
) : AbstractMusicBrainzItem() {
    data class ReleaseGroup(
        @SerializedName("artist-credit")
        val artistCredit: List<MusicBrainzArtistCredit>,
        val disambiguation: String?,
        @SerializedName("first-release-date")
        val firstReleaseDate: String?,
        val genres: List<MusicBrainzGenre>,
        override val id: String,
        @SerializedName("primary-type")
        val primaryType: MusicBrainzReleaseGroupPrimaryType?,
        @SerializedName("primary-type-id")
        val primaryTypeId: String?,
        val title: String,
    ) : AbstractMusicBrainzItem() {
        val year: Int?
            get() = firstReleaseDate
                ?.substringBefore('-')
                ?.takeIf { it.matches(Regex("^\\d{4}$")) }
                ?.toInt()
    }

    data class Media(
        val format: String?,
        @SerializedName("format-id")
        val formatId: String?,
        val position: Int,
        @SerializedName("track-count")
        val trackCount: Int,
        @SerializedName("track-offset")
        val trackOffset: Int,
        val tracks: List<MusicBrainzTrack>,
    ) {
        data class AlbumMatch(val medium: Media, val score: Int)

        fun getAlbumMatchScore(combo: AlbumWithTracksCombo): Int = combo.tracks
            .zip(tracks)
            .filter { (track, mbTrack) -> !track.title.contains(mbTrack.title, true) }
            .size + abs(tracks.size - combo.tracks.size)
    }

    data class AlbumMatch(
        val score: Double,
        val albumCombo: AlbumWithTracksCombo,
    )

    private val allGenres: List<MusicBrainzGenre>
        get() = genres
            .asSequence()
            .plus(releaseGroup.genres)
            .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
            .groupBy { it }
            .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
            .sortedByDescending { it.count }

    val year: Int?
        get() = releaseGroup.year ?: date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumMatch {
        val medium = getBestMediumMatch(combo)?.medium

        return AlbumMatch(
            score = getAlbumDistance(combo),
            albumCombo = combo.copy(
                album = combo.album.copy(
                    title = title,
                    year = year,
                    artist = artistCredit.artistString(),
                    musicBrainzReleaseId = id,
                    musicBrainzReleaseGroupId = releaseGroup.id,
                ),
                tracks = combo.tracks.mapIndexed { index, track ->
                    val mbTrack = medium?.tracks?.getOrNull(index)
                    track.copy(
                        musicBrainzId = mbTrack?.id,
                        artist = mbTrack?.artist ?: track.artist,
                        year = mbTrack?.year ?: track.year,
                        title = mbTrack?.title ?: track.title,
                    )
                },
                tags = combo.tags.toSet().plus(allGenres.toInternal()).toList(),
            ),
        )
    }

    private fun getAlbumDistance(combo: AlbumWithTracksCombo): Double {
        /**
         * Test if relevant strings from this release and its tracks are contained in the corresponding strings of
         * combo. Perfect match returns 0. If the numbers of tracks differ, the difference is factored into the result
         * (by Media.getAlbumMatchScore()).
         */
        var result = 0.0

        // +1 if _none_ of the credited artists match:
        if (!artistCredit.any { (combo.album.artist ?: combo.album.title).contains(it.name, true) }) result++
        if (!title.contains(combo.album.title, true)) result++
        if (combo.tracks.isNotEmpty()) {
            getBestMediumMatch(combo)?.also { result += (it.score.toDouble() / combo.tracks.size) * 2 }
        }

        return result
    }

    private fun getBestMediumMatch(combo: AlbumWithTracksCombo): Media.AlbumMatch? = media
        .map { Media.AlbumMatch(medium = it, score = it.getAlbumMatchScore(combo)) }
        .minByOrNull { it.score }
}
