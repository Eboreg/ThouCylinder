package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.media3.common.PlaybackException
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.radio.RadioCombo
import us.huseli.thoucylinder.enums.RadioType
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(@ApplicationContext private val context: Context) : AbstractScopeHolder(),
    ILogger {
    fun onActivateRadio(radioCombo: RadioCombo) {
        val message =
            if (radioCombo.type == RadioType.LIBRARY) context.getUmlautifiedString(R.string.activating_library_radio)
            else context.getString(R.string.activating_x_radio, radioCombo.title).umlautify()

        showInfoSnackbar(message)
    }

    fun onAddAlbumsToLibrary(
        albumIds: List<String>,
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        if (albumIds.size == 1) {
            if (onGotoAlbumClick != null) {
                SnackbarEngine.addInfo(
                    message = context.getUmlautifiedString(R.string.the_album_was_added_to_the_library),
                    actionLabel = context.getUmlautifiedString(R.string.go_to_album),
                    onActionPerformed = { onGotoAlbumClick(albumIds[0]) },
                )
            } else {
                SnackbarEngine.addInfo(context.getUmlautifiedString(R.string.the_album_was_added_to_the_library))
            }
        } else {
            val message = context.getUmlautifiedString(R.string.x_albums_were_added_to_the_library, albumIds.size)

            if (onGotoLibraryClick != null) {
                SnackbarEngine.addInfo(
                    message = message,
                    actionLabel = context.getUmlautifiedString(R.string.go_to_library),
                    onActionPerformed = onGotoLibraryClick,
                )
            } else {
                SnackbarEngine.addInfo(message)
            }
        }
    }

    fun onAddTracksToPlaylist(trackCount: Int, onPlaylistClick: () -> Unit) {
        SnackbarEngine.addInfo(
            message = context.resources
                .getQuantityString(R.plurals.x_tracks_added_to_playlist, trackCount, trackCount)
                .umlautify(),
            actionLabel = context.getUmlautifiedString(R.string.go_to_playlist),
            onActionPerformed = onPlaylistClick,
        )
    }

    fun onAlbumDownloadFinish(album: String, result: AlbumDownloadTask.Result, onGotoAlbumClick: () -> Unit) {
        if (result.succeededTracks.isEmpty() && result.failedTracks.isNotEmpty()) {
            showErrorSnackbar(context.getUmlautifiedString(R.string.x_could_not_be_downloaded, album))
        } else if (result.succeededTracks.isNotEmpty()) {
            val message = listOfNotNull(
                context.resources
                    .getQuantityString(
                        R.plurals.x_tracks_were_successfully_downloaded,
                        result.succeededTracks.size,
                        result.succeededTracks.size,
                    ).umlautify(),
                result.failedTracks.takeIf { it.isNotEmpty() }?.let {
                    context.resources.getQuantityString(
                        R.plurals.however_x_tracks_could_not_be_downloaded,
                        result.failedTracks.size,
                        result.failedTracks.size,
                    ).umlautify()
                },
            ).joinToString(" ")

            SnackbarEngine.addInfo(
                message = message,
                actionLabel = context.getUmlautifiedString(R.string.go_to_album),
                onActionPerformed = onGotoAlbumClick,
            )
        }
    }

    fun onDeleteLocalAlbumFiles(albumCount: Int, firstTitle: String) {
        showInfoSnackbar(
            context.resources
                .getQuantityString(R.plurals.deleted_local_album_files, albumCount, albumCount, firstTitle).umlautify(),
        )
    }

    fun onDeletePlaylist(onUndoClick: () -> Unit) {
        SnackbarEngine.addInfo(
            message = context.getUmlautifiedString(R.string.the_playlist_was_deleted),
            actionLabel = context.getUmlautifiedString(R.string.undo),
            onActionPerformed = onUndoClick,
        )
    }

    fun onEnqueueTracksNext(trackCount: Int) {
        showInfoSnackbar(
            context.resources
                .getQuantityString(R.plurals.x_tracks_enqueued_next, trackCount, trackCount).umlautify(),
        )
    }

    fun onExportPlaylist(success: Boolean, path: String? = null) {
        if (success) showInfoSnackbar(
            context
                .getString(R.string.playlist_exported_to_x, path).umlautify()
        )
        else showErrorSnackbar(context.getUmlautifiedString(R.string.could_not_save_playlist_for_some_reason))
    }

    fun onHideAlbums(albumCount: Int, firstTitle: String, onUndoClick: () -> Unit) {
        SnackbarEngine.addInfo(
            message = context.resources
                .getQuantityString(R.plurals.hid_x_albums_from_library, albumCount, albumCount, firstTitle)
                .umlautify(),
            actionLabel = context.getUmlautifiedString(R.string.undo),
            onActionPerformed = onUndoClick,
        )
    }

    fun onHideAlbumsAndDeleteFiles(albumCount: Int, firstTitle: String, onUndoClick: () -> Unit) {
        SnackbarEngine.addInfo(
            message = context.resources
                .getQuantityString(R.plurals.removed_x_albums_and_local_files, albumCount, albumCount, firstTitle)
                .umlautify(),
            actionLabel = context.getUmlautifiedString(R.string.undelete_album),
            onActionPerformed = onUndoClick,
        )
    }

    fun onLastFmAuthError(error: Throwable) {
        showErrorSnackbar(context.getUmlautifiedString(R.string.last_fm_authorization_failed, error))
    }

    fun onMatchUnplayableTracks(trackCount: Int) {
        if (trackCount > 0) showInfoSnackbar(
            context.resources
                .getQuantityString(R.plurals.x_tracks_matched_and_updated, trackCount, trackCount).umlautify()
        )
        else showErrorSnackbar(message = context.getUmlautifiedString(R.string.no_tracks_could_be_matched))
    }

    fun onPlayerError(error: PlaybackException) {
        showErrorSnackbar(error.toString())
    }

    fun onRadioRecommendationsNotFound(radioTitle: String?) {
        showErrorSnackbar(
            if (radioTitle != null) context.getUmlautifiedString(
                R.string.could_not_start_radio_for_x,
                radioTitle
            )
            else context.getUmlautifiedString(R.string.could_not_start_radio),
        )
    }

    fun onSaveAlbumArtFromUri(success: Boolean) {
        if (success) showInfoSnackbar(context.getUmlautifiedString(R.string.updated_album_cover))
        else showErrorSnackbar(context.getUmlautifiedString(R.string.could_not_open_the_selected_image))
    }

    fun onSpotifyAuthError(error: Throwable) {
        showErrorSnackbar(context.getUmlautifiedString(R.string.spotify_authorization_failed, error))
    }

    fun onTrackDownloadError(track: String, error: Throwable) {
        showErrorSnackbar(context.getUmlautifiedString(R.string.error_on_downloading_x, track, error))
    }

    fun onUndeletePlaylist(onGotoPlaylistClick: () -> Unit) {
        SnackbarEngine.addInfo(
            message = context.getUmlautifiedString(R.string.the_playlist_was_restored),
            actionLabel = context.getUmlautifiedString(R.string.go_to_playlist),
            onActionPerformed = onGotoPlaylistClick,
        )
    }

    fun onUnhideLocalAlbums() {
        showInfoSnackbar(context.getUmlautifiedString(R.string.all_local_albums_have_been_unhidden))
    }
}
