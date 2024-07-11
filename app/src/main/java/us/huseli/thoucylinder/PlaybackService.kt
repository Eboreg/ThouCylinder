package us.huseli.thoucylinder

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_BACK
import androidx.media3.common.Player.COMMAND_SEEK_FORWARD
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import kotlinx.collections.immutable.persistentListOf

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    private inner class Callback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            return AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    ConnectionResult.DEFAULT_PLAYER_COMMANDS
                        .buildUpon()
                        .addAll(
                            COMMAND_SEEK_TO_PREVIOUS,
                            COMMAND_SEEK_BACK,
                            COMMAND_PLAY_PAUSE,
                            COMMAND_SEEK_FORWARD,
                            COMMAND_SEEK_TO_NEXT,
                        )
                        .build()
                )
                .build()
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(Callback())
            .setCustomLayout(getCommandButtons())
            .build()
            .also {
                it.setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        666,
                        Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setClass(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                )
            }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun getCommandButtons() = persistentListOf(
        CommandButton.Builder().setPlayerCommand(COMMAND_SEEK_TO_PREVIOUS).build(),
        CommandButton.Builder().setPlayerCommand(COMMAND_SEEK_BACK).build(),
        CommandButton.Builder().setPlayerCommand(COMMAND_PLAY_PAUSE).build(),
        CommandButton.Builder().setPlayerCommand(COMMAND_SEEK_FORWARD).build(),
        CommandButton.Builder().setPlayerCommand(COMMAND_SEEK_TO_NEXT).build(),
    )
}
