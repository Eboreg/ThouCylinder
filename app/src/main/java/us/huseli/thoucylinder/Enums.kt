package us.huseli.thoucylinder

import android.content.Context
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album

enum class SortOrder(val sql: String) { ASCENDING("ASC"), DESCENDING("DESC") }

interface SortParameter<T> {
    val stringRes: Int
    val sqlColumn: String
}

enum class TrackSortParameter : SortParameter<AbstractTrackCombo> {
    TITLE {
        override val stringRes = R.string.title
        override val sqlColumn = "LOWER(Track_title)"
    },
    ALBUM_TITLE {
        override val stringRes = R.string.album_title
        override val sqlColumn = "LOWER(Album_title)"
    },
    ARTIST {
        override val stringRes = R.string.artist
        override val sqlColumn = "COALESCE(LOWER(trackArtist), LOWER(albumArtist))"
    },
    ALBUM_ARTIST {
        override val stringRes = R.string.album_artist
        override val sqlColumn = "LOWER(albumArtist)"
    },
    YEAR {
        override val stringRes = R.string.year
        override val sqlColumn = "COALESCE(Track_year, Album_year)"
    },
    DURATION {
        override val stringRes = R.string.duration
        override val sqlColumn = "COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs)"
    };

    companion object {
        fun withLabels(context: Context): Map<TrackSortParameter, String> =
            entries.associateWith { context.getString(it.stringRes).umlautify() }
    }
}

enum class AlbumSortParameter : SortParameter<Album> {
    TITLE {
        override val stringRes = R.string.title
        override val sqlColumn = "LOWER(Album_title)"
    },
    ARTIST {
        override val stringRes = R.string.artist
        override val sqlColumn = "LOWER(AlbumArtist_name)"
    },
    YEAR {
        override val stringRes = R.string.year
        override val sqlColumn = "Album_year"
    };

    companion object {
        fun withLabels(context: Context): Map<AlbumSortParameter, String> =
            entries.associateWith { context.getString(it.stringRes).umlautify() }
    }
}

enum class AvailabilityFilter(val stringRes: Int) {
    ALL(R.string.all),
    ONLY_PLAYABLE(R.string.only_playable),
    ONLY_LOCAL(R.string.only_local),
}
