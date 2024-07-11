package us.huseli.thoucylinder.externalcontent

import androidx.annotation.StringRes
import us.huseli.thoucylinder.R

enum class ImportBackend(@StringRes val stringRes: Int) {
    LOCAL(R.string.local),
    SPOTIFY(R.string.spotify),
    LAST_FM(R.string.last_fm),
}

enum class SearchBackend(@StringRes val stringRes: Int) {
    YOUTUBE(R.string.youtube),
    SPOTIFY(R.string.spotify),
    MUSICBRAINZ(R.string.musicbrainz),
}

enum class ExternalListType(@StringRes val stringRes: Int) {
    ALBUMS(R.string.albums),
    TRACKS(R.string.tracks),
}

enum class SearchCapability {
    TRACK,
    ALBUM,
    ARTIST,
    FREE_TEXT,
}
