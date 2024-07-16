package us.huseli.thoucylinder.widget

import us.huseli.thoucylinder.R

enum class WidgetButton(val title: Int, val drawable: Int) {
    PREVIOUS(R.string.previous_track, R.drawable.media3_notification_seek_to_previous),
    REWIND(R.string.rewind_10s, R.drawable.media3_notification_seek_back),
    PLAY_OR_PAUSE(R.string.play_pause, R.drawable.media3_notification_play),
    FORWARD(R.string.forward_10s, R.drawable.media3_notification_seek_forward),
    NEXT(R.string.next_track, R.drawable.media3_notification_seek_to_next),
    REPEAT(R.string.toggle_repeat, R.drawable.repeat),
    SHUFFLE(R.string.toggle_shuffle, R.drawable.shuffle);

    companion object {
        val default: List<WidgetButton> = listOf(PREVIOUS, REWIND, PLAY_OR_PAUSE, FORWARD, NEXT)
        val defaultNames: Set<String> = default.map { it.name }.toSet()

        fun fromNames(names: Collection<String>) = names.mapNotNull {
            try {
                WidgetButton.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
