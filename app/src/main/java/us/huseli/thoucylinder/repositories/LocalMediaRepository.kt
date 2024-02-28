package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.isWritable
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.padStart
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.copyFrom
import us.huseli.thoucylinder.copyTo
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.ID3Data
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.combos.stripTitleCommons
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.listCoverImages
import us.huseli.thoucylinder.dataclasses.extractID3Data
import us.huseli.thoucylinder.dataclasses.extractTrackMetadata
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.escapeQuotes
import us.huseli.thoucylinder.getRelativePathWithoutFilename
import us.huseli.thoucylinder.getSquareSize
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(@ApplicationContext private val context: Context, database: Database) {
    private val albumDao = database.albumDao()
    private val artistDao = database.artistDao()
    private val trackDao = database.trackDao()
    private val _isImportingLocalMedia = MutableStateFlow(false)

    val isImportingLocalMedia = _isImportingLocalMedia.asStateFlow()

    fun setIsImporting(value: Boolean) {
        _isImportingLocalMedia.value = value
    }

    /** IMAGE RELATED METHODS ************************************************/
    @WorkerThread
    fun collectNewLocalAlbumArtUris(combo: AlbumWithTracksCombo): List<Uri> =
        combo.trackCombos.map { it.track }.listCoverImages(context)
            .map { it.uri }
            .filter { it != combo.album.albumArt?.uri }

    @WorkerThread
    fun deleteAlbumDirectoryAlbumArt(
        albumCombo: AbstractAlbumCombo,
        albumDirectory: DocumentFile?,
        tracks: Collection<Track>? = null,
    ) {
        if (albumDirectory != null) albumCombo.album.albumArt?.deleteDirectoryFiles(context, albumDirectory)
        tracks?.listCoverImages(context, includeThumbnails = true)?.forEach { documentFile ->
            if (documentFile.isFile && documentFile.canWrite()) documentFile.delete()
        }
    }

    @WorkerThread
    suspend fun getBestNewLocalAlbumArt(
        trackCombos: Collection<AbstractTrackCombo>,
        current: MediaStoreImage? = null,
    ): MediaStoreImage? = trackCombos.map { it.track }.listCoverImages(context)
        .filter { it.uri != current?.uri }
        .map { MediaStoreImage.fromUri(it.uri, context) }
        .maxByOrNull { albumArt -> albumArt.getFullImageBitmap(context)?.getSquareSize() ?: 0 }

    suspend fun saveAlbumDirectoryAlbumArtFiles(albumArt: MediaStoreImage, albumDirectory: DocumentFile) =
        albumArt.saveToDirectory(context, albumDirectory)

    @WorkerThread
    suspend fun saveInternalAlbumArtFiles(albumArt: MediaStoreImage, album: Album): MediaStoreImage? =
        albumArt.saveInternal(album, context)


    /** AUDIO RELATED METHODS ************************************************/
    suspend fun importNewLocalAlbums(
        treeDocumentFile: DocumentFile,
        existingTrackUris: Collection<Uri>,
        existingAlbumsCombos: Collection<AbstractAlbumCombo>,
        getArtist: suspend (String) -> Artist,
        onEach: suspend (AlbumWithTracksCombo) -> Unit = {},
    ) {
        val albumCombos = mutableSetOf<AlbumWithTracksCombo>()
        val trackCombos = mutableListOf<TrackCombo>()
        val getExistingAlbum: suspend (ID3Data, String?, String) -> Album? =
            { id3, artistName, title ->
                val combo = existingAlbumsCombos.find {
                    (it.album.title == title && it.artists.joined() == artistName) ||
                        (it.album.musicBrainzReleaseId != null && it.album.musicBrainzReleaseId == id3.musicBrainzReleaseId)
                }

                if (combo != null) {
                    if (
                        (id3.musicBrainzReleaseId != null && combo.album.musicBrainzReleaseId == null) ||
                        (id3.musicBrainzReleaseGroupId != null && combo.album.musicBrainzReleaseGroupId == null)
                    ) {
                        combo.album.copy(
                            musicBrainzReleaseId = id3.musicBrainzReleaseId ?: combo.album.musicBrainzReleaseId,
                            musicBrainzReleaseGroupId = id3.musicBrainzReleaseGroupId
                                ?: combo.album.musicBrainzReleaseGroupId,
                        ).also { albumDao.updateAlbums(it) }
                    } else combo.album
                } else null
            }
        val getAlbumCombo: suspend (ID3Data, String?, String) -> AlbumWithTracksCombo =
            { id3, artistName, title ->
                val combo = albumCombos.find {
                    (it.album.title == title && it.artists.joined() == artistName) ||
                        (it.album.musicBrainzReleaseId != null && it.album.musicBrainzReleaseId == id3.musicBrainzReleaseId)
                }

                if (combo != null) {
                    if (
                        (id3.musicBrainzReleaseId != null && combo.album.musicBrainzReleaseId == null) ||
                        (id3.musicBrainzReleaseGroupId != null && combo.album.musicBrainzReleaseGroupId == null)
                    ) {
                        combo.copy(
                            album = combo.album.copy(
                                musicBrainzReleaseId = id3.musicBrainzReleaseId ?: combo.album.musicBrainzReleaseId,
                                musicBrainzReleaseGroupId = id3.musicBrainzReleaseGroupId
                                    ?: combo.album.musicBrainzReleaseGroupId,
                            ),
                        ).also { albumDao.updateAlbums(it.album) }
                    } else combo
                } else {
                    val artist = artistName?.let { getArtist(it) }
                    val album = Album(
                        title = title,
                        isInLibrary = true,
                        isLocal = true,
                        musicBrainzReleaseId = id3.musicBrainzReleaseId,
                        musicBrainzReleaseGroupId = id3.musicBrainzReleaseGroupId,
                    )
                    val artistCredit = artist?.let { AlbumArtistCredit(artist = it, albumId = album.albumId) }

                    AlbumWithTracksCombo(
                        album = album,
                        artists = artistCredit?.let { listOf(it) } ?: emptyList(),
                    ).also { albumCombo ->
                        albumCombos.add(albumCombo)
                        albumDao.upsertAlbums(albumCombo.album)
                        if (artistCredit != null) artistDao.insertAlbumArtists(artistCredit.toAlbumArtist())
                    }
                }
            }

        treeDocumentFile.listFiles().forEach { documentFile ->
            if (documentFile.isDirectory) {
                // Go through subdirectories recursively:
                importNewLocalAlbums(
                    treeDocumentFile = documentFile,
                    existingTrackUris = existingTrackUris,
                    getArtist = getArtist,
                    onEach = onEach,
                    existingAlbumsCombos = existingAlbumsCombos.plus(albumCombos),
                )
            } else if (documentFile.isFile && !existingTrackUris.contains(documentFile.uri)) {
                // TODO: Seems like FFprobeKit doesn't get permission to access the file even though we have read/write
                // permissions through the DocumentFile API. So we do this stupid copy to temp file shit until we have
                // a better solution.
                val mimeTypeGuess = MimeTypeMap.getSingleton().getMimeTypeFromExtension(documentFile.extension)

                // Just to avoid copying lots of irrelevant files:
                if (mimeTypeGuess?.startsWith("audio/") == true) {
                    val tempFile = documentFile.copyTo(context, context.cacheDir).also { it.deleteOnExit() }
                    val mediaInfo = FFprobeKit.getMediaInformation(tempFile.path)?.mediaInformation
                    val metadata = tempFile.extractTrackMetadata(mediaInfo)

                    if (metadata?.mimeType?.startsWith("audio/") == true) {
                        // Make an educated guess on artist/album names from path. Segment 1 is most likely a standard
                        // directory like "Music", so drop it:
                        val pathSegments = documentFile.uri.getRelativePathWithoutFilename()?.split('/')?.drop(1)
                        val id3 = mediaInfo?.extractID3Data() ?: ID3Data()
                        val (pathArtist, pathTitle) = when (pathSegments?.size) {
                            1 -> pathSegments[0].split(Regex(" +- +"), 2).padStart(2).toList()
                            0 -> listOf(null, null)
                            null -> listOf(null, null)
                            else -> pathSegments.takeLast(2)
                        }
                        val (filenameAlbumPosition, filenameTitle) =
                            Regex("^(\\d+)?[ -.]*(.*)$").find(documentFile.baseName)
                                ?.groupValues
                                ?.takeLast(2)
                                ?.map { it.nullIfEmpty() }
                                ?: listOf(null, null)
                        val albumTitle = id3.album ?: pathTitle ?: context.getString(R.string.unknown_album)
                        val albumArtist = id3.albumArtist ?: pathArtist
                        val albumId = getExistingAlbum(id3, albumArtist, albumTitle)?.albumId
                            ?: getAlbumCombo(id3, albumArtist, albumTitle).album.albumId
                        val track = Track(
                            title = id3.title ?: filenameTitle ?: context.getString(R.string.unknown_title),
                            isInLibrary = true,
                            albumPosition = id3.trackNumber ?: filenameAlbumPosition?.toIntOrNull(),
                            discNumber = id3.discNumber,
                            year = id3.year,
                            albumId = albumId,
                            metadata = metadata,
                            localUri = documentFile.uri,
                            musicBrainzId = id3.musicBrainzTrackId,
                        )
                        val artist = (id3.artist ?: pathArtist)?.let { getArtist(it) }
                        val trackArtist = artist?.let { TrackArtistCredit(artist = it, trackId = track.trackId) }
                        val trackCombo = TrackCombo(
                            track = track,
                            artists = trackArtist?.let { listOf(it) } ?: emptyList(),
                        )

                        trackDao.upsertTracks(trackCombo.track)
                        if (trackArtist != null) artistDao.insertTrackArtists(trackArtist.toTrackArtist())
                        trackCombos.add(trackCombo)
                    }
                }
            }
        }

        albumCombos.forEach { albumCombo ->
            val albumTrackCombos =
                trackCombos.filter { it.track.albumId == albumCombo.album.albumId }.stripTitleCommons()
            val albumArt = getBestNewLocalAlbumArt(albumTrackCombos)
                ?.also { albumDao.updateAlbumArt(albumCombo.album.albumId, it) }

            onEach(
                albumCombo.copy(
                    trackCombos = albumTrackCombos,
                    album = albumCombo.album.copy(albumArt = albumArt),
                )
            )
        }
    }

    fun listTracksWithBrokenLocalUris(allTracks: Collection<Track>): List<Track> {
        return allTracks.associateWith { it.localUri }
            .filterValuesNotNull()
            .filter { (_, uri) -> DocumentFile.fromSingleUri(context, uri)?.exists() != true }
            .map { it.key }
    }

    @WorkerThread
    fun tagTrack(trackCombo: AbstractTrackCombo, albumArtists: List<AlbumArtistCredit>? = null) {
        val documentFile = trackCombo.track.getDocumentFile(context)

        if (documentFile != null) {
            if (!documentFile.isWritable(context))
                Log.e(this::class.simpleName, "tagAlbumTracks: Cannot write to $documentFile")
            else tagTrack(trackCombo = trackCombo, documentFile = documentFile, albumArtists = albumArtists)
        }
    }

    @WorkerThread
    fun tagTrack(
        trackCombo: AbstractTrackCombo,
        documentFile: DocumentFile,
        albumArtists: List<AlbumArtistCredit>? = null,
    ) {
        val tmpInFile = File(context.cacheDir, "${documentFile.baseName}.in.tmp.${documentFile.extension}")
        val tmpOutFile = File(context.cacheDir, "${documentFile.baseName}.out.tmp.${documentFile.extension}")
        val tagCommands = getTagMap(trackCombo, albumArtists).map { (key, value) ->
            "-metadata \"$key=${value.escapeQuotes()}\" -metadata:s \"$key=${value.escapeQuotes()}\""
        }.joinToString(" ")
        val ffmpegCommand =
            "-i \"${tmpInFile.path}\" -y -codec copy -id3v2_version 3 $tagCommands \"${tmpOutFile.path}\""

        Log.i(javaClass.simpleName, "tagTrack: running ffmpeg $ffmpegCommand")
        documentFile.copyTo(tmpInFile, context)

        val tagSession = FFmpegKit.execute(ffmpegCommand)

        if (tagSession.returnCode.isValueSuccess) {
            documentFile.copyFrom(tmpOutFile, context)
        } else if (tagSession.returnCode.isValueError) {
            Log.e(javaClass.simpleName, "tagTrack: error, return code=${tagSession.returnCode.value}")
            tagSession.allLogsAsString?.also { Log.e(javaClass.simpleName, it) }
        } else if (tagSession.returnCode.isValueCancel) {
            Log.w(javaClass.simpleName, "tagTrack: cancel, return code=${tagSession.returnCode.value}")
            tagSession.allLogsAsString?.also { Log.e(javaClass.simpleName, it) }
        }

        tmpInFile.delete()
        tmpOutFile.delete()
    }


    /** PRIVATE METHODS *******************************************************/
    private fun getTagMap(
        trackCombo: AbstractTrackCombo,
        albumArtists: List<AlbumArtistCredit>? = null,
    ): Map<String, String> {
        val tags = mutableMapOf("title" to trackCombo.track.title)

        (trackCombo.artists.joined() ?: albumArtists?.joined())?.also { tags["artist"] = it }
        albumArtists?.joined()?.also { tags["album_artist"] = it }
        trackCombo.album?.title?.also { tags["album"] = it }
        trackCombo.track.albumPosition?.also { tags["track"] = it.toString() }
        (trackCombo.track.year ?: trackCombo.album?.year)?.also { tags["date"] = it.toString() }
        trackCombo.track.musicBrainzId?.also { tags["mb_track_id"] = it }
        trackCombo.album?.musicBrainzReleaseId?.also { tags["mb_release_id"] = it }
        trackCombo.album?.musicBrainzReleaseGroupId?.also { tags["mb_release_group_id"] = it }
        albumArtists?.firstNotNullOfOrNull { it.musicBrainzId }?.also { tags["mb_artist_id"] = it }

        return tags
    }
}
