package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import us.huseli.retaintheme.extensions.dpToPx
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.toBitmap
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.pojos.ArtistPojo
import us.huseli.thoucylinder.getSquareBitmapByUrl
import us.huseli.thoucylinder.matchDirectoriesRecursive
import us.huseli.thoucylinder.matchFiles
import us.huseli.thoucylinder.toBitmap
import javax.inject.Inject

@HiltViewModel
class ArtistListViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _artistImages = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    private val _isLoading = MutableStateFlow(true)

    val artistPojos =
        combine(repos.artist.albumArtistPojos, repos.artist.trackArtistPojos) { a1, a2 ->
            (a1 + a2).sortedBy { it.name.lowercase() }
        }.onStart { _isLoading.value = true }.onEach { _isLoading.value = false }
    val isLoading = _isLoading.asStateFlow()

    fun flowArtistImage(artistPojo: ArtistPojo, context: Context) = MutableStateFlow<ImageBitmap?>(null).apply {
        if (_artistImages.value.containsKey(artistPojo.name)) value = _artistImages.value[artistPojo.name]
        else viewModelScope.launch(Dispatchers.IO) {
            val imageBitmap = fetchArtistImage(artistPojo, context)
            _artistImages.value += artistPojo.name to imageBitmap
            value = imageBitmap
        }
    }.asStateFlow()

    private suspend fun fetchArtistImage(artistPojo: ArtistPojo, context: Context): ImageBitmap? {
        repos.settings.getLocalMusicDocumentFile()
            ?.matchDirectoriesRecursive(Regex("^${artistPojo.name}"))
            ?.map { it.matchFiles(Regex("^artist\\..*", RegexOption.IGNORE_CASE), Regex("^image/.*")) }
            ?.flatten()
            ?.distinctBy { it.uri.path }
            ?.firstNotNullOfOrNull { it.toBitmap(context) }
            ?.also { return it.asImageBitmap() }

        artistPojo.listAlbumArtUris()
            .forEach { uri -> uri.toFile().toBitmap()?.asImageBitmap()?.also { return it } }

        artistPojo.listFullImageUrls().forEach { url ->
            url.getSquareBitmapByUrl()
                ?.scaleToMaxSize(context.dpToPx(IMAGE_MAX_DP_FULL))
                ?.asImageBitmap()
                ?.also { return it }
        }

        return null
    }
}