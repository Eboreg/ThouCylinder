package us.huseli.thoucylinder.managers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.thoucylinder.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class NotificationManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = context.getString(R.string.notification_channel_description)
        enableVibration(false)
    }
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(
        id: Int,
        title: String,
        text: String? = null,
        ongoing: Boolean = false,
        progress: Double? = null,
    ) {
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.media3_notification_small_icon)
                .setContentTitle(title)
                .setOngoing(ongoing)

            if (text != null) builder.setContentText(text)
            if (progress != null) builder.setProgress(100, (progress * 100).roundToInt(), false)
            else builder.setProgress(0, 0, false)
            notify(id, builder.build())
        }
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    companion object {
        const val CHANNEL_ID = "fistopyBasicNotifications"
        const val ID_IMPORT_ALBUMS = 1
    }
}
