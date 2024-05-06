package us.huseli.thoucylinder.widget

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import us.huseli.thoucylinder.MainActivity
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.managers.ImageManager
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.umlautify

@SuppressLint("RestrictedApi")
@Composable
fun Widget(playerRepo: PlayerRepository, imageManager: ImageManager) {
    val context = LocalContext.current
    val canGotoNext by playerRepo.canGotoNext.collectAsState()
    val canPlay by playerRepo.canPlay.collectAsState(false)
    val currentCombo by playerRepo.currentCombo.collectAsState()
    val isPlaying by playerRepo.isPlaying.collectAsState(false)
    val isShuffleEnabled by playerRepo.isShuffleEnabled.collectAsState()
    val isRepeatEnabled by playerRepo.isRepeatEnabled.collectAsState()

    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val backgroundColor =
        if (bitmap.value != null) ColorProvider(R.color.widget_background) else GlanceTheme.colors.background
    val currentTrackString = currentCombo
        ?.let { listOfNotNull(it.artists.joined(), it.track.title) }
        ?.joinToString(" • ")
        ?: context.getString(R.string.no_track_playing).umlautify()

    LaunchedEffect(currentCombo) {
        bitmap.value = currentCombo?.let { imageManager.getTrackComboFullBitmap(it) }
    }

    Box(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(8.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        bitmap.value?.also {
            Image(
                provider = ImageProvider(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        Box(modifier = GlanceModifier.fillMaxSize().background(backgroundColor), content = {})

        Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            ) {
                Text(
                    text = currentTrackString.umlautify(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }

            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.shuffle),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (isShuffleEnabled) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                        .height(24.dp)
                        .clickable { playerRepo.toggleShuffle() },
                )
                Image(
                    provider = ImageProvider(R.drawable.media3_notification_seek_to_previous),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                    modifier = GlanceModifier.defaultWeight()
                        .height(36.dp)
                        .clickable { playerRepo.skipToStartOrPrevious() },
                )
                if (isPlaying) {
                    Image(
                        provider = ImageProvider(R.drawable.media3_notification_pause),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                        modifier = GlanceModifier.defaultWeight()
                            .height(36.dp)
                            .clickable { playerRepo.playOrPauseCurrent() },
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.media3_notification_play),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            if (canPlay) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.defaultWeight()
                            .height(36.dp)
                            .clickable { playerRepo.playOrPauseCurrent() },
                    )
                }
                Image(
                    provider = ImageProvider(R.drawable.media3_notification_seek_to_next),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (canGotoNext) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                        .height(36.dp)
                        .clickable { playerRepo.skipToNext() },
                )
                Image(
                    provider = ImageProvider(R.drawable.repeat),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (isRepeatEnabled) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                        .height(24.dp)
                        .clickable { playerRepo.toggleRepeat() },
                )
            }
        }
    }
}
