package us.huseli.thoucylinder.widget

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.managers.WidgetManager

@SuppressLint("RestrictedApi")
@Composable
fun Widget(manager: WidgetManager) {
    val context = LocalContext.current
    val defaultBackgroundColor = GlanceTheme.colors.background

    val canGotoNext by manager.canGotoNext.collectAsState()
    val canPlay by manager.canPlay.collectAsState()
    val currentBitmap by manager.currentBitmap.collectAsState()
    val currentTrackString by manager.currentTrackString.collectAsState()
    val isPlaying by manager.isPlaying.collectAsState()
    val isShuffleEnabled by manager.isShuffleEnabled.collectAsState()
    val isRepeatEnabled by manager.isRepeatEnabled.collectAsState()

    val backgroundColor = remember(currentBitmap) {
        if (currentBitmap != null) ColorProvider(R.color.widget_background) else defaultBackgroundColor
    }

    Box(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(8.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        currentBitmap?.also {
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
                    text = (currentTrackString ?: context.getUmlautifiedString(R.string.no_track_playing)),
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
                        .clickable { manager.toggleShuffle() },
                )
                Image(
                    provider = ImageProvider(R.drawable.media3_notification_seek_to_previous),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                    modifier = GlanceModifier.defaultWeight()
                        .height(36.dp)
                        .clickable { manager.skipToStartOrPrevious() },
                )
                if (isPlaying) {
                    Image(
                        provider = ImageProvider(R.drawable.media3_notification_pause),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                        modifier = GlanceModifier.defaultWeight()
                            .height(36.dp)
                            .clickable { manager.playOrPauseCurrent() },
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
                            .clickable { manager.playOrPauseCurrent() },
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
                        .clickable { manager.skipToNext() },
                )
                Image(
                    provider = ImageProvider(R.drawable.repeat),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (isRepeatEnabled) GlanceTheme.colors.onBackground else GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                        .height(24.dp)
                        .clickable { manager.toggleRepeat() },
                )
            }
        }
    }
}
