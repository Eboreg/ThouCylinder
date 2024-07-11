package us.huseli.thoucylinder.enums

import android.content.Context
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.artist.ArtistCombo
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo
import us.huseli.thoucylinder.getUmlautifiedString

interface SortParameter<T> {
    val stringRes: Int
    fun sql(sortOrder: SortOrder): String
}

enum class AlbumSortParameter : SortParameter<Album> {
    TITLE {
        override val stringRes = R.string.title
        override fun sql(sortOrder: SortOrder): String = "LOWER(Album_title) ${sortOrder.sql}"
    },
    ARTIST {
        override val stringRes = R.string.artist
        override fun sql(sortOrder: SortOrder) =
            "LOWER(AlbumArtist_name) ${sortOrder.sql}, LOWER(Album_title) ${sortOrder.sql}"
    },
    YEAR {
        override val stringRes = R.string.year
        override fun sql(sortOrder: SortOrder): String = "COALESCE(Album_year, AlbumCombo_minYear) ${sortOrder.sql}"
    };

    companion object {
        fun withLabels(context: Context): ImmutableMap<AlbumSortParameter, String> =
            entries.associateWith { context.getUmlautifiedString(it.stringRes) }.toImmutableMap()
    }
}

enum class ArtistSortParameter : SortParameter<ArtistCombo> {
    NAME {
        override val stringRes = R.string.name
        override fun sql(sortOrder: SortOrder) = "LOWER(Artist_name) ${sortOrder.sql}"
    },
    ALBUM_COUNT {
        override val stringRes = R.string.album_count
        override fun sql(sortOrder: SortOrder) = "albumCount ${sortOrder.sql}"
    },
    TRACK_COUNT {
        override val stringRes = R.string.track_count
        override fun sql(sortOrder: SortOrder) = "trackCount ${sortOrder.sql}"
    };

    companion object {
        fun withLabels(context: Context): ImmutableMap<ArtistSortParameter, String> =
            entries.associateWith { context.getUmlautifiedString(it.stringRes) }.toImmutableMap()
    }
}

enum class SortOrder(val sql: String) { ASCENDING("ASC"), DESCENDING("DESC") }

enum class TrackSortParameter : SortParameter<ITrackCombo> {
    TITLE {
        override val stringRes = R.string.title
        override fun sql(sortOrder: SortOrder): String = "LOWER(Track_title) ${sortOrder.sql}"
    },
    ALBUM_TITLE {
        override val stringRes = R.string.album_title
        override fun sql(sortOrder: SortOrder): String = "LOWER(Album_title) ${sortOrder.sql}"
    },
    ARTIST {
        override val stringRes = R.string.artist
        override fun sql(sortOrder: SortOrder): String =
            "COALESCE(LOWER(trackArtist), LOWER(albumArtist)) ${sortOrder.sql}"
    },
    ALBUM_ARTIST {
        override val stringRes = R.string.album_artist
        override fun sql(sortOrder: SortOrder): String = "LOWER(albumArtist) ${sortOrder.sql}"
    },
    YEAR {
        override val stringRes = R.string.year
        override fun sql(sortOrder: SortOrder): String = "COALESCE(Track_year, Album_year) ${sortOrder.sql}"
    },
    DURATION {
        override val stringRes = R.string.duration
        override fun sql(sortOrder: SortOrder): String =
            "COALESCE(Track_durationMs, Track_youtubeVideo_durationMs) ${sortOrder.sql}"
    };

    companion object {
        fun withLabels(context: Context): ImmutableMap<TrackSortParameter, String> =
            entries.associateWith { context.getUmlautifiedString(it.stringRes) }.toImmutableMap()
    }
}
