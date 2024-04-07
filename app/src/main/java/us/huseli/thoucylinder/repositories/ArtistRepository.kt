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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.AlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.TrackArtist
import us.huseli.thoucylinder.dataclasses.entities.enumerate
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.matchDirectoriesRecursive
import us.huseli.thoucylinder.matchFiles
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    inner class ArtistCache : MutexCache<UnsavedArtist, String, Artist>(
        fetchMethod = { artistDao.createOrUpdateArtist(it) },
        itemToKey = { it.name },
        debugLabel = "artistCache",
    ) {
        suspend fun getByName(name: String): Artist = get(UnsavedArtist(name = name))
    }

    private val artistDao = database.artistDao()
    private val levenshtein = LevenshteinDistance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val allArtists = MutableStateFlow<List<Artist>>(emptyList())
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())

    val artistsWithTracksOrAlbums = artistDao.flowArtistsWithTracksOrAlbums().distinctUntilChanged()
    val artistCombos = artistDao.flowArtistCombos()
    val artistCache = ArtistCache()
    val artistImageUriCache = MutexCache<ArtistCombo, String, Uri?>(
        itemToKey = { it.artist.artistId },
        debugLabel = "ArtistRepository.artistImageUriCache",
        fetchMethod = { combo ->
            combo.artist.image?.fullUri
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
            artistDao.flowArtists().distinctUntilChanged().collect { allArtists.value = it }
        }
    }

    suspend fun clearArtists() = artistDao.clearArtists()

    fun flowArtistById(id: String) = artistDao.flowArtistById(id)

    suspend fun getArtist(id: String) = artistDao.getArtist(id)

    suspend fun getArtistImage(combo: ArtistCombo) = withContext(Dispatchers.IO) {
        artistImageUriCache.getOrNull(combo)?.getBitmap(context)?.asThumbnailImageBitmap(context)
    }

    fun getArtistNameSuggestions(name: String, limit: Int = 10) = allArtists.value
        .filter { it.name.contains(name, true) }
        .map { it.name }
        .sortedBy { levenshtein.apply(name.lowercase(), it.lowercase()) }
        .take(limit)

    suspend fun insertAlbumArtists(albumArtists: Collection<AlbumArtist>) {
        if (albumArtists.isNotEmpty()) artistDao.insertAlbumArtists(*albumArtists.toTypedArray())
    }

    suspend fun insertTrackArtists(trackArtists: Collection<TrackArtist>) {
        if (trackArtists.isNotEmpty()) artistDao.insertTrackArtists(*trackArtists.toTypedArray())
    }

    suspend fun listTopSpotifyArtists(limit: Int = 10): List<TopLocalSpotifyArtistPojo> =
        artistDao.listTopSpotifyArtists(limit)

    suspend fun listTrackArtistCredits(trackId: String): List<TrackArtistCredit> =
        artistDao.listTrackArtistCredits(trackId)

    suspend fun setAlbumArtists(albumArtists: Collection<AlbumArtist>) {
        if (albumArtists.isNotEmpty()) {
            artistDao.clearAlbumArtists(*albumArtists.map { it.albumId }.toSet().toTypedArray())
            artistDao.insertAlbumArtists(*albumArtists.enumerate().toTypedArray())
        }
    }

    suspend fun setArtistMusicBrainzId(artistId: String, musicBrainzId: String) =
        artistDao.setMusicBrainzId(artistId, musicBrainzId)

    suspend fun setArtistSpotifyData(artistId: String, spotifyId: String, image: MediaStoreImage?) =
        artistDao.setSpotifyData(
            artistId = artistId,
            spotifyId = spotifyId,
            imageUri = image?.fullUriString,
            imageThumbnailUri = image?.thumbnailUriString,
            imageHash = image?.hash
        )

    suspend fun setArtistSpotifyId(artistId: String, spotifyId: String) = artistDao.setSpotifyId(artistId, spotifyId)

    suspend fun setTrackArtists(trackArtists: Collection<TrackArtist>) {
        if (trackArtists.isNotEmpty()) {
            artistDao.clearTrackArtists(*trackArtists.map { it.trackId }.toSet().toTypedArray())
            artistDao.insertTrackArtists(*trackArtists.toTypedArray())
        }
    }

    suspend fun upsertSpotifyArtist(artist: SpotifyArtist) = artistDao.upsertSpotifyArtist(artist)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_LOCAL_MUSIC_URI) localMusicUri.value = preferences.getString(key, null)?.toUri()
    }
}
