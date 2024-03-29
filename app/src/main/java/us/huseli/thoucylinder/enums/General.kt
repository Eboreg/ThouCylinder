package us.huseli.thoucylinder.enums

import androidx.annotation.StringRes
import us.huseli.thoucylinder.R

enum class AvailabilityFilter(@StringRes val stringRes: Int) {
    ALL(R.string.all),
    ONLY_PLAYABLE(R.string.only_playable),
    ONLY_LOCAL(R.string.only_local),
}

enum class PlaybackState { STOPPED, PLAYING, PAUSED }

enum class RadioState { INACTIVE, LOADING, LOADED_FIRST, LOADED }

enum class RadioType(@StringRes val stringRes: Int) {
    LIBRARY(R.string.library),
    ARTIST(R.string.artist),
    ALBUM(R.string.album),
    TRACK(R.string.track),
}
