package us.huseli.thoucylinder.dataclasses.callbacks

data class RadioCallbacks(
    val deactivate: () -> Unit,
    val requestMoreTracks: () -> Unit,
)
