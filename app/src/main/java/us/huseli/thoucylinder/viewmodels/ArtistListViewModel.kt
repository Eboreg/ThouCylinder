package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.asFullImageBitmap
import us.huseli.thoucylinder.dataclasses.combos.ArtistCombo
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class ArtistListViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)

    val artistCombos = combine(repos.artist.albumArtistCombos, repos.artist.trackArtistCombos) { a1, a2 ->
        (a1 + a2).sortedBy { it.artist.name.lowercase() }
    }.onStart { _isLoading.value = true }.onEach { _isLoading.value = false }
    val isLoading = _isLoading.asStateFlow()

    fun flowArtistImage(combo: ArtistCombo, context: Context) = MutableStateFlow<ImageBitmap?>(null).apply {
        launchOnIOThread {
            value = repos.artist.artistImageUriCache.getOrNull(combo)?.getBitmap(context)?.asFullImageBitmap(context)
        }
    }.asStateFlow()
}
