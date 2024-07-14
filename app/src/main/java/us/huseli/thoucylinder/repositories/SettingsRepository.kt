package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.thoucylinder.Constants.PREF_APP_START_COUNT
import us.huseli.thoucylinder.Constants.PREF_AUTO_IMPORT_LOCAL_MUSIC
import us.huseli.thoucylinder.Constants.PREF_LIBRARY_RADIO_NOVELTY
import us.huseli.thoucylinder.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.thoucylinder.Constants.PREF_REGION
import us.huseli.thoucylinder.Constants.PREF_UMLAUTIFY
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Umlautify
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.tag.TagPojo
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.ArtistSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.Region
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _albumSearchTerm = MutableStateFlow("")
    private val _albumSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _albumSortParameter = MutableStateFlow(AlbumSortParameter.ARTIST)
    private val _appStartCount = MutableStateFlow(preferences.getInt(PREF_APP_START_COUNT, 1))
    private val _artistSearchTerm = MutableStateFlow("")
    private val _artistSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _artistSortParameter = MutableStateFlow(ArtistSortParameter.NAME)
    private val _autoImportLocalMusic = MutableStateFlow(
        if (preferences.contains(PREF_AUTO_IMPORT_LOCAL_MUSIC))
            preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
        else null
    )
    private val _contentSize = MutableStateFlow(Size.Zero)
    private val _libraryAlbumTagFilter = MutableStateFlow<ImmutableList<TagPojo>>(persistentListOf())
    private val _libraryAvailabilityFilter = MutableStateFlow(AvailabilityFilter.ALL)
    private val _libraryDisplayType = MutableStateFlow(DisplayType.LIST)
    private val _libraryListType = MutableStateFlow(ListType.ALBUMS)
    private val _libraryRadioNovelty = MutableStateFlow(preferences.getFloat(PREF_LIBRARY_RADIO_NOVELTY, 0.5f))
    private val _libraryTrackTagFilter = MutableStateFlow<ImmutableList<TagPojo>>(persistentListOf())
    private val _localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())
    private val _region =
        MutableStateFlow(preferences.getString(PREF_REGION, null)?.let { Region.valueOf(it) } ?: Region.SE)
    private val _screenSize = MutableStateFlow<Size?>(null)
    private val _screenSizeDp = MutableStateFlow<DpSize?>(null)
    private val _showArtistsWithoutAlbums = MutableStateFlow(false)
    private val _trackSearchTerm = MutableStateFlow("")
    private val _trackSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _trackSortParameter = MutableStateFlow(TrackSortParameter.TITLE)

    val albumSearchTerm = _albumSearchTerm.asStateFlow()
    val albumSortOrder = _albumSortOrder.asStateFlow()
    val albumSortParameter = _albumSortParameter.asStateFlow()
    val appStartCount = _appStartCount.asStateFlow()
    val artistSearchTerm = _artistSearchTerm.asStateFlow()
    val artistSortOrder = _artistSortOrder.asStateFlow()
    val artistSortParameter = _artistSortParameter.asStateFlow()
    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val contentSize: StateFlow<Size> = _contentSize.asStateFlow()
    val libraryAlbumTagFilter = _libraryAlbumTagFilter.asStateFlow()
    val libraryAvailabilityFilter = _libraryAvailabilityFilter.asStateFlow()
    val libraryDisplayType = _libraryDisplayType.asStateFlow()
    val libraryListType = _libraryListType.asStateFlow()
    val libraryRadioNovelty: StateFlow<Float> = _libraryRadioNovelty.asStateFlow()
    val libraryTrackTagFilter = _libraryTrackTagFilter.asStateFlow()
    val localMusicUri: StateFlow<Uri?> = _localMusicUri.asStateFlow()
    val region: StateFlow<Region> = _region.asStateFlow()
    val showArtistsWithoutAlbums = _showArtistsWithoutAlbums.asStateFlow()
    val trackSearchTerm = _trackSearchTerm.asStateFlow()
    val trackSortOrder = _trackSortOrder.asStateFlow()
    val trackSortParameter = _trackSortParameter.asStateFlow()

    init {
        Umlautify.setEnabled(preferences.getBoolean(PREF_UMLAUTIFY, false))
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun createAlbumDirectory(albumTitle: String, artistString: String?): DocumentFile? {
        val subdirs = listOf(
            artistString?.sanitizeFilename() ?: context.getString(R.string.unknown_artist),
            albumTitle.sanitizeFilename(),
        )

        return getLocalMusicDirectory()?.makeFolder(context, subdirs.joinToString("/"), CreateMode.REUSE)
    }

    fun getLocalMusicDirectory(): DocumentFile? = _localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun setAlbumSearchTerm(value: String) {
        _albumSearchTerm.value = value
    }

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) {
        _albumSortParameter.value = sortParameter
        _albumSortOrder.value = sortOrder
    }

    fun setLibraryAvailabilityFilter(value: AvailabilityFilter) {
        _libraryAvailabilityFilter.value = value
    }

    fun setArtistSearchTerm(value: String) {
        _artistSearchTerm.value = value
    }

    fun setArtistSorting(sortParameter: ArtistSortParameter, sortOrder: SortOrder) {
        _artistSortParameter.value = sortParameter
        _artistSortOrder.value = sortOrder
    }

    fun setAutoImportLocalMusic(value: Boolean) {
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
    }

    fun setContentSize(size: Size) {
        _contentSize.value = size
    }

    fun setLibraryAlbumTagFilter(value: List<TagPojo>) {
        _libraryAlbumTagFilter.value = value.toImmutableList()
    }

    fun setLibraryDisplayType(value: DisplayType) {
        _libraryDisplayType.value = value
    }

    fun setLibraryListType(value: ListType) {
        _libraryListType.value = value
    }

    fun setLibraryRadioNovelty(value: Float) {
        preferences.edit().putFloat(PREF_LIBRARY_RADIO_NOVELTY, value).apply()
    }

    fun setLibraryTrackTagFilter(value: List<TagPojo>) {
        _libraryTrackTagFilter.value = value.toImmutableList()
    }

    fun setLocalMusicUri(value: Uri?) {
        preferences.edit().putString(PREF_LOCAL_MUSIC_URI, value?.toString()).apply()
    }

    fun setRegion(value: Region) {
        preferences.edit().putString(PREF_REGION, value.name).apply()
    }

    fun setScreenSize(dpSize: DpSize, size: Size) {
        _screenSize.value = size
        _screenSizeDp.value = dpSize
    }

    fun setShowArtistsWithoutAlbums(value: Boolean) {
        _showArtistsWithoutAlbums.value = value
    }

    fun setTrackSearchTerm(value: String) {
        _trackSearchTerm.value = value
    }

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) {
        _trackSortParameter.value = sortParameter
        _trackSortOrder.value = sortOrder
    }

    fun setUmlautify(value: Boolean) {
        preferences.edit().putBoolean(PREF_UMLAUTIFY, value).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_APP_START_COUNT -> _appStartCount.value = preferences.getInt(PREF_APP_START_COUNT, 1)
            PREF_UMLAUTIFY -> Umlautify.setEnabled(preferences.getBoolean(PREF_UMLAUTIFY, false))
            PREF_LOCAL_MUSIC_URI -> _localMusicUri.value = preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri()
            PREF_LIBRARY_RADIO_NOVELTY -> _libraryRadioNovelty.value =
                preferences.getFloat(PREF_LIBRARY_RADIO_NOVELTY, 0.5f)
            PREF_AUTO_IMPORT_LOCAL_MUSIC -> _autoImportLocalMusic.value =
                preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
            PREF_REGION -> preferences.getString(PREF_REGION, null)
                ?.let { Region.valueOf(it) }
                ?.also { _region.value = it }
        }
    }
}
