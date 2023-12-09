package us.huseli.thoucylinder

import android.content.Context

@Suppress("unused")
enum class LoadStatus { NOT_LOADED, LOADING, LOADED, ERROR }

enum class SortOrder { ASCENDING, DESCENDING }

interface SortParameter {
    val stringRes: Int
    val sqlColumn: String
}

@Suppress("unused")
enum class TrackSortParameter : SortParameter {
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
        override val sqlColumn = "COALESCE(LOWER(Track_artist), LOWER(Album_artist))"
    },
    ALBUM_ARTIST {
        override val stringRes = R.string.album_artist
        override val sqlColumn = "LOWER(Album_artist)"
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
            entries.associateWith { context.getString(it.stringRes) }
    }
}

@Suppress("unused")
enum class AlbumSortParameter : SortParameter {
    TITLE {
        override val stringRes = R.string.title
        override val sqlColumn = "LOWER(Album_title)"
    },
    ARTIST {
        override val stringRes = R.string.artist
        override val sqlColumn = "LOWER(Album_artist)"
    },
    YEAR {
        override val stringRes = R.string.year
        override val sqlColumn = "Album_year"
    };

    companion object {
        fun withLabels(context: Context): Map<AlbumSortParameter, String> =
            entries.associateWith { context.getString(it.stringRes) }
    }
}

enum class MenuItemId(val route: String) {
    SEARCH_YOUTUBE("search-youtube"),
    LIBRARY("library"),
    QUEUE("queue"),
    IMPORT("import"),
    DEBUG("debug"),
    DOWNLOADS("downloads"),
    SETTINGS("settings"),
    MENU("menu"),
}
