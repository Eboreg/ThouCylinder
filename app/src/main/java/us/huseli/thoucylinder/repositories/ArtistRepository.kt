package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.combos.ArtistCombo
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.entities.enumerate
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.getMutexCache
import us.huseli.thoucylinder.matchDirectoriesRecursive
import us.huseli.thoucylinder.matchFiles
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val artistDao = database.artistDao()
    private val levenshtein = LevenshteinDistance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _allArtists = MutableStateFlow<List<Artist>>(emptyList())
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())

    val trackArtistCombos = artistDao.flowTrackArtistCombos()
    val albumArtistCombos = artistDao.flowAlbumArtistCombos()
    val artistCache: MutexCache<String, String, Artist> =
        getMutexCache("artistCache") { name -> artistDao.getOrCreateArtistByName(name) }
    val artistImageUriCache = MutexCache<ArtistCombo, UUID, Uri?>(
        itemToKey = { it.artist.id },
        fetchMethod = { combo ->
            combo.artist.image?.uri
                ?: localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }
                    ?.matchDirectoriesRecursive(Regex("^${combo.artist.name}"))
                    ?.map { it.matchFiles(Regex("^artist\\..*", RegexOption.IGNORE_CASE), Regex("^image/.*")) }
                    ?.flatten()
                    ?.distinctBy { it.uri.path }
                    ?.firstOrNull()
                    ?.uri
                ?: combo.listAlbumArtUris().firstOrNull()
                ?: combo.listFullImageUrls().firstOrNull()?.toUri()
        },
    )

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        scope.launch {
            artistDao.flowArtists().collect { _allArtists.value = it }
        }
    }

    suspend fun clearArtists() = artistDao.clearArtists()

    suspend fun deleteOrphans() = artistDao.deleteOrphans()

    fun flowArtistById(id: UUID) = artistDao.flowArtistById(id)

    fun flowArtists() = artistDao.flowArtists()

    fun getArtistNameSuggestions(name: String, limit: Int = 10) = _allArtists.value
        .filter { it.name.contains(name, true) }
        .map { it.name }
        .sortedBy { levenshtein.apply(name.lowercase(), it.lowercase()) }
        .slice(0, limit)

    suspend fun insertAlbumArtists(albumArtists: Collection<AlbumArtist>) {
        if (albumArtists.isNotEmpty()) artistDao.insertAlbumArtists(*albumArtists.toTypedArray())
    }

    suspend fun insertTrackArtists(trackArtists: Collection<TrackArtist>) {
        if (trackArtists.isNotEmpty()) artistDao.insertTrackArtists(*trackArtists.toTypedArray())
    }

    suspend fun listArtists(): List<Artist> = artistDao.listArtists()

    suspend fun listTopSpotifyArtists(limit: Int = 10): List<TopLocalSpotifyArtistPojo> =
        artistDao.listTopSpotifyArtists(limit)

    suspend fun setAlbumArtists(albumArtists: Collection<AlbumArtist>) {
        if (albumArtists.isNotEmpty()) {
            artistDao.clearAlbumArtists(*albumArtists.map { it.albumId }.toSet().toTypedArray())
            artistDao.insertAlbumArtists(*albumArtists.enumerate().toTypedArray())
        }
    }

    suspend fun setTrackArtists(trackArtists: Collection<TrackArtist>) {
        if (trackArtists.isNotEmpty()) {
            artistDao.clearTrackArtists(*trackArtists.map { it.trackId }.toSet().toTypedArray())
            artistDao.insertTrackArtists(*trackArtists.toTypedArray())
        }
    }

    suspend fun updateArtist(artist: Artist) = updateArtists(listOf(artist))

    suspend fun updateArtists(artists: Collection<Artist>) {
        if (artists.isNotEmpty()) artistDao.updateArtists(*artists.toTypedArray())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_LOCAL_MUSIC_URI) localMusicUri.value = preferences.getString(key, null)?.toUri()
    }
}
