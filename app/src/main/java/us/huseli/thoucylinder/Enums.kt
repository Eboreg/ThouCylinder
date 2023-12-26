package us.huseli.thoucylinder

import android.content.Context
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album

@Suppress("unused")
enum class LoadStatus { NOT_LOADED, LOADING, LOADED, ERROR }

enum class SortOrder(val sql: String) { ASCENDING("ASC"), DESCENDING("DESC") }

interface SortParameter<T> {
    val stringRes: Int
    val sqlColumn: String

    fun getStringValue(obj: T): String
}

enum class TrackSortParameter : SortParameter<AbstractTrackPojo> {
    TITLE {
        override val stringRes = R.string.title
        override val sqlColumn = "LOWER(Track_title)"
        override fun getStringValue(obj: AbstractTrackPojo): String = obj.track.title
    },
    ALBUM_TITLE {
        override val stringRes = R.string.album_title
        override val sqlColumn = "LOWER(Album_title)"
        override fun getStringValue(obj: AbstractTrackPojo): String = obj.album?.title ?: ""
    },
    ARTIST {
        override val stringRes = R.string.artist
        override val sqlColumn = "COALESCE(LOWER(Track_artist), LOWER(Album_artist))"
        override fun getStringValue(obj: AbstractTrackPojo): String = obj.track.artist ?: obj.album?.artist ?: ""
    },
    ALBUM_ARTIST {
        override val stringRes = R.string.album_artist
        override val sqlColumn = "LOWER(Album_artist)"
        override fun getStringValue(obj: AbstractTrackPojo): String = obj.album?.artist ?: ""
    },
    YEAR {
        override val stringRes = R.string.year
        override val sqlColumn = "COALESCE(Track_year, Album_year)"
        override fun getStringValue(obj: AbstractTrackPojo): String =
            obj.track.year?.toString() ?: obj.album?.year?.toString() ?: ""
    },
    DURATION {
        override val stringRes = R.string.duration
        override val sqlColumn = "COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs)"
        override fun getStringValue(obj: AbstractTrackPojo): String =
            obj.track.duration?.inWholeMilliseconds?.toString() ?: ""
    };

    companion object {
        fun withLabels(context: Context): Map<TrackSortParameter, String> =
            entries.associateWith { context.getString(it.stringRes) }
    }
}

enum class AlbumSortParameter : SortParameter<Album> {
    TITLE {
        override val stringRes = R.string.title
        override val sqlColumn = "LOWER(Album_title)"
        override fun getStringValue(obj: Album): String = obj.title
    },
    ARTIST {
        override val stringRes = R.string.artist
        override val sqlColumn = "LOWER(Album_artist)"
        override fun getStringValue(obj: Album): String = obj.artist ?: ""
    },
    YEAR {
        override val stringRes = R.string.year
        override val sqlColumn = "Album_year"
        override fun getStringValue(obj: Album): String = obj.year?.toString() ?: ""
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
